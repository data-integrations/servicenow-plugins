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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
  private static final Gson GSON = new Gson();
  private static final String HTTP_ERROR_MESSAGE = "Http call to ServiceNow instance returned status code %d.";
  private static final String REST_ERROR_MESSAGE = "Rest Api response has errors. Error message: %s.";
  private static final Set<Integer> SUCCESS_CODES = new HashSet<>(Arrays.asList(HttpStatus.SC_CREATED,
                                                                                HttpStatus.SC_OK));
  private final Map<String, String> headers;
  // Deprecated: storing full body as String can cause OOM
  @Deprecated
  private String responseBody;
  @Nullable private final ServiceNowAPIException exception;

  // New: store InputStream for streaming consumption
  private InputStream inputStream;

  public RestAPIResponse(
      Map<String, String> headers,
      @Nullable String responseBody,
      InputStream inputStream,
      @Nullable ServiceNowAPIException exception) {
    this.headers = headers;
    this.responseBody = responseBody;
    this.inputStream = inputStream;
    this.exception = exception;
  }

  public RestAPIResponse(
    Map<String, String> headers,
    InputStream inputStream,
    @Nullable ServiceNowAPIException exception) {
    this.headers = headers;
    this.inputStream = inputStream;
    this.exception = exception;
  }

  /**
   * Parses HttpResponse into RestAPIResponse object when no errors occur.
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
      return new RestAPIResponse(headers, null, null, serviceNowAPIException);
    }
    /*try {
      responseBody = EntityUtils.toString(httpResponse.getEntity());
    } catch (IOException e) {
      return new RestAPIResponse(headers, null, null, new ServiceNowAPIException(e, httpResponse));
    }*/
    try {
      return prepareResponseWithBodyAndStream(httpResponse, headers, serviceNowAPIException);
    } catch (IOException e) {
      return new RestAPIResponse(headers, null, null, new ServiceNowAPIException(e, httpResponse));
    }
  }

  public static RestAPIResponse prepareResponseWithBodyAndStream(HttpResponse httpResponse, Map<String, String> headers,
      ServiceNowAPIException serviceNowAPIException) throws IOException {
    HttpEntity httpEntity = httpResponse.getEntity();
    if (httpEntity != null) {
      try (InputStream inputStream = httpEntity.getContent();
           ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

        // Copy the InputStream into the ByteArrayOutputStream
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data)) != -1) {
          buffer.write(data, 0, bytesRead);
        }
        // Convert the buffer to a String for the responseBody
        String responseBody = buffer.toString(String.valueOf(StandardCharsets.UTF_8));
        serviceNowAPIException = validateRestApiResponse(httpResponse, responseBody);
        // Create a new InputStream from the buffer for further processing
        InputStream reusableStream = new ByteArrayInputStream(buffer.toByteArray());
        // return new RestAPIResponse(headers, responseBody, serviceNowAPIException);
        return new RestAPIResponse(headers, responseBody, reusableStream, serviceNowAPIException);
      }
    } else {
      return new RestAPIResponse(headers, null, null, serviceNowAPIException);
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
  public String getResponseBody() {
    return responseBody;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  @Nullable
  public ServiceNowAPIException getException() {
    return exception;
  }

  public boolean hasException() {
    return exception != null;
  }
}
