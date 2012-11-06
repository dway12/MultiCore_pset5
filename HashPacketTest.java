
class SerialHashPacket {
  public static void main(String[] args) {

    final int numMilliseconds = Integer.parseInt(args[0]);    
    final float fractionAdd = Float.parseFloat(args[1]);
    final float fractionRemove = Float.parseFloat(args[2]);
    final float hitRate = Float.parseFloat(args[3]);
    final int maxBucketSize = Integer.parseInt(args[4]);
    final long mean = Long.parseLong(args[5]);
    final int initSize = Integer.parseInt(args[6]);

    @SuppressWarnings({"unchecked"})
    StopWatch timer = new StopWatch();
    HashPacketGenerator source = new HashPacketGenerator(fractionAdd,fractionRemove,hitRate,mean);
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
    SerialHashTable<Packet> table = new SerialHashTable<Packet>(1, maxBucketSize);
    
    for( int i = 0; i < initSize; i++ ) {
      HashPacket<Packet> pkt = source.getAddPacket();
      table.add(pkt.mangleKey(), pkt.getItem());
    }
    SerialHashPacketWorker workerData = new SerialHashPacketWorker(done, source, table);
    Thread workerThread = new Thread(workerData);
    
    workerThread.start();
    timer.startTimer();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    done.value = true;
    memFence.value = true;
    try {
      workerThread.join();
    } catch (InterruptedException ignore) {;}      
    timer.stopTimer();
    final long totalCount = workerData.totalPackets;
    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
  }
}


class ParallelHashPacket {
  public static void main(String[] args) {

    final int numMilliseconds = Integer.parseInt(args[0]);    
    final float fractionAdd = Float.parseFloat(args[1]);
    final float fractionRemove = Float.parseFloat(args[2]);
    final float hitRate = Float.parseFloat(args[3]);
    final int maxBucketSize = Integer.parseInt(args[4]);
    final long mean = Long.parseLong(args[5]);
    final int initSize = Integer.parseInt(args[6]);
    final int numWorkers = Integer.parseInt(args[7]); 

    StopWatch timer = new StopWatch();
    //
    // allocate and initialize Lamport queues and hash table
    //
    HashPacketGenerator source = new HashPacketGenerator(fractionAdd,fractionRemove,hitRate,mean);
    // 
    // initialize your hash table w/ initSize number of add() calls using
    // source.getAddPacket();
    //
    // allocate and initialize locks and any signals used to marshal threads (eg. done signals)
    // 
    // allocate and inialize Dispatcher and Worker threads
    //
    // call .start() on your Workers
    //
    timer.startTimer();
    //
    // call .start() on your Dispatcher
    //
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    //
    // assert signals to stop Dispatcher
    // 
    // call .join() on Dispatcher
    //
    // assert signals to stop Workers - they are responsible for leaving
    // the queues empty
    //
    // call .join() for each Worker
    //
    timer.stopTimer();
    // report the total number of packets processed and total time
  }
}
