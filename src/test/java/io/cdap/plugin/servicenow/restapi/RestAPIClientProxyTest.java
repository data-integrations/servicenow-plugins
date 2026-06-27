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

import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Tests for the proxy wiring in {@link RestAPIClient#getHttpClientBuilder()}.
 *
 * <p>{@link HttpClientBuilder#setProxy(HttpHost)} and related setters are {@code final}, so they cannot be mocked.
 * Instead these tests build a real {@link HttpClientBuilder} and inspect its private {@code proxy} and
 * {@code credentialsProvider} fields to assert that the proxy was wired in.</p>
 */
public class RestAPIClientProxyTest {

  /**
   * Minimal concrete subclass to exercise the abstract {@link RestAPIClient}.
   */
  private static final class TestRestAPIClient extends RestAPIClient {
    private TestRestAPIClient(String proxyUrl, String proxyUsername, String proxyPassword) {
      super(proxyUrl, proxyUsername, proxyPassword);
    }
  }

  private static Object readField(HttpClientBuilder builder, String fieldName) throws Exception {
    Field field = HttpClientBuilder.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(builder);
  }

  @Test
  public void testProxyWithCredentialsSetsProxyAndCredentials() throws Exception {
    HttpClientBuilder builder =
      new TestRestAPIClient("http://proxy.example.com:8080", "puser", "ppass").getHttpClientBuilder();

    HttpHost proxy = (HttpHost) readField(builder, "proxy");
    Assert.assertNotNull(proxy);
    Assert.assertEquals("proxy.example.com", proxy.getHostName());
    Assert.assertEquals(8080, proxy.getPort());
    Assert.assertNotNull(readField(builder, "credentialsProvider"));
  }

  @Test
  public void testProxyWithoutCredentialsSetsProxyOnly() throws Exception {
    HttpClientBuilder builder =
      new TestRestAPIClient("http://proxy.example.com:8080", null, null).getHttpClientBuilder();

    Assert.assertNotNull(readField(builder, "proxy"));
    Assert.assertNull(readField(builder, "credentialsProvider"));
    // The builder should still produce a usable client.
    Assert.assertNotNull(builder.build());
  }

  @Test
  public void testNoProxyDoesNotSetProxy() throws Exception {
    HttpClientBuilder builder = new TestRestAPIClient(null, null, null).getHttpClientBuilder();

    Assert.assertNull(readField(builder, "proxy"));
    Assert.assertNull(readField(builder, "credentialsProvider"));
    Assert.assertNotNull(builder.build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidProxyUrlThrows() {
    new TestRestAPIClient("not a valid url", null, null).getHttpClientBuilder();
  }
}
