import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.locks.ReentrantLock.*;

class LockingHashTable<T> implements HashTable<T> {
  final ReentrantReadWriteLock[] locks; 
  private SerialList<T,Integer>[] table;
  private int logSize;
  private int mask;
  private final int maxBucketSize;
  private int numThreads;

  public LockingHashTable(int logSize,int numThreads, int maxBucketSize) {
    this.logSize = logSize;
    this.mask = (1 << logSize) -1;
    this.maxBucketSize = maxBucketSize;
    this.table = new SerialList[1 << logSize];
    locks = new ReentrantReadWriteLock[numThreads];
    
    for (int j=0; j<locks.length; j++) {
      locks[j] = new ReentrantReadWriteLock();
    }
    
  }
  public void resizeIfNecessary(int key) {
    while( table[key & mask] != null 
          && table[key & mask].getSize() >= maxBucketSize )
      resize();
  }
  public final void acquireRead(int key) {
    locks[(key & mask)  % locks.length].readLock().lock();
  }
  public final void releaseRead(int key) {
    locks[(key & mask)  % locks.length].readLock().unlock();
  }
  public final void acquireWrite(int key) {
    locks[(key & mask) % locks.length].writeLock().lock();
  }
  public final void releaseWrite(int key) {
    locks[(key & mask) % locks.length].writeLock().unlock();
  }
  public boolean contains(int key) {
    acquireRead(key);
    int myBucket = key & mask;
    try {
      if ( table[myBucket] != null ) 
        return table[myBucket].contains(key);
      else
        return false;
    } finally {
      releaseRead(key);
    }
  }

  public boolean add(int key, T x) {
    resizeIfNecessary(key); 
    acquireWrite(key);
    int myBucket = key & mask;
    if ( table[myBucket] == null) {
      //System.out.println("null");
      table[myBucket] = new SerialList<T,Integer>(key,x);
    } else {
      table[myBucket].add(key,x);
      //System.out.println("not null");
    }
 
    releaseWrite(key);
    return true;
  }
  public boolean remove(int key) {
    resizeIfNecessary(key);
    boolean result = false;
    acquireWrite(key);
    if (table[key & mask]!=null)
      result =  table[key & mask].remove(key);
    else
      result =  false;
    releaseWrite(key);
    return result;
  }
  public void resize() {
    for (ReentrantReadWriteLock lock: locks) {
      lock.writeLock().lock();
    }
    SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
    for( int i = 0; i < table.length; i++ ) {
      if( table[i] == null )
        continue;
      SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
      while( iterator != null && iterator.hasNext() ) {
        if( newTable[iterator.key & ((2*mask)+1)] == null )
          newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
        else
          newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
        iterator = iterator.getNext();
      }
    }
    table = newTable;
    logSize++;
    mask = (1 << logSize) - 1;
    for (ReentrantReadWriteLock lock: locks) {
      lock.writeLock().unlock();
    }
  }
  public void printTable() {
    for( int i = 0; i <= mask; i++ ) {
      System.out.println("...." + i + "....");
      if( table[i] != null)
        table[i].printList();
    }
  }

}
class LockingHashTableTest {
  public static void main(String[] args) {
    LockingHashTable<Integer> table = new LockingHashTable<Integer>(2,6,8);
    for (int i = 0; i < 256; i++) {
      table.add(i, i*i);
    }
    table.printTable();
  }
}
 







