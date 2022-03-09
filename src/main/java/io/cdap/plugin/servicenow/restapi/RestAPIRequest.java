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

import com.google.common.base.Joiner;
import org.apache.http.HttpEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pojo class to represent Rest API Request.
 */
public class RestAPIRequest {
  private String url;
  private Map<String, String> headers;
  private HttpEntity entity;
  private String[] responseHeaders;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public HttpEntity getEntity() {
    return entity;
  }

  public void setEntity(HttpEntity entity) {
    this.entity = entity;
  }

  public String[] getResponseHeaders() {
    return responseHeaders;
  }

  public void setResponseHeaders(String[] responseHeaders) {
    this.responseHeaders = responseHeaders;
  }

  /**
   * The Builder class for RestAPIRequest.
   */
  public abstract static class Builder {
    protected String url;
    protected Map<String, String> headers = new HashMap<>();
    protected Map<String, String> parameters = new HashMap<>();
    protected HttpEntity entity;
    protected String[] responseHeaders;

    protected Builder(String url) {
      this.url = url;
    }

    public Builder setAuthHeader(String token) {
      this.headers.put("Authorization", String.format("Bearer %s", token));
      return this;
    }

    public Builder setAcceptHeader(String acceptHeaderValue) {
      this.headers.put("Accept", acceptHeaderValue);
      return this;
    }

    public Builder setContentTypeHeader(String contentTypeHeaderValue) {
      this.headers.put("Content-type", contentTypeHeaderValue);
      return this;
    }

    public Builder setEntity(HttpEntity entity) {
      this.entity = entity;
      return this;
    }

    public Builder setResponseHeaders(String... responseHeaders) {
      this.responseHeaders = responseHeaders;
      return this;
    }

    /**
     * Builds an instance of RestAPIRequest.
     *
     * @return An instance of RestAPIRequest
     */
    public RestAPIRequest build() {
      RestAPIRequest request = new RestAPIRequest();

      request.setUrl(attachQueryParamsToUrl(this.url, this.parameters));
      request.setHeaders(this.headers);

      if (this.entity != null) {
        request.setEntity(this.entity);
      }
      request.setResponseHeaders(this.responseHeaders);

      return request;
    }

    private String attachQueryParamsToUrl(String url, Map<String, String> queryParams) {
      List<String> parameters = new ArrayList<>();
      queryParams.forEach((k, v) -> parameters.add(String.format("%s=%s", k, v)));
      return String.format("%s%s", url, parameters.isEmpty() ? "" : "?" + Joiner.on('&').join(parameters));
    }
  }
}
