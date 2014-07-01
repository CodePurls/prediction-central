package name.nirav.mp.utils;
import java.io.IOException;
import java.nio.ByteBuffer;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;


@SuppressWarnings("restriction")
public class NumberUtils {
  private static final BASE64Encoder e = new BASE64Encoder();
  private static final BASE64Decoder d = new BASE64Decoder();

  public static String encode(int num) {
    byte[] array = ByteBuffer.allocate(8).putInt(num).array();
    return e.encode(array);
  }

  public static int decode(String str) {
    try {
      byte[] decodeBuffer = d.decodeBuffer(str);
      return ByteBuffer.wrap(decodeBuffer).getInt();
    } catch (IOException e) {
    }
    return -1;
  }

  public static void main(String[] args) {
    for (int i = -1000; i < 10000; i++) {
      String encode = NumberUtils.encode(i);
      int decode = NumberUtils.decode(encode);
      if (decode != i) System.out.println(decode + " != " + i);
    }
  }

}
