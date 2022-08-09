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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An abstract class to call Rest API.
 */
public abstract class RestAPIClient {
  private static final Logger LOG = LoggerFactory.getLogger(RestAPIClient.class);
  /**
   * Executes the Rest API request and returns the response.
   *
   * @param request the Rest API request
   * @return an instance of RestAPIResponse object.
   */
  public RestAPIResponse executeGet(RestAPIRequest request) {
    HttpGet httpGet = new HttpGet(request.getUrl());
    request.getHeaders().entrySet().forEach(e -> httpGet.addHeader(e.getKey(), e.getValue()));
    RestAPIResponse apiResponse = null;

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
        apiResponse = RestAPIResponse.parse(httpResponse, request.getResponseHeaders());
      }
    } catch (Exception e) {
      apiResponse = RestAPIResponse.defaultErrorResponse(e.getMessage());
    }

    return apiResponse;
  }

  /**
   * Executes the Rest API request and returns the response.
   *
   * @param request the Rest API request
   * @return an instance of RestAPIResponse object.
   */
  public RestAPIResponse executePost(RestAPIRequest request) throws IOException {
    HttpPost httpPost = new HttpPost(request.getUrl());
    request.getHeaders().entrySet().forEach(e -> httpPost.addHeader(e.getKey(), e.getValue()));
    httpPost.setEntity(request.getEntity());
    RestAPIResponse apiResponse;

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
        apiResponse = RestAPIResponse.parse(httpResponse, request.getResponseHeaders());
      }
    } catch (IOException e) {
      // We're retrying all transport exceptions while executing the HTTP POST method and the generic transport
      // exceptions in HttpClient are represented by the standard java.io.IOException class
      // https://hc.apache.org/httpclient-legacy/exception-handling.html
      throw e;
    } catch (Exception e) {
      LOG.error("Exception while executing post request", e);
      apiResponse = RestAPIResponse.defaultErrorResponse(e.getMessage());
    }

    return apiResponse;
  }

  /**
   * Generates access token and returns the same.
   *
   * @param restApiEndpoint The rest API endpoint for ServiceNow
   * @param clientId The Client Id for ServiceNow
   * @param clientSecret The Client Secret for ServiceNow
   * @param user the user id for ServiceNow
   * @param password The password for ServiceNow
   * @return The access token
   * @throws OAuthSystemException
   * @throws OAuthProblemException
   */
  protected String generateAccessToken(String restApiEndpoint, String clientId, String clientSecret, String user,
                                       String password) throws OAuthSystemException, OAuthProblemException {
    String token = "NO-VALUE";

    OAuthClient client = new OAuthClient(new URLConnectionClient());
    OAuthClientRequest request = OAuthClientRequest.tokenLocation(restApiEndpoint)
      .setGrantType(GrantType.PASSWORD)
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setUsername(user)
      .setPassword(password)
      .buildBodyMessage();

    token = client.accessToken(request, OAuth.HttpMethod.POST, OAuthJSONAccessTokenResponse.class).getAccessToken();
    return token;
  }
}
