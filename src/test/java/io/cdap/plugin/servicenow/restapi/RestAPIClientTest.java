package io.cdap.plugin.servicenow.restapi;

import com.github.rholder.retry.WaitStrategies;
import com.github.rholder.retry.WaitStrategy;
import io.cdap.plugin.servicenow.apiclient.ServiceNowAPIException;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIRequestBuilder;
import io.cdap.plugin.servicenow.connector.ServiceNowConnectorConfig;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.http.HttpResponse;
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

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  RestAPIClient.class,
  HttpClientBuilder.class,
  RestAPIResponse.class,
  ServiceNowTableAPIClientImpl.class,
  EntityUtils.class,
  WaitStrategies.class
})
public class RestAPIClientTest {

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
    Assert.assertNull(actualResponse.getException().getMessage());
    Assert.assertTrue(actualResponse.getException().isErrorRetryable());
    Throwable ex = actualResponse.getException().getCause();
    Assert.assertTrue("Expected SocketException or similar, got: " + ex, ex instanceof SocketException);
  }

  @Test
  public void testExecuteGetWithRetries_throwExecutionException_unwrapCause() throws IOException {
    ServiceNowTableAPIRequestBuilder builder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = builder.build();
    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl clientSpy = Mockito.spy(new ServiceNowTableAPIClientImpl(config, true));
    IOException rootCause = new IOException("Unexpected transport failure");
    Mockito.doThrow(rootCause).when(clientSpy).executeGet(request);
    ServiceNowAPIException actualException = null;

    try {
      clientSpy.executeGetWithRetries(request);
      Assert.fail("Expected ServiceNowAPIException");
    } catch (ServiceNowAPIException e) {
      actualException = e;
    }

    Assert.assertNotNull(actualException);
    Assert.assertSame("Expected unwrapped IOException cause instead of ExecutionException",
                      rootCause, actualException.getCause());
    Assert.assertEquals("Unexpected transport failure", actualException.getMessage());
    Assert.assertEquals("Unexpected transport failure", actualException.getUnderlyingMessage());
    Assert.assertFalse(actualException.isErrorRetryable());
  }

  @Test
  public void testExecuteGetWithRetries_whenRetriesExhausted_marksErrorNonRetryable() throws IOException {
    WaitStrategy noWait = WaitStrategies.noWait();
    PowerMockito.mockStatic(WaitStrategies.class);
    PowerMockito.when(WaitStrategies.exponentialWait(Mockito.anyLong(), Mockito.any()))
      .thenReturn(noWait);

    ServiceNowTableAPIRequestBuilder builder = new ServiceNowTableAPIRequestBuilder("url");
    RestAPIRequest request = builder.build();
    ServiceNowConnectorConfig config = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl clientSpy = Mockito.spy(new ServiceNowTableAPIClientImpl(config, true));

    HttpResponse mockHttpResponse = Mockito.mock(HttpResponse.class);
    StatusLine statusLine = Mockito.mock(StatusLine.class);
    Mockito.when(statusLine.getStatusCode()).thenReturn(429);
    Mockito.when(mockHttpResponse.getStatusLine()).thenReturn(statusLine);
    ServiceNowAPIException retryableEx = new ServiceNowAPIException("Too Many Requests", mockHttpResponse);
    RestAPIResponse errorResponse = new RestAPIResponse(Collections.emptyMap(), null, retryableEx);
    Mockito.doReturn(errorResponse).when(clientSpy).executeGet(request);

    ServiceNowAPIException actualException = null;
    try {
      clientSpy.executeGetWithRetries(request);
      Assert.fail("Expected ServiceNowAPIException");
    } catch (ServiceNowAPIException e) {
      actualException = e;
    }

    Assert.assertNotNull(actualException);
    Assert.assertEquals("Too Many Requests", actualException.getMessage());
    Assert.assertEquals(429, actualException.getStatusCode());
    Assert.assertFalse(actualException.isErrorRetryable());
    Mockito.verify(clientSpy, Mockito.times(ServiceNowConstants.MAX_NUMBER_OF_RETRY_ATTEMPTS)).executeGet(request);
  }
}
