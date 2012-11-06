import java.util.concurrent.locks.Lock;

public interface HashPacketWorker<T> extends Runnable {
  public void run();
}

class SerialHashPacketWorker implements HashPacketWorker {
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
