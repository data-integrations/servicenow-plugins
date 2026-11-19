package io.cdap.plugin.servicenow.restapi;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.WaitStrategy;
import io.cdap.plugin.servicenow.apiclient.ServiceNowAPIException;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIRequestBuilder;
import io.cdap.plugin.servicenow.connector.ServiceNowConnectorConfig;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.net.SocketException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  RestAPIClient.class,
  HttpClientBuilder.class,
  RestAPIResponse.class,
  ServiceNowTableAPIClientImpl.class,
  EntityUtils.class
})
public class RestAPIClientTest {

  private static final long BASE_MULTIPLIER_MS = 4000L;
  private static final long MAX_JITTER_MS = 4000L;

  @Test
  public void testExecuteGet_throwRetryableException() throws IOException {
    CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = Mockito.mock(BasicStatusLine.class);
    Mockito.when(statusLine.getStatusCode()).thenReturn(429);
    Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);

    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any())).thenReturn(httpResponse);

    ServiceNowTableAPIRequestBuilder builder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = builder.build();

    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);
    RestAPIResponse actualResponse = client.executeGet(request);
    Assert.assertNotNull(actualResponse.getException());
    Assert.assertTrue(actualResponse.getException().isErrorRetryable());
  }

  @Test
  public void testExecuteGet_throwNonRetryableException() throws IOException {
    CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = Mockito.mock(BasicStatusLine.class);
    Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);

    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any())).thenReturn(httpResponse);

    ServiceNowTableAPIRequestBuilder builder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = builder.build();

    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);
    RestAPIResponse actualResponse = client.executeGet(request);
    Assert.assertNotNull(actualResponse.getException());
    Assert.assertFalse(actualResponse.getException().isErrorRetryable());
  }

  @Test
  public void testExecuteGet_StatusOk() throws IOException {
    CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = Mockito.mock(BasicStatusLine.class);
    Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);

    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any())).thenReturn(httpResponse);

    PowerMockito.mockStatic(EntityUtils.class);
    PowerMockito.when(EntityUtils.toString(Mockito.any())).thenReturn("{}");

    ServiceNowTableAPIRequestBuilder builder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = builder.build();

    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);
    client.executeGet(request);
  }

  @Test
  public void testExecuteGet_throwConnectTimeoutException_markAsRetryable() throws IOException {
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any()))
      .thenThrow(new ConnectTimeoutException("Connection timed out"));

    ServiceNowTableAPIRequestBuilder builder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = builder.build();

    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);
    RestAPIResponse actualResponse = client.executeGet(request);
    Assert.assertNotNull(actualResponse.getException());
    Assert.assertTrue(actualResponse.getException().isErrorRetryable());
    Throwable ex = actualResponse.getException().getCause();
    Assert.assertTrue("Expected ConnectTimeoutException or similar, got: " + ex,
      ex instanceof ConnectTimeoutException || ex.getMessage().toLowerCase().contains("Connection timed out"));
  }

  @Test
  public void testExecuteGet_throwSocketException_markAsRetryable() throws IOException {
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any()))
      .thenThrow(new SocketException());

    ServiceNowTableAPIRequestBuilder builder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = builder.build();

    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);
    RestAPIResponse actualResponse = client.executeGet(request);
    Assert.assertNotNull(actualResponse.getException());
    Assert.assertTrue(actualResponse.getException().isErrorRetryable());
    Throwable ex = actualResponse.getException().getCause();
    Assert.assertTrue("Expected SocketException or similar, got: " + ex, ex instanceof SocketException);
  }

  @Test
  public void testGetRetryer_computeSleepTime_appliesExponentialMultiplierAndJitter() throws Exception {
    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);
    Retryer<RestAPIResponse> retryer = client.getRetryer();
    WaitStrategy waitStrategy = Whitebox.getInternalState(retryer, "waitStrategy");
    Attempt<?> firstAttempt = Mockito.mock(Attempt.class);
    Mockito.when(firstAttempt.getAttemptNumber()).thenReturn(1L);
    Attempt<?> secondAttempt = Mockito.mock(Attempt.class);
    Mockito.when(secondAttempt.getAttemptNumber()).thenReturn(2L);

    long firstAttemptSleepMs = waitStrategy.computeSleepTime(firstAttempt);
    long secondAttemptSleepMs = waitStrategy.computeSleepTime(secondAttempt);

    long expectedFirstBaseMs = BASE_MULTIPLIER_MS * 2;
    long expectedSecondBaseMs = BASE_MULTIPLIER_MS * 4;
    Assert.assertTrue(
        "Expected first attempt sleep between 8000ms and 12000ms, got: " + firstAttemptSleepMs,
        firstAttemptSleepMs >= expectedFirstBaseMs
            && firstAttemptSleepMs <= expectedFirstBaseMs + MAX_JITTER_MS);
    Assert.assertTrue(
        "Expected second attempt sleep between 16000ms and 20000ms, got: " + secondAttemptSleepMs,
        secondAttemptSleepMs >= expectedSecondBaseMs
            && secondAttemptSleepMs <= expectedSecondBaseMs + MAX_JITTER_MS);
  }

  @Test
  public void testGetRetryer_computeSleepTimeExceedingMaxWait_capsExponentialBackoffAndAddsJitter()
      throws Exception {
    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);
    Retryer<RestAPIResponse> retryer = client.getRetryer();
    WaitStrategy waitStrategy = Whitebox.getInternalState(retryer, "waitStrategy");
    Attempt<?> fifthAttempt = Mockito.mock(Attempt.class);
    Mockito.when(fifthAttempt.getAttemptNumber()).thenReturn(5L);

    long fifthAttemptSleepMs = waitStrategy.computeSleepTime(fifthAttempt);

    long maxBaseWaitMs = ServiceNowConstants.WAIT_TIME;
    Assert.assertTrue(
        "Expected capped attempt sleep between 120000ms and 124000ms, got: " + fifthAttemptSleepMs,
        fifthAttemptSleepMs >= maxBaseWaitMs && fifthAttemptSleepMs <= maxBaseWaitMs + MAX_JITTER_MS);
  }

  @Test
  public void testExecuteGetWithRetries_retryableErrorThenSuccess_retriesWithBackoffAndReturnsResponse()
      throws Exception {
    CloseableHttpResponse retryableHttpResponse = Mockito.mock(CloseableHttpResponse.class);
    StatusLine retryableStatusLine = Mockito.mock(BasicStatusLine.class);
    Mockito.when(retryableStatusLine.getStatusCode()).thenReturn(429);
    Mockito.when(retryableHttpResponse.getStatusLine()).thenReturn(retryableStatusLine);
    CloseableHttpResponse okHttpResponse = Mockito.mock(CloseableHttpResponse.class);
    StatusLine okStatusLine = Mockito.mock(BasicStatusLine.class);
    Mockito.when(okStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    Mockito.when(okHttpResponse.getStatusLine()).thenReturn(okStatusLine);
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any()))
        .thenReturn(retryableHttpResponse)
        .thenReturn(okHttpResponse);
    PowerMockito.mockStatic(EntityUtils.class);
    PowerMockito.when(EntityUtils.toString(Mockito.any())).thenReturn("{\"result\":[]}");
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = requestBuilder.build();
    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true) {
      @Override
      protected Retryer<RestAPIResponse> getRetryer() {
        return getRetryer(attempt -> { });
      }
    };

    RestAPIResponse actualResponse = client.executeGetWithRetries(request);

    Assert.assertFalse(actualResponse.hasException());
    Assert.assertEquals("{\"result\":[]}", actualResponse.getResponseBody());
    Mockito.verify(httpClient, Mockito.times(2)).execute(Mockito.any());
  }

  @Test
  public void testExecuteGetWithRetries_retriesExhausted_throwsServiceNowAPIException() throws Exception {
    CloseableHttpResponse retryableHttpResponse = Mockito.mock(CloseableHttpResponse.class);
    StatusLine retryableStatusLine = Mockito.mock(BasicStatusLine.class);
    Mockito.when(retryableStatusLine.getStatusCode()).thenReturn(429);
    Mockito.when(retryableHttpResponse.getStatusLine()).thenReturn(retryableStatusLine);
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any())).thenReturn(retryableHttpResponse);
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = requestBuilder.build();
    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true) {
      @Override
      protected Retryer<RestAPIResponse> getRetryer() {
        return getRetryer(attempt -> { });
      }
    };

    ServiceNowAPIException thrownException = null;
    try {
      client.executeGetWithRetries(request);
    } catch (ServiceNowAPIException e) {
      thrownException = e;
    }

    Assert.assertNotNull(thrownException);
    Assert.assertTrue(thrownException.isErrorRetryable());
    Mockito.verify(httpClient, Mockito.times(ServiceNowConstants.MAX_NUMBER_OF_RETRY_ATTEMPTS))
        .execute(Mockito.any());
  }

  @Test
  public void testExecuteGetWithRetries_nonRetryableError_throwsImmediatelyWithoutRetry() throws Exception {
    CloseableHttpResponse errorHttpResponse = Mockito.mock(CloseableHttpResponse.class);
    StatusLine errorStatusLine = Mockito.mock(BasicStatusLine.class);
    Mockito.when(errorStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    Mockito.when(errorHttpResponse.getStatusLine()).thenReturn(errorStatusLine);
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    HttpClientBuilder httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
    PowerMockito.mockStatic(HttpClientBuilder.class);
    PowerMockito.when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
    Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
    Mockito.when(httpClient.execute(Mockito.any())).thenReturn(errorHttpResponse);
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = requestBuilder.build();
    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl client = new ServiceNowTableAPIClientImpl(config, true);

    ServiceNowAPIException thrownException = null;
    try {
      client.executeGetWithRetries(request);
    } catch (ServiceNowAPIException e) {
      thrownException = e;
    }

    Assert.assertNotNull(thrownException);
    Assert.assertFalse(thrownException.isErrorRetryable());
    Mockito.verify(httpClient, Mockito.times(1)).execute(Mockito.any());
  }
}
