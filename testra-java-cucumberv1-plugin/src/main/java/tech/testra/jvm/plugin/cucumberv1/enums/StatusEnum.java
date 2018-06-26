package tech.testra.jvm.plugin.cucumberv1.enums;

public enum StatusEnum {
  PASSED("PASSED"),FAILED("FAILED"),SKIPPED("SKIPPED"),PENDING("PENDING");

  private String value;

  StatusEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}