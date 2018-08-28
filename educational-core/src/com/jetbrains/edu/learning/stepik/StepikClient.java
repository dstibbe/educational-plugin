package com.jetbrains.edu.learning.stepik;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PlatformUtils;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.intellij.util.net.ssl.ConfirmingTrustManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.stepik.format.StepikCourse;
import com.jetbrains.edu.learning.stepik.serialization.StepikLessonAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikRemoteInfoAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikReplyAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikStepOptionsAdapter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class StepikClient {
  private static final Logger LOG = Logger.getInstance(StepikClient.class.getName());
  private static CloseableHttpClient ourClient;
  private static final int TIMEOUT_SECONDS = 10;

  private StepikClient() {
  }

  @NotNull
  public static CloseableHttpClient getHttpClient() {
    if (ourClient == null) {
      initializeClient();
    }
    return ourClient;
  }

  public static <T> T getFromStepik(String link, final Class<T> container) throws IOException {
    return getFromStepik(link, container, getHttpClient());
  }

  static <T> T getFromStepik(String link, final Class<T> container, @NotNull final CloseableHttpClient client) throws IOException {
    if (!link.startsWith("/")) link = "/" + link;
    final HttpGet request = new HttpGet(StepikNames.STEPIK_API_URL + link);
    addTimeout(request);

    final CloseableHttpResponse response = client.execute(request);
    final StatusLine statusLine = response.getStatusLine();
    final HttpEntity responseEntity = response.getEntity();
    final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
    EntityUtils.consume(responseEntity);
    if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
      throw new IOException("Stepik returned non 200 status code " + responseString);
    }
    return deserializeStepikResponse(container, responseString);
  }

  private static void addTimeout(@NotNull HttpGet request) {
    int connectionTimeoutMs = TIMEOUT_SECONDS * 1000;
    RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(connectionTimeoutMs)
            .setConnectTimeout(connectionTimeoutMs)
            .setSocketTimeout(connectionTimeoutMs)
            .build();
    request.setConfig(requestConfig);
  }

  static <T> T deserializeStepikResponse(Class<T> container, String responseString) {
    Gson gson = createGson();
    return gson.fromJson(responseString, container);
  }

  public static Gson createGson() {
    return new GsonBuilder()
        .registerTypeAdapter(StepikWrappers.StepOptions.class, new StepikStepOptionsAdapter())
        .registerTypeAdapter(Lesson.class, new StepikLessonAdapter())
        .registerTypeAdapter(StepikWrappers.Reply.class, new StepikReplyAdapter())
        .registerTypeAdapter(StepikCourse.class, new StepikRemoteInfoAdapter())
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  }

  private static void initializeClient() {
    if (ourClient == null) {
      final HttpClientBuilder builder = getBuilder();
      ourClient = builder.build();
    }
  }

  @NotNull
  static HttpClientBuilder getBuilder() {
    final HttpClientBuilder builder = HttpClients.custom().setSSLContext(CertificateManager.getInstance().getSslContext()).
      setMaxConnPerRoute(100000).setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE).setUserAgent(getUserAgent());

    final HttpConfigurable proxyConfigurable = HttpConfigurable.getInstance();
    final List<Proxy> proxies = proxyConfigurable.getOnlyBySettingsSelector().select(URI.create(StepikNames.STEPIK_URL));
    final InetSocketAddress address = proxies.size() > 0 ? (InetSocketAddress)proxies.get(0).address() : null;
    if (address != null) {
      builder.setProxy(new HttpHost(address.getHostName(), address.getPort()));
    }
    final ConfirmingTrustManager trustManager = CertificateManager.getInstance().getTrustManager();
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
      builder.setSSLContext(sslContext);
    }
    catch (NoSuchAlgorithmException | KeyManagementException e) {
      LOG.error(e.getMessage());
    }
    return builder;
  }

  @NotNull
  private static String getUserAgent() {
    String pluginVersion = EduVersions.pluginVersion(EduNames.PLUGIN_ID);
    String version = pluginVersion == null ? "unknown" : pluginVersion;

    return String.format("%s/version(%s)/%s/%s", StepikNames.PLUGIN_NAME, version, System.getProperty("os.name"),
                         PlatformUtils.getPlatformPrefix());
  }

  static boolean isTokenUpToDate(@NotNull String token) {
    if (token.isEmpty()) return false;

    final List<BasicHeader> headers = new ArrayList<>();
    headers.add(new BasicHeader("Authorization", "Bearer " + token));
    headers.add(new BasicHeader("Content-type", StepikNames.CONTENT_TYPE_APP_JSON));
    CloseableHttpClient httpClient = getBuilder().setDefaultHeaders(headers).build();

    try {
      final StepikWrappers.AuthorWrapper wrapper =
        getFromStepik(StepikNames.CURRENT_USER, StepikWrappers.AuthorWrapper.class, httpClient);
      if (wrapper != null && !wrapper.users.isEmpty()) {
        StepicUser user = wrapper.users.get(0);
        return user != null && !user.isGuest();
      }
      else {
        throw new IOException(wrapper == null ? "Got a null current user" : "Got an empty wrapper");
      }
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
      return false;
    }
  }
}
