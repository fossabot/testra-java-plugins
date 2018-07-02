package tech.testra.jvm.commons.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

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
    String myHash = DatatypeConverter
        .printHexBinary(digest).toUpperCase();

   return myHash;
  }

}
