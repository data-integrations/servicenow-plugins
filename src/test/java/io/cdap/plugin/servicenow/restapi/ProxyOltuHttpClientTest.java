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
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;

/**
 * Tests for {@link ProxyOltuHttpClient}.
 */
public class ProxyOltuHttpClientTest {

  private static final String TOKEN_URL = "https://instance.service-now.com/oauth_token.do";

  private static OAuthClientRequest tokenRequest() throws OAuthSystemException {
    return OAuthClientRequest.tokenLocation(TOKEN_URL)
      .setGrantType(GrantType.PASSWORD)
      .setClientId("clientId")
      .setClientSecret("clientSecret")
      .setUsername("user")
      .setPassword("password")
      .buildBodyMessage();
  }

  private static CloseableHttpResponse mockResponse(int statusCode, String body) throws IOException {
    CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
    Mockito.when(response.getStatusLine())
      .thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, "OK"));
    Mockito.when(response.getEntity()).thenReturn(new StringEntity(body, ContentType.APPLICATION_JSON));
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[0]);
    return response;
  }

  private static HttpClientBuilder mockBuilderReturning(CloseableHttpClient httpClient) {
    HttpClientBuilder builder = Mockito.mock(HttpClientBuilder.class);
    Mockito.when(builder.build()).thenReturn(httpClient);
    return builder;
  }

  @Test
  public void testExecutePostParsesTokenAndUsesPost() throws Exception {
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    CloseableHttpResponse response = mockResponse(200, "{\"access_token\":\"tok\",\"token_type\":\"Bearer\"}");
    ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
    Mockito.when(httpClient.execute(captor.capture())).thenReturn(response);

    ProxyOltuHttpClient client = new ProxyOltuHttpClient(mockBuilderReturning(httpClient));
    OAuthJSONAccessTokenResponse tokenResponse =
      client.execute(tokenRequest(), null, OAuth.HttpMethod.POST, OAuthJSONAccessTokenResponse.class);

    Assert.assertEquals("tok", tokenResponse.getAccessToken());

    HttpUriRequest executed = captor.getValue();
    Assert.assertTrue(executed instanceof HttpPost);
    Assert.assertEquals(TOKEN_URL, executed.getURI().toString());
    // The token request is form-encoded, so a Content-Type header must be present.
    Assert.assertNotNull(executed.getFirstHeader(OAuth.HeaderType.CONTENT_TYPE));
  }

  @Test
  public void testExecuteGetUsesGet() throws Exception {
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    CloseableHttpResponse response = mockResponse(200, "{\"access_token\":\"tok\"}");
    ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
    Mockito.when(httpClient.execute(captor.capture())).thenReturn(response);

    ProxyOltuHttpClient client = new ProxyOltuHttpClient(mockBuilderReturning(httpClient));
    client.execute(tokenRequest(), null, OAuth.HttpMethod.GET, OAuthJSONAccessTokenResponse.class);

    Assert.assertTrue(captor.getValue() instanceof HttpGet);
  }

  @Test(expected = OAuthSystemException.class)
  public void testIOExceptionIsWrapped() throws Exception {
    CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
    Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenThrow(new IOException("boom"));

    ProxyOltuHttpClient client = new ProxyOltuHttpClient(mockBuilderReturning(httpClient));
    client.execute(tokenRequest(), null, OAuth.HttpMethod.POST, OAuthJSONAccessTokenResponse.class);
  }
}
