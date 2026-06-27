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

package io.cdap.plugin.servicenow.connector;

import io.cdap.cdap.etl.api.validation.CauseAttributes;
import io.cdap.cdap.etl.api.validation.ValidationFailure;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for the proxy configuration on {@link ServiceNowConnectorConfig}.
 */
public class ServiceNowConnectorConfigProxyTest {

  private static ServiceNowConnectorConfig configWithProxy(String proxyUrl, String proxyUsername,
                                                           String proxyPassword) {
    return new ServiceNowConnectorConfig("clientId", "clientSecret", "https://instance.service-now.com",
                                         "user", "password", proxyUrl, proxyUsername, proxyPassword);
  }

  @Test
  public void testProxyGettersAndDetection() {
    ServiceNowConnectorConfig config = configWithProxy("http://proxy.example.com:8080", "puser", "ppass");
    Assert.assertEquals("http://proxy.example.com:8080", config.getProxyUrl());
    Assert.assertEquals("puser", config.getProxyUsername());
    Assert.assertEquals("ppass", config.getProxyPassword());
    Assert.assertTrue(config.hasProxyConfigured());
  }

  @Test
  public void testNoProxyConfigured() {
    ServiceNowConnectorConfig config = configWithProxy(null, null, null);
    Assert.assertFalse(config.hasProxyConfigured());
    MockFailureCollector collector = new MockFailureCollector();
    config.validateProxyFields(collector);
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testValidProxyPassesValidation() {
    ServiceNowConnectorConfig config = configWithProxy("http://proxy.example.com:8080", "puser", "ppass");
    MockFailureCollector collector = new MockFailureCollector();
    config.validateProxyFields(collector);
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testInvalidProxyUrlFailsValidation() {
    ServiceNowConnectorConfig config = configWithProxy("not a valid url", null, null);
    MockFailureCollector collector = new MockFailureCollector();
    config.validateProxyFields(collector);
    List<ValidationFailure> failures = collector.getValidationFailures();
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals(ServiceNowConstants.PROPERTY_PROXY_URL,
                        failures.get(0).getCauses().get(0).getAttribute(CauseAttributes.STAGE_CONFIG));
  }

  @Test
  public void testProxyUsernameWithoutPasswordFailsValidation() {
    ServiceNowConnectorConfig config = configWithProxy("http://proxy.example.com:8080", "puser", null);
    MockFailureCollector collector = new MockFailureCollector();
    config.validateProxyFields(collector);
    List<ValidationFailure> failures = collector.getValidationFailures();
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals(ServiceNowConstants.PROPERTY_PROXY_PASSWORD,
                        failures.get(0).getCauses().get(0).getAttribute(CauseAttributes.STAGE_CONFIG));
  }

  @Test
  public void testProxyPasswordWithoutUsernameFailsValidation() {
    ServiceNowConnectorConfig config = configWithProxy("http://proxy.example.com:8080", null, "ppass");
    MockFailureCollector collector = new MockFailureCollector();
    config.validateProxyFields(collector);
    List<ValidationFailure> failures = collector.getValidationFailures();
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals(ServiceNowConstants.PROPERTY_PROXY_USERNAME,
                        failures.get(0).getCauses().get(0).getAttribute(CauseAttributes.STAGE_CONFIG));
  }
}
