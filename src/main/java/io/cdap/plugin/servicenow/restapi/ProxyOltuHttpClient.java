/*
 * Copyright © 2026 Cask Data, Inc.
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

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.oltu.oauth2.client.HttpClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponseFactory;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of Oltu's {@link HttpClient} backed by Apache {@link CloseableHttpClient}. It is used so that the
 * OAuth token request is executed through the same proxy-aware HTTP client as the rest of the ServiceNow API calls.
 */
public class ProxyOltuHttpClient implements HttpClient {

  private final HttpClientBuilder httpClientBuilder;

  public ProxyOltuHttpClient(HttpClientBuilder httpClientBuilder) {
    this.httpClientBuilder = httpClientBuilder;
  }

  @Override
  public <T extends OAuthClientResponse> T execute(OAuthClientRequest request, Map<String, String> headers,
                                                   String requestMethod, Class<T> responseClass)
    throws OAuthSystemException, OAuthProblemException {

    HttpRequestBase httpRequest;
    if (OAuth.HttpMethod.POST.equalsIgnoreCase(requestMethod) || OAuth.HttpMethod.PUT.equalsIgnoreCase(requestMethod)) {
      HttpPost httpPost = new HttpPost(request.getLocationUri());
      if (request.getBody() != null) {
        httpPost.setEntity(new StringEntity(request.getBody(), StandardCharsets.UTF_8));
      }
      httpRequest = httpPost;
    } else {
      httpRequest = new HttpGet(request.getLocationUri());
    }

    if (request.getHeaders() != null) {
      request.getHeaders().forEach(httpRequest::setHeader);
    }
    if (headers != null) {
      headers.forEach(httpRequest::setHeader);
    }
    if (httpRequest.getFirstHeader(OAuth.HeaderType.CONTENT_TYPE) == null) {
      httpRequest.setHeader(OAuth.HeaderType.CONTENT_TYPE, OAuth.ContentType.URL_ENCODED);
    }

    try (CloseableHttpClient httpClient = httpClientBuilder.build();
         CloseableHttpResponse response = httpClient.execute(httpRequest)) {
      int responseCode = response.getStatusLine().getStatusCode();
      String contentType = response.getEntity() != null && response.getEntity().getContentType() != null
        ? response.getEntity().getContentType().getValue() : null;
      String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
      return OAuthClientResponseFactory.createCustomResponse(body, contentType, responseCode,
                                                             extractHeaders(response), responseClass);
    } catch (IOException e) {
      throw new OAuthSystemException(e);
    }
  }

  @Override
  public void shutdown() {
    // No persistent resources are held; a fresh client is created per request and closed in execute().
  }

  private Map<String, List<String>> extractHeaders(CloseableHttpResponse response) {
    Map<String, List<String>> responseHeaders = new java.util.HashMap<>();
    for (Header header : response.getAllHeaders()) {
      List<String> values = responseHeaders.computeIfAbsent(header.getName(), k -> new ArrayList<>());
      values.add(header.getValue());
    }
    return Collections.unmodifiableMap(responseHeaders);
  }
}
