import java.util.*;
import java.math.*;
import java.util.concurrent.locks.*;

class Bin{
  private Object x;
  private int count;
  private int key;

  public Bin(Object packet,int c, int k) {

    this.x=packet;
    this.count=c;
    this.key=k;
  }

  public int getCount(){

    return count;
  }
  
  public Object getPacket(){

    return x;
  }

  public int getKey(){

    return key;
  }

  public void setPacket(Object packet){

    this.x=packet;
  }

  public String toString(){

	
	return "packet:  " + x ;
    
  }
	

}



class LinearHashTable<T> implements HashTable<T> {
  final ReentrantReadWriteLock[] locks; 
  private Bin[] table;
  private int logSize;
  private int mask;
  private int numThreads;
  
  public LinearHashTable(int logSize,int numThreads) {
    this.mask = (1 << logSize) -1;
    this.table = new Bin[1 << logSize];
    locks = new ReentrantReadWriteLock[numThreads];
    
    for (int j=0; j<locks.length; j++) {
      locks[j] = new ReentrantReadWriteLock();
    }
    
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


  public boolean add(int key, T x) {
    
    acquireWrite(key);
    //System.out.println("add aquired write");
    int count=0;
    int myBucket = key & mask;
    int oldCount=0;
    int resizeCounter=0;
    if (table[myBucket] != null)  {
      oldCount = table[myBucket].getCount();
    }
    
    while(table[myBucket] != null && !(table[myBucket].getPacket().equals("removed"))){
      count=count+1;
      myBucket=myBucket+1;
      
      if (myBucket>=table.length){
	 if (resizeCounter==0){
		myBucket=myBucket % table.length;
		resizeCounter=1;}
	 else{	
	 System.out.println("Bucket: " + myBucket);
	 //printTable();
	 releaseWrite(key);
	 resize();
	 acquireWrite(key);
	 //printTable();
     }
      }
      }
    table[myBucket]= new Bin( x, Math.max(count,oldCount),key);
    releaseWrite(key);
    //System.out.println("add released write");
    
    return true;
  }

 public boolean remove(int key) {
    boolean result = false;
    int myBucket=key & mask;
    acquireWrite(key);
    //System.out.println("remove aquired write");
    if (table[myBucket]!=null){
      if (table[myBucket].getKey()== key){
	table[myBucket].setPacket("removed");
	releaseWrite(key);
	//System.out.println("remove released write");
      	return true;}
      else{
	int count=table[myBucket].getCount();
	for(int c=1; c<=count; c++){
	  if (myBucket+c < table.length && table[myBucket+c]!=null){
		if (table[myBucket+c].getKey()== key){
			table[myBucket+c].setPacket("removed");
			releaseWrite(key);
			//System.out.println("remove released write");
      			return true;
			}
	  	else
			count=Math.max(count, table[myBucket+c].getCount());}
	  else{
		releaseWrite(key);
   		//System.out.println("remove released write");
		return false;}
          	}
        
	releaseWrite(key);
	//System.out.println("remove released write");
    	return result;	}
    }      
    else{ 
    releaseWrite(key);
    //System.out.println("remove released write");
    return result;}
  }

 public boolean contains(int key) {
    boolean result = false;
    int myBucket=key & mask;
    acquireRead(key);
   // System.out.println("contains aquired read");
    if (table[myBucket]!=null){
      if (table[myBucket].getKey()== key){
	releaseRead(key);
	//System.out.println("contains released read");
      	return true;}
      else{
	int count=table[myBucket].getCount();
	for(int c=1; c<=count; c++){
	  if (myBucket+c < table.length && table[myBucket+c]!=null){
	 	 if (table[myBucket+c].getKey()==key && !(table[myBucket+c].getPacket().equals("removed"))){
		 	releaseRead(key);
			//System.out.println("contains released read");
      			return true;
		 }
	  	 else
		        count=Math.max(count, table[myBucket+c].getCount());
			
	   }else{
			releaseRead(key);
   			//System.out.println("contains released read");
			return false;}
         }
	releaseRead(key);
        //System.out.println("contains released read");
    	return result;	}
    }      
    else{ 
    releaseRead(key);
    //System.out.println("contains released read");
    return result;}
  }

public void resize() {
    //System.out.println("Resize in progress: Size="+table.length);
    for (ReentrantReadWriteLock lock: locks) {
      lock.writeLock().lock();
    }
    Bin[] newTable = new Bin[2*table.length];
    for( int i = 0; i < table.length; i++ ) {
      if( table[i] == null )
        continue;
      else
	newTable[i]=table[i];
    }
    
    table = newTable;
    logSize++;
    mask = (1 << logSize) - 1;
    //System.out.println("Resize completed: Size="+table.length);
    for (ReentrantReadWriteLock lock: locks) {
      lock.writeLock().unlock();
    }
  }

public void printTable() {
    for( int i = 0; i < table.length; i++ ) {
      System.out.println("...." + i + "....");
      if( table[i] != null)
        System.out.println(table[i]);
      else
	System.out.println("null");
    }
  }

}
class LinearHashTableTest {
  public static void main(String[] args) {
    LinearHashTable<Integer> table = new LinearHashTable<Integer>(2,6);
    for (int i = 0; i < 256; i++) {
      table.add(i, i*i);
    }
    table.printTable();
  }
}

 


