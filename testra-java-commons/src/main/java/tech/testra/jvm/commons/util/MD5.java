package tech.testra.jvm.commons.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

  public static String generateMD5(String string){

    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    md.update(string.getBytes());
    byte[] digest = md.digest();
    String myHash = bytesToHex(digest).toUpperCase();

   return myHash;
  }

  public static String bytesToHex(final byte[] bytes)
  {
    final int numBytes = bytes.length;
    final char[] container = new char[numBytes * 2];

    for (int i = 0; i < numBytes; i++)
    {
      final int b = bytes[i] & 0xFF;

      container[i * 2] = Character.forDigit(b >>> 4, 0x10);
      container[i * 2 + 1] = Character.forDigit(b & 0xF, 0x10);
    }

    return new String(container);
  }

}
