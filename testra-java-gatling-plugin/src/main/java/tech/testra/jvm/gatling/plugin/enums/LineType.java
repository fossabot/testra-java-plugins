package tech.testra.jvm.gatling.plugin.enums;

public enum LineType {
  OK("OK"),
  REQUEST("REQUEST"),
  RUN("RUN"),
  USER("USER"),
  START("START"),
  END("END");

  private String value;

  LineType(String value) {
    this.value = value;
  }


  public String getValue() {
    return value;
  }
}