package tech.testra.jvm.api.util;

import com.jleth.util.LoggingInterceptorOkHttp2;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import okio.Buffer;

import java.io.EOFException;
import java.io.IOException;
import java.util.Locale;

public class CustomLoggingInterceptor extends LoggingInterceptorOkHttp2 {

  private Logger logger;
  private LogLevel logLevel;

  public CustomLoggingInterceptor(Logger logger) {
    super(logger, LogLevel.FULL);
    this.logger = logger;
    this.logLevel = super.getLogLevel();
  }

  private static String bodyToString(final Request request) {
    try (Buffer buffer = new Buffer()) {
      if (request.body() == null) {
        return "(no body)";
      }
      final Request copy = request.newBuilder().build();
      copy.body().writeTo(buffer);
      String requestContent = buffer.readUtf8();
      if (requestContent.isEmpty()) {
        requestContent = "(empty body)";
      }
      return requestContent;
    } catch (Exception e) {
      return "(body not printable)";
    }
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    if (!logLevel.log()) {
      return chain.proceed(chain.request());
    }
    Request request = chain.request();

    long t1 = System.nanoTime();
    logger.info(String.format(Locale.ENGLISH, ">>==> HTTP %s %s%n",
        request.method(),
        request.url()));

    request.headers().toMultimap()
        .forEach((n, v) -> logger.info(n + ": " + v.stream().reduce("", String::concat)));

    String requestContent = bodyToString(request);
    logger.info(String.format(Locale.ENGLISH, "payload: %s%n", requestContent));
    logger.info(String.format(Locale.ENGLISH, ">>==> END HTTP (%d-byte body)%n",
        (request.body() != null ? request.body().contentLength() : 0)));
    Response response;
    try {
      response = chain.proceed(request);

    } catch (IOException e) {
      logger.info(String.format(Locale.ENGLISH, "<==> HTTP connection error (%s) %s",
          e.getClass().getCanonicalName(), e.getMessage()));
      throw e;
    }

    long t2 = System.nanoTime();
    logger.info(String.format(Locale.ENGLISH, "<==<< HTTP %d %s (%.1fms)%n",
        response.code(),
        response.request().url(),
        (t2 - t1) / 1e6d));
    response.headers().toMultimap()
        .forEach((n, v) -> logger.info(n + ": " + v.stream().reduce("", String::concat)));
    MediaType contentType = null;
    String responseContent = null;
    if (response.body() != null) {
      contentType = response.body().contentType();
      try {
        responseContent = response.body().string();
      } catch (EOFException e) {
        responseContent = "";
      }
    }
    if (responseContent != null && !responseContent.contains("<!DOCTYPE html")) {
      logger.info(String.format(Locale.ENGLISH, "payload: %s%n", responseContent));
    }

    logger.info(String.format(Locale.ENGLISH, "<==<< END HTTP (%d-byte body)%n",
        (response.body() != null ? response.body().contentLength() : 0)));

    if (response.body() != null) {
      ResponseBody wrappedBody = ResponseBody.create(contentType, responseContent);
      response = response.newBuilder().body(wrappedBody).build();
    }

    return response;
  }
}
