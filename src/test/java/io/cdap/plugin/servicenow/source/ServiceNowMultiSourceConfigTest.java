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

import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.validation.ValidationFailure;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.source.util.SourceValueType;

import org.apache.http.HttpStatus;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceNowMultiSourceConfig}.
 */
public class ServiceNowMultiSourceConfigTest {

  private static final String CLIENT_ID = System.getProperty("servicenow.test.clientId");
  private static final String CLIENT_SECRET = System.getProperty("servicenow.test.clientSecret");
  private static final String REST_API_ENDPOINT = System.getProperty("servicenow.test.restApiEndpoint");
  private static final String USER = System.getProperty("servicenow.test.user");
  private static final String PASSWORD = System.getProperty("servicenow.test.password");
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowInputFormat.class);
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private ServiceNowMultiSourceConfig serviceNowMultiSourceConfig;

  @Before
  public void initializeTests() {
    try {
      serviceNowMultiSourceConfig =
        ServiceNowSourceConfigHelper.newConfigBuilder()
          .setReferenceName("Reference Name")
          .setRestApiEndpoint(REST_API_ENDPOINT)
          .setUser(USER)
          .setPassword(PASSWORD)
          .setClientId(CLIENT_ID)
          .setClientSecret(CLIENT_SECRET)
          .setTableNames("sys_user")
          .setValueType("Actual")
          .setStartDate("2021-12-30")
          .setEndDate("2021-12-31")
          .setTableNameField("tablename")
          .buildMultiSource();
      Assume.assumeNotNull(CLIENT_ID, CLIENT_SECRET, REST_API_ENDPOINT, USER, PASSWORD);
    } catch (AssumptionViolatedException e) {
      LOG.warn("Service Now Multi Source tests are skipped. ");
      throw e;
    }
  }

  @Test
  public void testConstructor() {
    Assert.assertEquals("Table Names",
                        (new ServiceNowMultiSourceConfig("Reference Name",
                                                         "Table Name Field",
                                                         "42", "Client Secret",
                                                         "https://dev115725.service-now.com",
                                                         "User",
                                                         "password",
                                                         "42",
                                                         "2021-12-30",
                                                         "2021-12-31",
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
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig =
      new ServiceNowMultiSourceConfig("Reference Name",
                                      "Table Name Field",
                                      "42",
                                      "Client Secret",
                                      "https://dev115725.service-now.com/",
                                      "admin",
                                      "6qa8xrCJzWTV",
                                      "Actual",
                                      "2021-12-30", "2021-12-31",
                                      "Table Names");
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
  public void testValidateWhenFieldsAreMacro() throws OAuthProblemException, OAuthSystemException {
    ServiceNowMultiSourceConfig config = mock(ServiceNowMultiSourceConfig.class);
    PluginConfig pluginConfig = mock(PluginConfig.class);
    ServiceNowTableAPIClientImpl serviceNowTableAPIClient = mock(ServiceNowTableAPIClientImpl.class);
    MockFailureCollector mockFailureCollector = new MockFailureCollector();
    Mockito.when(config.containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAMES)).thenReturn(Boolean.TRUE);
    Mockito.when(pluginConfig.containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAMES)).thenReturn(Boolean.TRUE);

    int httpStatus = HttpStatus.SC_BAD_REQUEST;
    Map<String, String> headers = new HashMap<>();
    String responseBody = "{\n" +
      "    \"result\": []\n" +
      "}";
    RestAPIResponse restAPIResponse = new RestAPIResponse(httpStatus, headers, responseBody);
    Mockito.when(serviceNowTableAPIClient.getAccessToken()).thenReturn("token");
    Mockito.when(serviceNowTableAPIClient.executeGet(Mockito.any())).thenReturn(restAPIResponse);

    serviceNowMultiSourceConfig.validateTableNames(mockFailureCollector);
    assertEquals(0, mockFailureCollector.getValidationFailures().size());
  }


  @Test
  public void testValidateTableNamesWhenTableNamesAreEmpty() {
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig =
      new ServiceNowMultiSourceConfig("Reference Name",
                                      "tablename",
                                      "fc7e6822e4fc8d10a509b583b67b5dd3",
                                      "N$phjaGWsr",
                                      "https://ven05127.service-now.com",
                                      "pipeline" + ".user.1 ",
                                      "Cloudsufi@1234",
                                      SourceValueType.SHOW_ACTUAL_VALUE.getValueType(),
                                      "2021-12-30",
                                      "2021-12-31",
                                      "");
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

  @Test
  public void testValidateTableNamesWhenTableHasNoData() {
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig =
      new ServiceNowMultiSourceConfig("Reference Name",
                                      "tablename",
                                      "fc7e6822e4fc8d10a509b583b67b5dd3",
                                      "N$phjaGWsr",
                                      "https://ven05127.service-now.com",
                                      "pipeline" + ".user.1 ",
                                      "Cloudsufi@1234",
                                      SourceValueType.SHOW_ACTUAL_VALUE.getValueType(),
                                      "2021-12-30",
                                      "2021-12-31", "sys_user");
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig.validateTableNames(mockFailureCollector);
    List<ValidationFailure> validationFailures = mockFailureCollector.getValidationFailures();
    Assert.assertEquals(1, validationFailures.size());
    ValidationFailure getResult = validationFailures.get(0);
    List<ValidationFailure.Cause> causes = getResult.getCauses();
    Assert.assertEquals(5, causes.size());
    Assert.assertEquals("Unable to connect to ServiceNow Instance.", getResult.getMessage());
    Assert.assertEquals("Stage Name", getResult.getCorrectiveAction());
    //Assert.assertEquals("Stage 'Ensure properties like Client ID, Client Secret, API Endpoint, User Name, Password " +
    //  "are correct.' encountered : Unable to connect to ServiceNow Instance]. Stage Name", getResult.getFullMessage
    //  ());
    Assert.assertEquals("clientId", causes.get(0).getAttributes().get("stageConfig"));
  }

  @Test
  public void testValidateTableNamesWhenTableNameIsInvalid() {
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig =
      new ServiceNowMultiSourceConfig("Reference Name", "tablename",
                                      "fc7e6822e4fc8d10a509b583b67b5dd3", "N$phjaGWsr",
                                      "https://ven05127.service-now.com", "pipeline" + ".user.1 ",
                                      "Cloudsufi@1234", SourceValueType.SHOW_ACTUAL_VALUE.getValueType(),
                                      "2021-12-30", "2021-12-31", "invalid_table");
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig.validateTableNames(mockFailureCollector);
    List<ValidationFailure> validationFailures = mockFailureCollector.getValidationFailures();
    Assert.assertEquals(1, validationFailures.size());
    ValidationFailure getResult = validationFailures.get(0);
    List<ValidationFailure.Cause> causes = getResult.getCauses();
    Assert.assertEquals(5, causes.size());
    Assert.assertEquals("Unable to connect to ServiceNow Instance.", getResult.getMessage());
    Assert.assertEquals("Stage Name", getResult.getCorrectiveAction());
    //Assert.assertEquals("Stage 'Ensure properties like Client ID, Client Secret, API Endpoint, User Name, Password " +
    //  "are correct.' encountered : Unable to connect to ServiceNow Instance]. Stage Name", getResult.getFullMessage
    //  ());
    Assert.assertEquals("clientId", causes.get(0).getAttributes().get("stageConfig"));
  }

}

