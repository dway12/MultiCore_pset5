import java.util.*;
import java.util.concurrent.atomic.*;

class Node {
  Object item;
  int key;
  AtomicMarkableReference<Node> next;

  public Node (int key, Object x) {
    this.key = key;
    this.item = x;
  }
}

class Window {
  public Node pred, curr;
  public Window (Node myPred, Node myCurr) {
    pred = myPred; 
    curr = myCurr;
  }


  public Window find(Node head, int key) {
    Node pred = null, curr = null, succ = null;
    boolean[] marked = {false};
    boolean snip;
    retry: while (true) {
      pred = head;
      curr = pred.next.getReference();
      while (true) {
        if (curr.next == null) {
          return new Window(pred, curr);
        }
        succ = curr.next.get(marked);
        while(marked[0]) {
          snip = pred.next.compareAndSet(curr, succ, false, false);
          if (!snip) { continue retry;}
          curr = succ;
          succ = curr.next.get(marked);
        }
        if (curr.key >= key) {
          return new Window(pred, curr);
        }
        pred = curr;
        curr = succ;
      }
    }
  }
}

class BucketListLockFree<T>  {
  static final int HI_MASK = 0x00800000;
  static final int MASK = 0x00FFFFFF;
  Node head;
  public BucketListLockFree(Node node) {
    if (node == null) {
      head = new Node(0,null);
    } else {
      head = node;
    }
    head.next = new AtomicMarkableReference<Node>(new Node(Integer.MAX_VALUE,null), false);
  }
  public int makeOrdinaryKey(int key) {
    int code = key & MASK;
    return Integer.reverse(code | HI_MASK);
  }
  private static int makeSentinelKey(int key) {
    return Integer.reverse(key & MASK);
  }
  public boolean contains(int key) {
    key = makeOrdinaryKey(key);
    Window window = new Window(null,null);
    window = window.find(head, key);
    Node pred = window.pred;
    Node curr = window.curr;
    return (curr.key == key);
  }

  public boolean add(int key, T x) {
    key = makeOrdinaryKey(key);
    while (true) {
      Window window = new Window(null,null);
      window = window.find(head, key);
      Node pred = window.pred, curr= window.curr;
      if (curr.key == key){
        return false;
      }else {
        Node node = new Node(key,x);
        node.next = new AtomicMarkableReference(curr, false);
        if (pred.next.compareAndSet(curr,node,false,false)) {
          return true;
        }
      }
    }
  }
  public boolean remove(int key) {
    key = makeOrdinaryKey(key);
    boolean snip;
    while (true) {
  
      Window window = new Window(null,null);
      window = window.find(head, key);
      Node pred = window.pred, curr= window.curr;
      if (curr.key != key) {
        return false;
      } else {
        Node succ = curr.next.getReference();
        snip = curr.next.attemptMark(succ, true);
        if (!snip) {
          continue;
        }
        pred.next.compareAndSet(curr, succ, false, false);
        return true;
      }
    }
  }

  public BucketListLockFree<T> getSentinel(int index) {
    int key = makeSentinelKey(index);
    boolean splice;
    while(true) {
      Window window = new Window(null,null);
      window = window.find(head,key);
      Node pred = window.pred;
      Node curr = window.curr;
      if (curr.key == key) {
        return new BucketListLockFree(curr);
      } else {
        Node node = new Node(key,null);
        //System.out.println(node.next);
        node.next = new AtomicMarkableReference( pred.next, false);
        splice = pred.next.compareAndSet(curr, node, false, false);
        if (splice) {
          return new BucketListLockFree(node);
        }else{
          continue;
        }
      }
    }
  }

}


class AwesomeHashTable<T> implements HashTable<T> {
  protected BucketListLockFree<T>[] bucket;
  protected AtomicInteger bucketSize;
  protected AtomicInteger setSize;
  private int mask;
  int maxBucketSize;
  public AwesomeHashTable(int capacity,int maxBucketSize) {
    bucket = (BucketListLockFree<T>[]) new BucketListLockFree[maxBucketSize];
    bucket[0] = new BucketListLockFree<T>(null);
    bucketSize = new AtomicInteger(1);
    setSize = new AtomicInteger(0);
    this.mask = ( 1<< capacity) -1;
    this.maxBucketSize = maxBucketSize;
  }
  public boolean add (int key, T x) {
    //System.out.println("key: "+key);
    int myBucket = key % bucketSize.get();
    //System.out.println("myBucket: "+myBucket);
    BucketListLockFree<T> b = getBucketListLockFree(myBucket);
    if (!b.add(key,x)) {
      return false;
    }
    int setSizeNow = setSize.getAndIncrement();
    int bucketSizeNow = bucketSize.get();
    if (setSizeNow / bucketSizeNow > 4 && 2*bucketSizeNow<maxBucketSize) {
      bucketSize.compareAndSet(bucketSizeNow, 2*bucketSizeNow);
    //  System.out.println("incremtined buecktSize");
    /*  BucketListLockFree<T>[] bucket2 = (BucketListLockFree<T>[]) new BucketListLockFree[2*bucketSizeNow];
      for(int i = 0; i < bucketSizeNow; i++) {
        bucket2[i] = bucket[i]; 
      }
      for (int i = bucketSizeNow; i< 2*bucketSizeNow; i++) {
        bucket2[i] = null;
      }
      bucket = bucket2;
    */
    }
    return true;
  }
  public boolean contains (int key) {
    int myBucket = key % bucketSize.get();
    BucketListLockFree<T> b = getBucketListLockFree(myBucket);
    return b.contains(key);
  }

  public boolean remove (int key) {
    int myBucket = key  % bucketSize.get();
    BucketListLockFree<T> b = getBucketListLockFree(myBucket);
    if (!b.remove(key))  {
      return false;
    }
    int setSizeNow = setSize.getAndDecrement();
    return true;
  }
   
  private BucketListLockFree<T> getBucketListLockFree(int myBucket) {
    if (bucket[myBucket] == null) {
      initializeBucket(myBucket);
    }
    return bucket[myBucket];
  }
  private void initializeBucket(int myBucket) {
    int parent = getParent(myBucket);
    if (bucket[parent] == null) {
      initializeBucket(parent);
    }
    BucketListLockFree<T> b = bucket[parent].getSentinel(myBucket);
    if (b != null) {
      bucket[myBucket] = b;
    }
  }
  private int getParent(int myBucket) {
    int parent = bucketSize.get();
    do {
      parent = parent >> 1;
    } while (parent > myBucket);
    parent = myBucket - parent;
    return parent;
  }

/*
  public void printTable() {
    for (int x = 0; x< setSize; x++) {
      System.out.println("...." + i + "....");
      while(
*/
}


/*
class AwesomeHashTableTest {
  public static void main(String[] args) {
    AwesomeHashTable<Integer> table = new AwesomeHashTable<Integer>(1);
    for (int x = 0; x < 256; x++) {
      table.add(i, i*i);
    }
    table.printTable();
  }
}
*/
















