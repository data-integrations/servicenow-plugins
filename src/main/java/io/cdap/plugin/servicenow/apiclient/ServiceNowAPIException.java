package io.cdap.plugin.servicenow.apiclient;

import io.cdap.plugin.servicenow.util.ServiceNowConstants;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Custom Exception class for propagating API errors/exceptions back to caller.
 */
public class ServiceNowAPIException extends Exception {

  @Nullable private final HttpResponse httpResponse;
  @Nullable private final Boolean manualRetry;

  private static final Set<Integer> RETRYABLE_CODES = new HashSet<>(Arrays.asList(429,
      HttpStatus.SC_BAD_GATEWAY,
      HttpStatus.SC_SERVICE_UNAVAILABLE,
      HttpStatus.SC_REQUEST_TIMEOUT,
      HttpStatus.SC_GATEWAY_TIMEOUT));

  public ServiceNowAPIException(String message, @Nullable HttpResponse httpResponse) {
    this(message, null, httpResponse, null);
  }

  public ServiceNowAPIException(Throwable t, @Nullable HttpResponse httpResponse) {
    this(t != null ? t.getMessage() : null, t, httpResponse, null);
  }

  public ServiceNowAPIException(String message, Throwable t,
      @Nullable HttpResponse httpResponse) {
    this(message, t, httpResponse, null);
  }

  public ServiceNowAPIException(String message, Throwable t,
      @Nullable HttpResponse httpResponse, boolean manualRetry) {
    this(message, t, httpResponse, Boolean.valueOf(manualRetry));
  }

  private ServiceNowAPIException(String message, Throwable t,
      @Nullable HttpResponse httpResponse, @Nullable Boolean manualRetry) {
    super(message, t);
    this.httpResponse = httpResponse;
    this.manualRetry = manualRetry;
  }

  public String getUnderlyingMessage() {
    if (this.getCause() != null) {
      return this.getCause().getMessage();
    }
    return null;
  }

  @Nullable
  public HttpResponse getHttpResponse() {
    return httpResponse;
  }

  public int getStatusCode() {
    if (httpResponse != null && httpResponse.getStatusLine() != null) {
      return httpResponse.getStatusLine().getStatusCode();
    }
    return 0;
  }

  public boolean isErrorRetryable() {
    if (manualRetry != null) {
      return manualRetry;
    }
    Throwable t = this.getCause();
    return t instanceof OAuthSystemException
        || t instanceof ConnectTimeoutException
        || t instanceof SocketException
        || (this.getMessage() != null
        && this.getMessage().contains(ServiceNowConstants.MAXIMUM_EXECUTION_TIME_EXCEEDED))
        || RETRYABLE_CODES.contains(getStatusCode());
  }
}
