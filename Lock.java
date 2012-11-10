import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject;

interface ReadWriteLock {
  Lock readLock();
  Lock writeLock();
}

interface Lock {
  void lock();
  //Condition newCondition();
  void unlock();
}


class FIFOReadWriteLock implements ReadWriteLock {
  int readAcquires, readReleases;
  boolean writer;
  ReentrantLock lock;
  Condition condition;
  Lock readLock, writeLock;
  public FIFOReadWriteLock() {
    writer = false;
    readAcquires = readReleases = 0;
    lock = new ReentrantLock();
    readLock = new ReadLock();
    writeLock = new WriteLock();
    condition = lock.newCondition();
  }
  public Lock readLock() {
    return readLock;
  }
  public Lock writeLock() {
    return writeLock;
  }

  private class ReadLock implements Lock {
    public void lock() {
      lock.lock();
      try {
        while (writer) {
          condition.await();
        }
        readAcquires++;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }finally {
        lock.unlock();
      }
    }
    public void unlock() {
      lock.lock();
      try {
        readReleases++;
        if (readAcquires == readReleases)
          condition.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }
  private class WriteLock implements Lock {
    public void lock() {
      lock.lock();
      try {
        while (writer) {
          condition.await();
        }
        writer = true;
        while (readAcquires != readReleases) {
          condition.await();
        }
       // System.out.println("after awaiting");
      } catch (InterruptedException e) {
        e.printStackTrace(); 
      } finally {
        lock.unlock();
      }
    }
    public void unlock() {
      writer =false;
      try {
        condition.signalAll();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}





