package tech.testra.jvm.api.client.http;

import com.google.common.collect.Multimap;
import com.squareup.okhttp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.api.message.http.HttpRequestMessage;
import tech.testra.jvm.api.message.http.HttpResponseMessage;
import tech.testra.jvm.api.util.CustomLoggingInterceptor;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S00112")
public class GenericHttpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenericHttpClient.class);
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String MEDIA_TYPE_APP_JSON = "application/json";

  protected final OkHttpClient client;

  public GenericHttpClient(String identifier, int poolSize) {
    this(identifier, poolSize, true);
  }

  public GenericHttpClient(String identifier, int poolSize, boolean interceptHttp) {
    client = new OkHttpClient();
    client.setConnectTimeout(1, TimeUnit.MINUTES);
    client.setReadTimeout(1, TimeUnit.MINUTES);
    client.setFollowRedirects(false);
    if (interceptHttp) {
      Interceptor loggingInterceptor = new CustomLoggingInterceptor(LOGGER::info);
      client.interceptors().add(loggingInterceptor);
    }
    disableSsl();
    if (poolSize > 0) {
      client.setConnectionPool(new ConnectionPool(poolSize, 10, TimeUnit.MINUTES));
      LOGGER.info("Initialised Ok-http client for {} with connection pool size : {}", identifier,
          poolSize);
    }
  }

  private static HttpResponseMessage mapToHttpResponseMessage(final Response response) {
    HttpResponseMessage httpResponseMessage = new HttpResponseMessage();

    httpResponseMessage.setStatusCode(response.code());
    httpResponseMessage.setMessage(response.message());
    try {
      httpResponseMessage.setPayload(response.body().string());
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
      throw new RuntimeException("Response body serialisation error");
    }

    response.headers().toMultimap().forEach((k, v) -> {
      if (k.equals("Set-Cookie")) {
        httpResponseMessage.setCookies(v.stream().map(i -> i.split(";")[0])
            .collect(Collectors.toSet()));
      } else {
        httpResponseMessage.getHeaders().put(k, v.get(0));
      }
    });

    return httpResponseMessage;
  }

  private static Headers buildHeadersFromMap(Multimap<String, String> headersMap) {
    Headers.Builder builder = new Headers.Builder();
    headersMap.asMap().forEach((k, v) -> v.forEach(i -> builder.add(k, i)));
    return builder.build();
  }

  private static HttpUrl buildHttpUrl(HttpRequestMessage httpRequestMessage) {
    HttpUrl.Builder builder = HttpUrl.parse(httpRequestMessage.getUrl()).newBuilder();
    if (httpRequestMessage.getQueryParameters() != null) {
      httpRequestMessage.getQueryParameters().forEach(builder::addQueryParameter);
    }
    return builder.build();
  }

  public HttpResponseMessage get(HttpRequestMessage httpRequestMessage) {
    Request request = new Request.Builder()
        .url(buildHttpUrl(httpRequestMessage))
        .headers(buildHeadersFromMap(httpRequestMessage.getHeaders()))
        .get()
        .build();
    return execute(request);
  }

  public HttpResponseMessage post(final HttpRequestMessage httpRequestMessage,
      RequestBody requestBody) {
    final Request request = new Request.Builder()
        .url(buildHttpUrl(httpRequestMessage))
        .headers(buildHeadersFromMap(httpRequestMessage.getHeaders()))
        .post(requestBody)
        .build();
    return execute(request);
  }

  public HttpResponseMessage post(final HttpRequestMessage httpRequestMessage) {
    Optional<String> contentType = httpRequestMessage.getHeaders().get(CONTENT_TYPE_HEADER).stream()
        .findFirst();
    final Request request = new Request.Builder()
        .url(buildHttpUrl(httpRequestMessage))
        .headers(buildHeadersFromMap(httpRequestMessage.getHeaders()))
        .post(RequestBody.create(
            MediaType.parse(contentType.isPresent() ? contentType.get() : MEDIA_TYPE_APP_JSON),
            httpRequestMessage.getPayload()))
        .build();
    return execute(request);
  }

  public HttpResponseMessage put(HttpRequestMessage httpRequestMessage) {
    Optional<String> contentType = httpRequestMessage.getHeaders().get(CONTENT_TYPE_HEADER).stream()
        .findFirst();
    Request request = new Request.Builder()
        .url(buildHttpUrl(httpRequestMessage))
        .headers(buildHeadersFromMap(httpRequestMessage.getHeaders()))
        .put(RequestBody.create(
            MediaType.parse(contentType.isPresent() ? contentType.get() : MEDIA_TYPE_APP_JSON),
            httpRequestMessage.getPayload()))
        .build();
    return execute(request);
  }

  private HttpResponseMessage execute(final Request request) {
    try {
      Response response = client.newCall(request).execute();
      return mapToHttpResponseMessage(response);
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
      throw new RuntimeException(String
          .format("Http %s method failed with exception %s", request.method(), e.getMessage()));
    }
  }

  @SuppressWarnings("squid:S1186")
  // Disable ssl certification check
  private void disableSsl() {
    final TrustManager[] trustAllCerts = new TrustManager[]{new X509ExtendedTrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }

      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
      }

      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
      }

      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) {
      }

      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) {
      }
    }};

    // Install the all-trusting trust manager
    final SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      client.setSslSocketFactory(sslSocketFactory);
      client.setHostnameVerifier((hostname, session) -> true);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      LOGGER.error(e.getMessage());
    }
  }

  public HttpResponseMessage delete(HttpRequestMessage httpRequestMessage) {
    Optional<String> contentType = httpRequestMessage.getHeaders().get(CONTENT_TYPE_HEADER).stream()
        .findFirst();
    Request request = new Request.Builder()
        .url(buildHttpUrl(httpRequestMessage))
        .headers(buildHeadersFromMap(httpRequestMessage.getHeaders()))
        .delete(httpRequestMessage.getPayload() == null ? null :
            RequestBody.create(
                MediaType.parse(contentType.orElse(MEDIA_TYPE_APP_JSON)),
                httpRequestMessage.getPayload()))
        .build();
    return execute(request);
  }

}