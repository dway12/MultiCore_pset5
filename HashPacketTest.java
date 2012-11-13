import java.util.*;

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
    final int hash_map_type = Integer.parseInt(args[8]);

    StopWatch timer = new StopWatch();
 
    //
    // allocate and initialize Lamport queues and hash table
    //

    HashTable<Packet> hashTable = null;
    switch (hash_map_type){
      case 0:
        hashTable = new LockingHashTable<Packet>(1, numWorkers,
                                                 maxBucketSize);
        break;
      case 1:
        // put hashmap type here
        hashTable = new LockFreeHashTable<Packet>(1, numWorkers, maxBucketSize);
        break;
      case 2:
        hashTable = new AwesomeHashTable<Packet>(1,maxBucketSize);
        break;
      case 3: 
        hashTable = new OptimisticHashTable<Packet>(1, numWorkers,
                                                 maxBucketSize);
        break;
      case 4:
        hashTable = new LinearHashTable<Packet>(1, numWorkers);
        break;
      case 5:
        hashTable = null;
        break;
    } 
    List<LamportQ> q_bank = new ArrayList<LamportQ>(numWorkers);
    for (int x = 0; x< numWorkers; x++){
      LamportQ temp_q = new LamportQ(8);
      q_bank.add(temp_q);
    }   
    
    HashPacketGenerator source = new HashPacketGenerator(fractionAdd,fractionRemove,hitRate,mean);
    // 
    // initialize your hash table w/ initSize number of add() calls using
    // source.getAddPacket();
    //
    

    // allocate and initialize locks and any signals used to marshal threads (eg. done signals)
    // 
    List<ParallelHashPacketWorker> w_bank = new ArrayList<ParallelHashPacketWorker>
                                                (numWorkers);
    for (int x=0; x<numWorkers; x++) {
      PaddedPrimitive<Boolean> temp_done = new PaddedPrimitive(false);
      ParallelHashPacketWorker temp_worker = new ParallelHashPacketWorker(temp_done,
                                                       q_bank.get(x),
                                                       hashTable);
      w_bank.add(temp_worker);
      //
      // call .start() on your Workers
      //
      temp_worker.start();
      //System.out.println("starting");
    }
    PaddedPrimitive<Boolean> done_dis = new PaddedPrimitive(false);
    Dispatcher dispatcher = new Dispatcher(done_dis,numWorkers,
                                           source,q_bank); 
    for( int i = 0; i < initSize; i++ ) {
      HashPacket<Packet> pkt = source.getAddPacket();
      //System.out.println("adding");
      hashTable.add(pkt.mangleKey(), pkt.getItem());
    }
    // allocate and inialize Dispatcher and Worker threads
    
    timer.startTimer();
    //
    // call .start() on your Dispatcher
    //
    dispatcher.start();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    //
    // assert signals to stop Dispatcher
    // 
    dispatcher.done.value = true;
    try {
      dispatcher.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // call .join() on Dispatcher
    //
    // assert signals to stop Workers - they are responsible for leaving
    // the queues empty
    //
    // call .join() for each Worker
    //
    int numPackets = 0;
    for (int x = 0; x < numWorkers; x++ ) {
      ParallelHashPacketWorker temp_worker = w_bank.get(x);
      temp_worker.done.value = true;
      numPackets += temp_worker.totalPackets;
      try {
        temp_worker.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    timer.stopTimer();
    System.out.println("count: " + numPackets);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(numPackets/timer.getElapsedTime() + " pkts / ms");

    // report the total number of packets processed and total time
  }
}
