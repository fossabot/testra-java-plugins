package com.williamhill.whgtf.testra.jvm.util;

public final class StringHelper {

  private StringHelper() {
  }

  public static String substitute(String input, String searchString, String value) {
    return input.replace("{" + searchString + "}", value);
  }

  public static String substitute(String input, String searchString, int value) {
    return substitute(input, searchString, String.valueOf(value));
  }
}
