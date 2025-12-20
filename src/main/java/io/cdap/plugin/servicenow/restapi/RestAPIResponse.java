/*
 * Copyright © 2020 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin.servicenow.restapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.cdap.plugin.servicenow.apiclient.ServiceNowAPIException;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Pojo class to capture the API response.
 */
public class RestAPIResponse {
  private static final Logger LOG = LoggerFactory.getLogger(RestAPIResponse.class);
  private static final Gson GSON = new Gson();
  private static final String HTTP_ERROR_MESSAGE = "Http call to ServiceNow instance returned status code %d.";
  private static final String REST_ERROR_MESSAGE = "Rest Api response has errors. Error message: %s.";
  private static final Set<Integer> SUCCESS_CODES = new HashSet<>(Arrays.asList(HttpStatus.SC_CREATED,
                                                                                HttpStatus.SC_OK));
  private static final long MAX_PAGE_BYTES = 50L * 1024 * 1024; // 50 MB (Upper Bound)
  private final Map<String, String> headers;
  @Nullable private final ServiceNowAPIException exception;

  // New: store byte array
  private byte[] responseBody;

  public RestAPIResponse(
    Map<String, String> headers,
    byte[] responseBody,
    @Nullable ServiceNowAPIException exception) {
    this.headers = headers;
    this.responseBody = responseBody;
    this.exception = exception;
  }

  /**
   * Parses HttpResponse into RestAPIResponse object when no errors occur.
   * The RESTAPIResponse contains the HTTP response body as a stream. This stream is:
   * single-use, forward-only and owned by the caller. Caller is responsible for consuming and closing it.
   *
   * Throws a {@link ServiceNowAPIException}.
   *
   * @param httpResponse The HttpResponse object to parse
   * @param headerNames The list of header names to be extracted
   * @return An instance of RestAPIResponse object.
   */
  public static RestAPIResponse parse(HttpResponse httpResponse, String... headerNames) {
    List<String> headerNameList =
        headerNames == null ? Collections.emptyList() : Arrays.asList(headerNames);
    Map<String, String> headers = new HashMap<>();

    if (!headerNameList.isEmpty()) {
      headers.putAll(
          Arrays.stream(httpResponse.getAllHeaders())
              .filter(o -> headerNameList.contains(o.getName()))
              .collect(Collectors.toMap(Header::getName, Header::getValue)));
    }

    ServiceNowAPIException serviceNowAPIException = validateHttpResponse(httpResponse);
    if (serviceNowAPIException != null) {
      return new RestAPIResponse(headers, null, serviceNowAPIException);
    }
    try {
      return prepareResponseStream(httpResponse, headers, serviceNowAPIException);
    } catch (IOException e) {
      return new RestAPIResponse(headers, null, new ServiceNowAPIException(e, httpResponse));
    }
  }

  public static RestAPIResponse prepareResponseStream(HttpResponse httpResponse, Map<String, String> headers,
      ServiceNowAPIException serviceNowAPIException) throws IOException {
    HttpEntity httpEntity = httpResponse.getEntity();
    byte[] responseBody = new byte[0];
    InputStream inputStream;
    if (httpEntity != null) {
      inputStream = httpEntity.getContent();
      BoundedInputStream boundedInputStream = new BoundedInputStream(
        inputStream, MAX_PAGE_BYTES + 1); // +1 to detect overflow
      responseBody = IOUtils.toByteArray(boundedInputStream);
      LOG.info("RAW JSON: {}", new String(responseBody, StandardCharsets.UTF_8));
      if (responseBody.length > MAX_PAGE_BYTES) {
        throw new IOException(
          "ServiceNow page exceeded max allowed size: " + MAX_PAGE_BYTES);
      }
      return new RestAPIResponse(headers, responseBody, serviceNowAPIException);
    } else {
      return new RestAPIResponse(headers, responseBody, serviceNowAPIException);
    }
  }

  public static RestAPIResponse parse(HttpResponse httpResponse) throws IOException {
    return parse(httpResponse, new String[0]);
  }

  private static ServiceNowAPIException validateRestApiResponse(
      HttpResponse response, String responseBody) {
    JsonObject jo = GSON.fromJson(responseBody, JsonObject.class);
    // check if status is "failure"
    String status = null;
    if (jo.get(ServiceNowConstants.STATUS) != null) {
      status = jo.get(ServiceNowConstants.STATUS).getAsString();
    }
    if (!ServiceNowConstants.FAILURE.equals(status)) {
      return null;
    }
    // check if failure is retryable
    String errorMessage = jo.getAsJsonObject(ServiceNowConstants.ERROR).get(ServiceNowConstants.MESSAGE).getAsString();
    return new ServiceNowAPIException(String.format(REST_ERROR_MESSAGE, errorMessage), response);
  }

  private static ServiceNowAPIException validateHttpResponse(HttpResponse response) {
    int code = response.getStatusLine().getStatusCode();
    if (SUCCESS_CODES.contains(code)) {
      return null;
    }
    return new ServiceNowAPIException(
        String.format(HTTP_ERROR_MESSAGE, code), response);
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  @Nullable
  public byte[] getResponseBody() {
    return responseBody;
  }

  /**
   * Returns a fresh InputStream for the response body. Caller must close the stream.
   * @return InputStream
   */
  public InputStream getBodyAsStream() {
    return responseBody == null ? null : new ByteArrayInputStream(responseBody);
  }

  @Nullable
  public ServiceNowAPIException getException() {
    return exception;
  }

  public boolean hasException() {
    return exception != null;
  }
}
