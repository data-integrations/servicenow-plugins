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
import io.cdap.plugin.servicenow.apiclient.NonRetryableException;
import io.cdap.plugin.servicenow.apiclient.RetryableException;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pojo class to capture the API response.
 */
public class RestAPIResponse {
  private static final Gson GSON = new Gson();
  private static final String HTTP_ERROR_MESSAGE = "Http call to ServiceNow instance returned status code %d.";
  private static final String REST_ERROR_MESSAGE = "Rest Api response has errors. Error message: %s.";
  private static final Set<Integer> SUCCESS_CODES = new HashSet<>(Collections.singletonList(HttpStatus.SC_OK));
  private static final Set<Integer> RETRYABLE_CODES = new HashSet<>(Arrays.asList(429,
          HttpStatus.SC_BAD_GATEWAY,
          HttpStatus.SC_SERVICE_UNAVAILABLE,
          HttpStatus.SC_REQUEST_TIMEOUT,
          HttpStatus.SC_GATEWAY_TIMEOUT));
  private final int httpStatus;
  private final Map<String, String> headers;
  private final String responseBody;

  public RestAPIResponse(int httpStatus, Map<String, String> headers, String responseBody) {
    this.httpStatus = httpStatus;
    this.headers = headers;
    this.responseBody = responseBody;
  }

  /**
   * Parses HttpResponse into RestAPIResponse object when no errors occur.
   * Throws a {@link RetryableException} if the error is retryable.
   * Throws an {@link NonRetryableException} if the error is not retryable.
   *
   * @param httpResponse The HttpResponse object to parse
   * @param headerNames The list of header names to be extracted
   * @return An instance of RestAPIResponse object.
   */
  public static RestAPIResponse parse(HttpResponse httpResponse, String... headerNames) throws IOException {
    validateHttpResponse(httpResponse);
    List<String> headerNameList = headerNames == null ? Collections.emptyList() : Arrays.asList(headerNames);
    int httpStatus = httpResponse.getStatusLine().getStatusCode();
    Map<String, String> headers = new HashMap<>();

    if (!headerNameList.isEmpty()) {
      headers.putAll(Arrays.stream(httpResponse.getAllHeaders())
        .filter(o -> headerNameList.contains(o.getName()))
        .collect(Collectors.toMap(Header::getName, Header::getValue)));
    }
    String responseBody = EntityUtils.toString(httpResponse.getEntity());
    validateRestApiResponse(responseBody);
    return new RestAPIResponse(httpStatus, headers, responseBody);
  }

  public static RestAPIResponse parse(HttpResponse httpResponse) throws IOException {
    return parse(httpResponse, new String[0]);
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  private static void validateRestApiResponse(String responseBody) {
    JsonObject jo = GSON.fromJson(responseBody, JsonObject.class);
    // check if status is "failure"
    String status = null;
    if (jo.get(ServiceNowConstants.STATUS) != null) {
      status = jo.get(ServiceNowConstants.STATUS).getAsString();
    }
    if (!ServiceNowConstants.FAILURE.equals(status)) {
      return;
    }
    // check if failure is retryable
    String errorMessage = jo.getAsJsonObject(ServiceNowConstants.ERROR).get(ServiceNowConstants.MESSAGE).getAsString();
    if (errorMessage.contains(ServiceNowConstants.MAXIMUM_EXECUTION_TIME_EXCEEDED)) {
      throw new RetryableException(String.format(REST_ERROR_MESSAGE, errorMessage));
    } else {
      throw new NonRetryableException(String.format(REST_ERROR_MESSAGE, errorMessage));
    }
  }

  private static void validateHttpResponse(HttpResponse response) {
    int code = response.getStatusLine().getStatusCode();
    if (SUCCESS_CODES.contains(code)) {
      return;
    }
    if (RETRYABLE_CODES.contains(code)) {
      throw new RetryableException(String.format(HTTP_ERROR_MESSAGE, code));
    }
    throw new NonRetryableException(String.format(HTTP_ERROR_MESSAGE, code));
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getResponseBody() {
    return responseBody;
  }
}
