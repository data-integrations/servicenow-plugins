package io.cdap.plugin.servicenow.apiclient;

import org.apache.http.HttpResponse;

import javax.annotation.Nullable;

public class ServiceNowAPIException extends Exception {

  @Nullable private final HttpResponse httpResponse;
  private final boolean isErrorRetryable;

  public ServiceNowAPIException(
      Throwable t, @Nullable HttpResponse httpResponse, boolean isErrorRetryable) {
    super(t);
    this.httpResponse = httpResponse;
    this.isErrorRetryable = isErrorRetryable;
  }

  public ServiceNowAPIException(
      String message, @Nullable HttpResponse httpResponse, boolean isErrorRetryable) {
    super(message);
    this.httpResponse = httpResponse;
    this.isErrorRetryable = isErrorRetryable;
  }

  public HttpResponse getHttpResponse() {
    return httpResponse;
  }

  public boolean isErrorRetryable() {
    return isErrorRetryable;
  }
}
