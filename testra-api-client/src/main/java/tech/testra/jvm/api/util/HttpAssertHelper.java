package tech.testra.jvm.api.util;

import tech.testra.jvm.api.message.http.HttpResponseMessage;

import static org.assertj.core.api.Assertions.assertThat;


public final class HttpAssertHelper {

  private HttpAssertHelper() {
  }

  private static void assertHttpStatus(int expectedStatus, HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertThat(httpResponseMessage.getStatusCode())
        .as("HTTP status code not matched on %s for acc no %s", httpResponseMessage.getUrl(),
            accountId)
        .isEqualTo(expectedStatus);
  }

  public static void assertHttpStatusAsOk(HttpResponseMessage httpResponseMessage) {
    assertHttpStatusAsOk(httpResponseMessage, "");
  }

  public static void assertHttpStatusAsBadRequest(HttpResponseMessage httpResponseMessage) {
    assertHttpStatusAsBadRequest(httpResponseMessage, "");
  }

  public static void assertHttpStatusAsInternalServerError(
      HttpResponseMessage httpResponseMessage) {
    assertHttpStatusAsInternalServerError(httpResponseMessage, "");
  }

  public static void assertHttpStatusAsNotFound(HttpResponseMessage httpResponseMessage) {
    assertHttpStatusAsNotFound(httpResponseMessage, "");
  }

  public static void assertHttpStatusAsCreated(HttpResponseMessage httpResponseMessage) {
    assertHttpStatusAsCreated(httpResponseMessage, "");
  }

  public static void assertHttpStatusAsForbidden(HttpResponseMessage httpResponseMessage) {
    assertHttpStatusAsForbidden(httpResponseMessage, "");
  }

  public static void assertHttpStatusAsForbidden(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(403, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsPaymentRequired(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(402, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsConflict(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(409, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsOk(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(200, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsBadRequest(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(400, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsInternalServerError(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(500, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsNotFound(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(404, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsCreated(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(201, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAsMethodNotFound(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertHttpStatus(405, httpResponseMessage, accountId);
  }

  public static void assertHttpStatusAs4xx(HttpResponseMessage httpResponseMessage,
      String accountId) {
    assertThat((String.valueOf(httpResponseMessage.getStatusCode()).startsWith("4")))
        .isTrue()
        .as("HTTP status code is not 4xx on %s for acc no %s", httpResponseMessage.getUrl(),
            accountId);
  }

  public static void assertHttpStatusNoContent(HttpResponseMessage httpResponseMessage) {
    assertHttpStatusNoContent(httpResponseMessage, "");
  }

  public static void assertHttpStatusNoContent(HttpResponseMessage httpResponseMessage, String accountId) {
    assertHttpStatus(204, httpResponseMessage, accountId);
  }
}
