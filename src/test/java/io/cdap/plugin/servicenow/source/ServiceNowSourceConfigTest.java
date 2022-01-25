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

package io.cdap.plugin.servicenow.source;

import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.validation.CauseAttributes;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.source.util.SourceApplication;
import io.cdap.plugin.servicenow.source.util.SourceQueryMode;
import io.cdap.plugin.servicenow.source.util.SourceValueType;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

/**
 * Tests for {@link ServiceNowSourceConfig}.
 */
public class ServiceNowSourceConfigTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testQueryModeTable() {
    SourceQueryMode queryMode = SourceQueryMode.TABLE;
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setQueryMode("Table")
      .build();

    MockFailureCollector collector = new MockFailureCollector();
    Assert.assertEquals(queryMode, config.getQueryMode(collector));
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testQueryModeReporting() {
    SourceQueryMode queryMode = SourceQueryMode.REPORTING;
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setQueryMode("Reporting")
      .build();

    MockFailureCollector collector = new MockFailureCollector();
    Assert.assertEquals(queryMode, config.getQueryMode(collector));
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testQueryModeNull() {
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setQueryMode(null)
      .build();
    MockFailureCollector collector = new MockFailureCollector();

    try {
      config.getQueryMode(collector);
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_QUERY_MODE, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testQueryModeInvalid() {
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setQueryMode("Invalid")
      .build();
    MockFailureCollector collector = new MockFailureCollector();

    try {
      config.getQueryMode(collector);
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_QUERY_MODE, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testApplicationValid() {
    SourceApplication application = SourceApplication.CONTRACT_MANAGEMENT;
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setApplicationName("Contract Management")
      .build();

    MockFailureCollector collector = new MockFailureCollector();
    Assert.assertEquals(application, config.getApplicationName(collector));
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testApplicationInvalid() {
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setApplicationName(null)
      .build();
    MockFailureCollector collector = new MockFailureCollector();

    try {
      config.getApplicationName(collector);
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_APPLICATION_NAME, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testValueTypeValid() {
    SourceValueType application = SourceValueType.SHOW_DISPLAY_VALUE;
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setValueType("Display")
      .build();

    MockFailureCollector collector = new MockFailureCollector();
    Assert.assertEquals(application, config.getValueType(collector));
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testValueTypeInvalid() {
    ServiceNowSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setValueType(null)
      .build();
    MockFailureCollector collector = new MockFailureCollector();

    try {
      config.getValueType(collector);
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_VALUE_TYPE, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateClientIdNull() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(null)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_CLIENT_ID, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateClientSecretNull() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(null)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_CLIENT_SECRET, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateApiEndpointNull() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(null)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_API_ENDPOINT, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateUserNull() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(null)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_USER, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  @Test
  public void testValidatePasswordNull() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(null)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_PASSWORD, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  @Test
  public void testValidCredentials() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint((ServiceNowSourceConfigHelper.TEST_API_ENDPOINT))
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .build(), collector);

    config.validate(collector);

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testTableModeMissingTableName() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .setQueryMode("Table")
      .setTableName(null)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_TABLE_NAME, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testReportingModeMissingApplication() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .setQueryMode("Reporting")
      .setApplicationName(null)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_APPLICATION_NAME, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testReportingModeMissingTableNameField() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .setQueryMode("Reporting")
      .setApplicationName("Contract Management")
      .setTableNameField(null)
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testStartDateInvalid() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .setQueryMode("Table")
      .setTableName("ast-contract")
      .setStartDate("2020")
      .setEndDate("2020-03-01")
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_CLIENT_ID, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  @Test
  public void testEndDateInvalid() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .setQueryMode("Table")
      .setTableName("ast-contract")
      .setStartDate("2020-03-01")
      .setEndDate("2020")
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_CLIENT_ID, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  @Test
  public void testEndDateLessThanStartDate() {
    MockFailureCollector collector = new MockFailureCollector();
    ServiceNowSourceConfig config = withServiceNowValidationMock(ServiceNowSourceConfigHelper.newConfigBuilder()
      .setClientId(ServiceNowSourceConfigHelper.TEST_CLIENT_ID)
      .setClientSecret(ServiceNowSourceConfigHelper.TEST_CLIENT_SECRET)
      .setRestApiEndpoint(ServiceNowSourceConfigHelper.TEST_API_ENDPOINT)
      .setUser(ServiceNowSourceConfigHelper.TEST_USER)
      .setPassword(ServiceNowSourceConfigHelper.TEST_PASSWORD)
      .setQueryMode("Table")
      .setTableName("ast-contract")
      .setStartDate("2020-03-01")
      .setEndDate("2020-02-29")
      .build(), collector);

    try {
      config.validate(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_CLIENT_ID, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }

    Assert.assertEquals(2, collector.getValidationFailures().size());
  }

  private ServiceNowSourceConfig withServiceNowValidationMock(ServiceNowSourceConfig config,
                                                              FailureCollector collector) {
    ServiceNowSourceConfig spy = Mockito.spy(config);
    Mockito.doNothing().when(spy).validateServiceNowConnection(collector);
    return spy;
  }

  @Test
  public void testValidateTableNames() {
    MockFailureCollector collector = new MockFailureCollector();
    String tableNames = "tablenames";
    ServiceNowMultiSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setTableNames(tableNames)
      .buildMultiSource();
    config.validateTableNames(collector);

    Assert.assertEquals(1, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateTableNamesWhenEmpty() {
    MockFailureCollector collector = new MockFailureCollector();
    String tableNames = "";
    ServiceNowMultiSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setTableNames(tableNames)
      .buildMultiSource();

    try {
      config.validateTableNames(collector);
      collector.getOrThrowException();
    } catch (ValidationException e) {
      Assert.assertEquals(ServiceNowConstants.PROPERTY_TABLE_NAMES, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }
    Assert.assertEquals(1, collector.getValidationFailures().size());
  }
}
