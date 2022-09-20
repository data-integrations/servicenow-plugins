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
package io.cdap.plugin.servicenow.sink;

import io.cdap.plugin.servicenow.sink.model.RestRequest;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class RestRequestTest {

  @Test
  public void testRestRequests() {
    RestRequest request = new RestRequest();
    request.setId("id");
    request.setBody("body");
    request.setUrl("Servicenow.com");
    request.setMethod("POST");
    Assert.assertEquals("id", request.getId());
    Assert.assertEquals("body", request.getBody());
    Assert.assertEquals("Servicenow.com", request.getUrl());
    Assert.assertEquals("POST", request.getMethod());
  }

  @Test
  public void testGetExcludeResponseHeaders() {
    RestRequest request = new RestRequest();
    request.setExcludeResponseHeaders(true);
    Assert.assertTrue(request.getExcludeResponseHeaders());
  }

  @Test
  public void testHeader() {
    RestRequest request = new RestRequest();
    List<Header> headers = new ArrayList<>();
    Header contentTypeHeader = new BasicHeader("Content-Type", "application/json");
    Header acceptHeader = new BasicHeader("Accept", "application/json");
    headers.add(contentTypeHeader);
    headers.add(acceptHeader);
    request.setHeaders(headers);
    Assert.assertEquals(2, request.getHeaders().size());
    Assert.assertEquals(acceptHeader, request.getHeaders().get(1));
  }
}
