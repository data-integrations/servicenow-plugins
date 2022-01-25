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

import io.cdap.cdap.etl.api.validation.ValidationFailure;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

/**
 * Tests for {@link ServiceNowMultiSourceConfig}.
 */
public class ServiceNowMultiSourceConfigTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() {
    Assert.assertEquals("Table Names",
      (new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42", "Client Secret",
        "https://dev115725.service-now.com", "User", "password", "42", "2021-12-30", "2021-12-31",
        "Table Names")).getTableNames());
  }

  @Test
  public void testValidate() {
    ServiceNowMultiSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setReferenceName("test")
      .setRestApiEndpoint("https://dev115725.service-now.com")
      .setUser("User")
      .setPassword("password")
      .setClientId("42")
      .setClientSecret("Client Secret")
      .setTableNames("sys_user")
      .setValueType("Actual")
      .setStartDate("2021-12-30")
      .setEndDate("2021-12-31")
      .setTableNameField("tablename")
      .buildMultiSource();

    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    config.validate(mockFailureCollector);
    Assert.assertEquals(2, mockFailureCollector.getValidationFailures().size());
  }

  @Test
  public void testValidateTableNames() {
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig = new ServiceNowMultiSourceConfig("Reference Name",
      "Table Name Field", "42", "Client Secret", "https://dev115725.service-now.com/", "admin", "6qa8xrCJzWTV",
      "Actual",
      "2021-12-30", "2021-12-31", "Table Names");
    serviceNowMultiSourceConfig.validateTableNames(new MockFailureCollector("Stage Name"));
    Assert.assertEquals("42", serviceNowMultiSourceConfig.getClientId());
    Assert.assertEquals("Table Name Field", serviceNowMultiSourceConfig.tableNameField);
    Assert.assertEquals("admin", serviceNowMultiSourceConfig.getUser());
    Assert.assertEquals("Table Names", serviceNowMultiSourceConfig.getTableNames());
    Assert.assertEquals("2021-12-30", serviceNowMultiSourceConfig.getStartDate());
    Assert.assertEquals("https://dev115725.service-now.com/", serviceNowMultiSourceConfig.getRestApiEndpoint());
    Assert.assertEquals("6qa8xrCJzWTV", serviceNowMultiSourceConfig.getPassword());
    Assert.assertEquals("Client Secret", serviceNowMultiSourceConfig.getClientSecret());
    Assert.assertEquals("2021-12-31", serviceNowMultiSourceConfig.getEndDate());
  }

  @Test
  public void testValidateTableNamesWhenTableNamesAreEmpty() {
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig = new ServiceNowMultiSourceConfig("Reference Name",
      "Table Name Field", "42", "Client Secret", "https://dev115725.service-now.com/", "User", "password", "42",
      "2021-12-30", "2021-12-31", "");
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig.validateTableNames(mockFailureCollector);
    List<ValidationFailure> validationFailures = mockFailureCollector.getValidationFailures();
    Assert.assertEquals(1, validationFailures.size());
    ValidationFailure getResult = validationFailures.get(0);
    List<ValidationFailure.Cause> causes = getResult.getCauses();
    Assert.assertEquals(1, causes.size());
    Assert.assertEquals("Table names must be specified.", getResult.getMessage());
    Assert.assertEquals("Stage Name", getResult.getCorrectiveAction());
    Assert.assertEquals("Table names must be specified. Stage Name", getResult.getFullMessage());
    Assert.assertEquals("tableNames", causes.get(0).getAttributes().get("stageConfig"));
  }
}

