/*
 * Copyright © 2022 Cask Data, Inc.
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
package io.cdap.plugin.servicenow.sink.model;

import com.google.gson.annotations.SerializedName;
import org.apache.http.Header;

import java.util.List;

/**
 *  Single Rest Request Model
 */
public class RestRequest {

  private String id;
  private String url;
  private String method;
  private String body;

  private List<Header> headers;

  @SerializedName("exclude_response_headers")
  private Boolean excludeResponseHeaders = true;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public List<Header> getHeaders() {
    return headers;
  }

  public void setHeaders(List<Header> headers) {
    this.headers = headers;
  }

  public Boolean getExcludeResponseHeaders() {
    return excludeResponseHeaders;
  }

  public void setExcludeResponseHeaders(Boolean excludeResponseHeaders) {
    this.excludeResponseHeaders = excludeResponseHeaders;
  }

}
