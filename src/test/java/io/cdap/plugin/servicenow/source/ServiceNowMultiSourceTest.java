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

package io.cdap.plugin.servicenow.source;

import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.batch.BatchSourceContext;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.cdap.cdap.etl.mock.common.MockArguments;
import io.cdap.cdap.etl.mock.common.MockPipelineConfigurer;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.junit.*;
import org.junit.internal.AssumptionViolatedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ServiceNowMultiSourceTest {

  private static final String CLIENT_ID = "clientId"; //System.getProperty("servicenow.test.clientId");
  private static final String CLIENT_SECRET = "clientSecret"; //System.getProperty("servicenow.test.clientSecret");
  private static final String REST_API_ENDPOINT = "https://ven05127.service-now.com"; //System.getProperty("servicenow.test.restApiEndpoint");
  private static final String USER = "user"; //System.getProperty("servicenow.test.user");
  private static final String PASSWORD = "password"; //System.getProperty("servicenow.test.password");
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowMultiSourceTest.class);
  private ServiceNowMultiSource serviceNowMultiSource;
  private ServiceNowMultiSourceConfig serviceNowMultiSourceConfig;

  @Before
  public void initialize() {
    try {
      Assume.assumeNotNull(CLIENT_ID, CLIENT_SECRET, REST_API_ENDPOINT, USER, PASSWORD);
      serviceNowMultiSourceConfig = ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("referenceName")
        .setRestApiEndpoint(REST_API_ENDPOINT)
        .setUser(USER)
        .setPassword(PASSWORD)
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setTableNames("sys_user")
        .setValueType("Actual")
        .setStartDate("2021-01-01")
        .setEndDate("2022-02-18")
        .setTableNameField("tablename")
        .buildMultiSource();
      serviceNowMultiSource = new ServiceNowMultiSource(serviceNowMultiSourceConfig);
    } catch (AssumptionViolatedException e) {
      LOG.warn("Service Now batch Source tests are skipped. ");
      throw e;
    }
  }

  @Ignore
  @Test
  public void testConfigurePipeline() throws OAuthProblemException, OAuthSystemException {
    Schema inputSchema = null;
    Map<String, Object> plugins = new HashMap<>();
    MockFailureCollector mockFailureCollector = new MockFailureCollector();
    MockPipelineConfigurer mockPipelineConfigurer = new MockPipelineConfigurer(inputSchema, plugins);
    FailureCollector failureCollector = Mockito.mock(FailureCollector.class);

    ServiceNowTableAPIClientImpl serviceNowTableAPIClient = Mockito.spy(new ServiceNowTableAPIClientImpl(serviceNowMultiSourceConfig));
    Mockito.when(serviceNowTableAPIClient.getAccessToken()).thenReturn("token");
    serviceNowMultiSource.configurePipeline(mockPipelineConfigurer);
    Assert.assertEquals(0, mockFailureCollector.getValidationFailures().size());
  }

  @Ignore
  @Test
  public void testConfigurePipelineWithEmptyTable() {
    serviceNowMultiSourceConfig = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setReferenceName("referenceName")
      .setRestApiEndpoint(REST_API_ENDPOINT)
      .setUser(USER)
      .setPassword(PASSWORD)
      .setClientId(CLIENT_ID)
      .setClientSecret(CLIENT_SECRET)
      .setTableNames("clm_contract_history")
      .setValueType("Actual")
      .setStartDate("2019-01-01")
      .setEndDate("2022-02-18")
      .setTableNameField("tablename")
      .buildMultiSource();
    serviceNowMultiSource = new ServiceNowMultiSource(serviceNowMultiSourceConfig);
    Schema inputSchema = null;
    Map<String, Object> plugins = new HashMap<>();
    MockFailureCollector mockFailureCollector = new MockFailureCollector();
    MockPipelineConfigurer mockPipelineConfigurer = new MockPipelineConfigurer(inputSchema, plugins);
    try {
      serviceNowMultiSource.configurePipeline(mockPipelineConfigurer);
      Assert.fail("Exception is not thrown for Non-Empty Tables");
    } catch (ValidationException e) {
      Assert.assertEquals(0, mockFailureCollector.getValidationFailures().size());
      Assert.assertEquals("Errors were encountered during validation.",
                          e.getMessage());
    }

  }

  @Ignore
  @Test
  public void testPrepareRun() throws Exception {
    MockFailureCollector mockFailureCollector = new MockFailureCollector();
    MockArguments mockArguments = new MockArguments();
    BatchSourceContext context = Mockito.mock(BatchSourceContext.class);
    Mockito.when(context.getFailureCollector()).thenReturn(mockFailureCollector);
    Mockito.when(context.getArguments()).thenReturn(mockArguments);
    ServiceNowMultiSourceConfig config = Mockito.mock(ServiceNowMultiSourceConfig.class);

    ServiceNowTableAPIClientImpl serviceNowTableAPIClient = Mockito.spy(new ServiceNowTableAPIClientImpl(config));
    Mockito.doReturn("token").when(serviceNowTableAPIClient).getAccessToken();

    Mockito.doNothing().when(config).validate(mockFailureCollector);
    OAuthClient client = Mockito.spy(new OAuthClient(new URLConnectionClient()));
    OAuthClientRequest request = OAuthClientRequest.tokenLocation(REST_API_ENDPOINT)
            .setGrantType(GrantType.PASSWORD)
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setUsername(USER)
            .setPassword(PASSWORD)
            .buildBodyMessage();

    OAuthJSONAccessTokenResponse accessTokenResponse = new OAuthJSONAccessTokenResponse();
    Mockito.when(client.accessToken(request, OAuth.HttpMethod.POST, OAuthJSONAccessTokenResponse.class)).thenReturn(accessTokenResponse);
    Mockito.when(accessTokenResponse.getAccessToken()).thenReturn("token");

    serviceNowMultiSource.prepareRun(context);
    Assert.assertEquals(0, mockFailureCollector.getValidationFailures().size());
  }
}
