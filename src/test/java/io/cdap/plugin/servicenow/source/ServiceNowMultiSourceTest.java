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
import io.cdap.cdap.etl.api.batch.BatchSourceContext;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.cdap.cdap.etl.mock.common.MockArguments;
import io.cdap.cdap.etl.mock.common.MockPipelineConfigurer;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ServiceNowMultiSourceTest {

  private static final String CLIENT_ID = System.getProperty("servicenow.test.clientId");
  private static final String CLIENT_SECRET = System.getProperty("servicenow.test.clientSecret");
  private static final String REST_API_ENDPOINT = System.getProperty("servicenow.test.restApiEndpoint");
  private static final String USER = System.getProperty("servicenow.test.user");
  private static final String PASSWORD = System.getProperty("servicenow.test.password");
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

  @Test
  public void testConfigurePipeline() {
    Schema inputSchema = null;
    Map<String, Object> plugins = new HashMap<>();
    MockFailureCollector mockFailureCollector = new MockFailureCollector();
    MockPipelineConfigurer mockPipelineConfigurer = new MockPipelineConfigurer(inputSchema, plugins);
    serviceNowMultiSource.configurePipeline(mockPipelineConfigurer);
    Assert.assertEquals(0, mockFailureCollector.getValidationFailures().size());
  }

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

  @Test
  public void testPrepareRun() throws Exception {
    MockFailureCollector mockFailureCollector = new MockFailureCollector();
    MockArguments mockArguments = new MockArguments();
    BatchSourceContext context = Mockito.mock(BatchSourceContext.class);
    Mockito.when(context.getFailureCollector()).thenReturn(mockFailureCollector);
    Mockito.when(context.getArguments()).thenReturn(mockArguments);
    serviceNowMultiSource.prepareRun(context);
    Assert.assertEquals(0, mockFailureCollector.getValidationFailures().size());
  }
}
