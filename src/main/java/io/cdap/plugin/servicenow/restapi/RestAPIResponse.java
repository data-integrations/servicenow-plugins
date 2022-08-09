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
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pojo class to capture the API response.
 */
public class RestAPIResponse {
  private static List<Integer> successCodes = new ArrayList<Integer>() {
    {
      add(HttpStatus.SC_OK);
    }
  };
  private static final String JSON_ERROR_RESPONSE_TEMPLATE = "{\n" +
    "    \"error\": {\n" +
    "        \"message\": \"%s\",\n" +
    "        \"detail\": null\n" +
    "    },\n" +
    "    \"status\": \"failure\"\n" +
    "}";
  private int httpStatus;
  private Map<String, String> headers;
  private String responseBody;
  private boolean isRetryable;

  public RestAPIResponse(int httpStatus, Map<String, String> headers, String responseBody) {
    this.httpStatus = httpStatus;
    this.headers = headers;
    this.responseBody = responseBody;
    this.checkRetryable();
  }

  public static RestAPIResponse defaultErrorResponse(String message) {
    return new RestAPIResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, Collections.emptyMap(),
      String.format(JSON_ERROR_RESPONSE_TEMPLATE, message));
  }

  /**
   * Parses HttpResponse into RestAPIResponse object.
   *
   * @param httpResponse The HttpResponse object to parse
   * @param headerNames The list of header names to be extracted
   * @return An instance of RestAPIResponse object.
   */
  public static RestAPIResponse parse(HttpResponse httpResponse, String... headerNames) {
    List<String> headerNameList = headerNames == null ? Collections.emptyList() : Arrays.asList(headerNames);
    int httpStatus = httpResponse.getStatusLine().getStatusCode();
    Map<String, String> headers = new HashMap<>();

    if (!headerNameList.isEmpty()) {
      headers.putAll(Arrays.stream(httpResponse.getAllHeaders())
        .filter(o -> headerNameList.contains(o.getName()))
        .collect(Collectors.toMap(Header::getName, Header::getValue)));
    }

    String responseBody = "";
    try {
      responseBody = EntityUtils.toString(httpResponse.getEntity());
    } catch (Exception e) {
      httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
      return new RestAPIResponse(httpStatus, headers, String.format(JSON_ERROR_RESPONSE_TEMPLATE, e.getMessage()));
    }

    return new RestAPIResponse(httpStatus, headers, responseBody);
  }

  public static RestAPIResponse parse(HttpResponse httpResponse) {
    return parse(httpResponse, new String[0]);
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public boolean isSuccess() {
    boolean isSuccess = false;
    // ServiceNow Rest API may return 200 OK response with an error message in the response body.
    // Normally we would expect non-200 response code if there's an error.
    if (this.isRetryable) {
      isSuccess = false;
    } else if (successCodes.contains(getHttpStatus())) {
      isSuccess = true;
    }
    return isSuccess;
  }

  private void checkRetryable() {
    Gson gson = new Gson();
    JsonObject jo = gson.fromJson(this.responseBody, JsonObject.class);
    if (jo.get(ServiceNowConstants.STATUS) != null &&
      jo.get(ServiceNowConstants.STATUS).getAsString().equals(ServiceNowConstants.FAILURE) &&
      jo.getAsJsonObject(ServiceNowConstants.ERROR).get(ServiceNowConstants.MESSAGE).getAsString()
        .contains(ServiceNowConstants.MAXIMUM_EXECUTION_TIME_EXCEEDED)) {
      isRetryable = true;
    }
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public boolean isRetryable() {
    return isRetryable;
  }
}
