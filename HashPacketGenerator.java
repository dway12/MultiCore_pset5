import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.lang.Object;
import java.lang.Math;


class HashPacket<T> {
  int key;
  T body;
  PacketType type;
  public HashPacket(int key, T body, PacketType type) {
    this.key = key;
    this.body = body;
    this.type = type;
  }
  public enum PacketType {
    Add,
    Remove,
    Contains
  }
  public int mangleKey() { 
    final int CRC_POLY = 954680065; // 0x38E74301 - standard CRC30 from CDMA
    final int iterations = 32;
    int crc = key;
    for( int i = 0; i < iterations; i++ ) {
      if( ( crc & 1 ) > 0 )
        crc = (crc >> 1) ^ CRC_POLY;
      else
        crc = crc >> 1;
    }
    return crc;
  }
  public PacketType getType() {
    return type;
  }
  public T getItem() { return body; }
  public void printPacket() {
    if( type == PacketType.Add )
      System.out.println("Add      " + key + "/" + body);
    else if( type == PacketType.Remove )
      System.out.println("Remove   " + key + "/" + body);
    else
      System.out.println("Contains " + key + "/" + body);
  }
}

// invariants: fractionAdd > fractionRemove
// 0 < {fractionAdd, fractionRemove, rho} < 1


class HashPacketGenerator {
  final HashKeyGenerator head;
  int currentHead = 0;
  final HashKeyGenerator tail;
  int currentTail = 0;
  final RandomGenerator rand;
  int totalPackets = 0;
  final int DENOMINATOR = 1048575; // 2^20 - 1
  final int fractionAdd;
  final int fractionRemove;
  final long mean;
  public HashPacketGenerator(
    float fractionAdd,
    float fractionRemove,
    float hitRate,
    long mean) {
    float tmpDenom = (float) DENOMINATOR;
    this.fractionAdd = Math.round(tmpDenom*fractionAdd);
    this.fractionRemove = Math.round(tmpDenom*fractionRemove);
    this.head = new HashKeyGenerator(hitRate);
    this.tail = new HashKeyGenerator(hitRate);
    this.rand = new RandomGenerator();
    this.mean = mean;
  }
  public HashPacket<Packet> getRandomPacket() {
    int tmp = ( (int) rand.getRand() ) & DENOMINATOR;
    totalPackets++;
    if( ( tmp < fractionAdd ) || ( currentHead <= currentTail ) )
      return getAddPacket();
    else if( tmp < (fractionAdd + fractionRemove))
      return getRemovePacket();
    else
      return getContainsPacket();
  }
  @SuppressWarnings("unchecked")
  public HashPacket<Packet> getAddPacket() {
    currentHead = head.getKey();
    return new HashPacket(currentHead, new Packet(rand.getRand() % mean, rand.getRand() % mean), HashPacket.PacketType.Add);
  }
  @SuppressWarnings("unchecked")
  public HashPacket<Packet> getRemovePacket() {
    currentTail = tail.getKey();
    return new HashPacket(currentTail, new Packet(rand.getRand() % mean, rand.getRand() % mean), HashPacket.PacketType.Remove);
  }
  @SuppressWarnings("unchecked")
  public HashPacket<Packet> getContainsPacket() {
    final long THIRTY_ONE_BITS = 2147483647; // 2^32 - 1
    int key = (int) ( THIRTY_ONE_BITS & rand.getRand() );
    key = ( key % ( currentHead - currentTail + 1 ) ) + currentTail;
    return new HashPacket(key, new Packet(rand.getRand() % mean, rand.getRand() % mean), HashPacket.PacketType.Contains);
  }
}



class HashKeyGenerator {
  RandomGenerator rand = new RandomGenerator();
  float hitRate;
  float drunkWalk = 1.0f;
  int key = 1;
  public HashKeyGenerator(float hitRate) {
    this.hitRate = hitRate;
  }
  int getKey() {
    while( drunkWalk < 0 ) {
      key++;
      drunkWalk += hitRate;
    }
    drunkWalk -= 1.0;
    return key;
  }
}

class HashPacketGeneratorTest {
  public static void main(String[] args) {  
    HashPacketGenerator generator = new HashPacketGenerator(.09f, .01f, .5f, 2000);
    for( int i = 0; i < 40; i++ )
      generator.getRandomPacket().printPacket();
  }
}
