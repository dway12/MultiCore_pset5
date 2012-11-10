import java.util.concurrent.locks.Lock;
import java.util.*;

public abstract class HashPacketWorker<T> extends Thread {
  public abstract void run();
}

class SerialHashPacketWorker extends HashPacketWorker {
  PaddedPrimitiveNonVolatile<Boolean> done;
  final HashPacketGenerator source;
  final SerialHashTable<Packet> table;
  long totalPackets = 0;
  long residue = 0;
  Fingerprint fingerprint;
  public SerialHashPacketWorker(
    PaddedPrimitiveNonVolatile<Boolean> done, 
    HashPacketGenerator source,
    SerialHashTable<Packet> table) {
    this.done = done;
    this.source = source;
    this.table = table;
    fingerprint = new Fingerprint();
  }
  
  public void run() {
    HashPacket<Packet> pkt;
    while( !done.value ) {
      totalPackets++;
      pkt = source.getRandomPacket();
      residue += fingerprint.getFingerprint(pkt.getItem().iterations,pkt.getItem().seed);
      switch(pkt.getType()) {
        case Add: 
          table.add(pkt.mangleKey(),pkt.getItem());
          break;
        case Remove:
          table.remove(pkt.mangleKey());
          break;
        case Contains:
          table.contains(pkt.mangleKey());
          break;
      }
    }
  }  
}

//class ParallelHashPacketWorker<T> implements HashPacketWorker ...

class ParallelHashPacketWorker<T> extends HashPacketWorker {
  PaddedPrimitive<Boolean> done;
  final HashTable<Packet> table;
  public long totalPackets = 0;
  long residue = 0;
  Fingerprint fingerprint;
  LamportQ q;
  public ParallelHashPacketWorker(
    PaddedPrimitive<Boolean> done,
    LamportQ q,
    HashTable<Packet> table) {
    this.done = done;
    this.q = q;
    this.table = table;
    fingerprint = new Fingerprint();
  }
  
  public void run() {
    HashPacket<Packet> pkt = null;

    while( !done.value ) {
      boolean empty = false;
      try {
        pkt =(HashPacket<Packet>) q.deq();
      } catch( EmptyException e) {
        empty = true;
      }
      if (!empty) {
        totalPackets++;
        residue += fingerprint.getFingerprint(pkt.getItem().iterations,pkt.getItem().seed);
      
        switch(pkt.getType()) {
          case Add:
            table.add(pkt.mangleKey(),pkt.getItem());
            break;
          case Remove:
            table.remove(pkt.mangleKey());
            break;
          case Contains:
            table.contains(pkt.mangleKey());
            break;
        }

      }
    }
    // finish up queue, make it empty
    boolean empty1 = false;
    while(!empty1) {
      HashPacket<Packet> tmp = null;
      
      try {
        tmp = (HashPacket<Packet>) q.deq();  
      } catch(EmptyException e) {
        empty1 = true;
      }
      if (!empty1){
        totalPackets++;
        residue += fingerprint.getFingerprint(pkt.getItem().iterations,pkt.getItem().seed);
      
        switch(pkt.getType()) {
          case Add:
            table.add(pkt.mangleKey(),pkt.getItem());
            break;
          case Remove:
            table.remove(pkt.mangleKey());
            break;
          case Contains:
            table.contains(pkt.mangleKey());
            break;
        }
      }
    }
  }
}


class Dispatcher extends Thread {
  PaddedPrimitive<Boolean> done;
  HashPacketGenerator source;
  List<LamportQ> q_bank;
  int numSources;
  int totalPackets;
  int q_num = 0;
  public Dispatcher ( PaddedPrimitive<Boolean> done, int numSources, 
                      HashPacketGenerator source, List<LamportQ> q_bank) {
    this.done = done;
    this.numSources = numSources;
    this.source = source;
    this.q_bank = q_bank;
    q_num = 0;
  }


  public void run() {
    while(! (done.value) ) {
      HashPacket<Packet> tmp = null;
      totalPackets ++;
      try {
        LamportQ q = q_bank.get(q_num);
        tmp = source.getRandomPacket();
        q.enq(tmp);
      } catch (FullException e) {
        boolean spin = true;
        boolean success = true;
        while (spin) {
          for (int x = 0; x< q_bank.size(); x++) {
            success = true;
            try {
              LamportQ temp_q = q_bank.get(x);
              temp_q.enq(tmp);
            } catch (FullException f) {
              success = false;
            }
            if (success) break; 

          }
          if (success){
            spin = false;
          }

        }

      }
      q_num++;
      if (q_num >= numSources) {
         q_num = 0;
      }
    }
  }
}


class LamportQ<T> {
  public volatile int head = 0, tail =0;
  T[] items;
 
  public LamportQ (int capacity){
    items = (T[]) new Object[capacity];
    head = 0;
    tail = 0;
  }

  public void enq (T x) throws FullException {
    if (tail - head == items.length)
      throw new FullException();
    
    items[tail % items.length] = x;
    tail++;
  }


  public T deq() throws EmptyException {
    if (tail - head == 0)
      throw new EmptyException();
 
    T x = items[head % items.length];
    head ++;
    return x; 
  }


}









