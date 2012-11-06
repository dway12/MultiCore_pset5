
public class Statistics {

  public static double getStdDev(long[] count) {
    double sum = 0;
    double sumSquared = 0;
    for(int i = 0; i < count.length; i++) {
      sum += (double) count[i];
      sumSquared += (double) (count[i]*count[i]);
    }
    return Math.sqrt((sum*sum)-sumSquared)/((double) count.length);
  }

  public static double getEntropy(long[] count) {
    double[] p = new double[count.length];
    double total = 0;
    double entropy = 0;
    for(int i = 0; i < count.length; i++) {
      p[i] = (double) count[i];
      total += p[i];
    }
    for(int i = 0; i < count.length; i++) {
      p[i] /= total;
      entropy += p[i]*Math.log(p[i])/Math.log(2.0);
    }
    return -entropy;
  }
}