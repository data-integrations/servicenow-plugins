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
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ServiceNowMultiSourceConfig}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceNowTableAPIClientImpl.class, ServiceNowBaseSourceConfig.class,
  ServiceNowMultiSourceConfig.class})
public class ServiceNowMultiSourceConfigTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private ServiceNowMultiSourceConfig serviceNowMultiSourceConfig;

  @Test
  public void testConstructor() {
    serviceNowMultiSourceConfig = new ServiceNowMultiSourceConfig("referenceName",
    "tablename", "client_id", "client_secret", "https://example.com",
    "user", "password", "Actual", "2021-12-30", "2021-12-31",
    "sys_user");
    Assert.assertEquals("sys_user", serviceNowMultiSourceConfig.getTableNames());
    Assert.assertEquals("Actual", serviceNowMultiSourceConfig.getValueType().getValueType());
    Assert.assertEquals("2021-12-30", serviceNowMultiSourceConfig.getStartDate());
    Assert.assertEquals("2021-12-31", serviceNowMultiSourceConfig.getEndDate());
    Assert.assertEquals("tablename", serviceNowMultiSourceConfig.getTableNameField());
  }

  @Test
  public void testValidateInvalidConnection() {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig =
      ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("referenceName")
        .setRestApiEndpoint("https://example.com")
        .setUser("user")
        .setPassword("password")
        .setClientId("client_id")
        .setClientSecret("client_secret")
        .setTableNames("sys_user")
        .setValueType("Actual")
        .setStartDate("2021-12-30")
        .setEndDate("2021-12-31")
        .setTableNameField("tablename")
        .buildMultiSource();
    serviceNowMultiSourceConfig.validate(mockFailureCollector);
    Assert.assertEquals(2, mockFailureCollector.getValidationFailures().size());
    Assert.assertEquals("Unable to connect to ServiceNow Instance.",
                        mockFailureCollector.getValidationFailures().get(0).getMessage());
  }

  @Test
  public void testValidate() throws Exception {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig =
      ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("referenceName")
        .setRestApiEndpoint("https://example.com")
        .setUser("user")
        .setPassword("password")
        .setClientId("client_id")
        .setClientSecret("client_secret")
        .setTableNames("sys_user")
        .setValueType("Actual")
        .setStartDate("2021-12-30")
        .setEndDate("2021-12-31")
        .setTableNameField("tablename")
        .buildMultiSource();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    Mockito.when(restApi.getAccessToken()).thenReturn("token");
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).
      withArguments(Mockito.any(ServiceNowBaseSourceConfig.class)).thenReturn(restApi);
    int httpStatus = HttpStatus.SC_OK;
    Map<String, String> headers = new HashMap<>();
    Map<String, Object> map = new HashMap<>();
    List<Map<String, Object>> result = new ArrayList<>();
    map.put("key", "value");
    result.add(map);
    String responseBody = "{\n" +
      "    \"result\": [{\n" +
      "            \"calendar_integration\": \"1\",\n" +
      "            \"country\": \"\",\n" +
      "            \"last_login_time\": \"2019-04-05 22:16:30\",\n" +
      "            \"source\": \"\",\n" +
      "            \"sys_updated_on\": \"2019-04-05 21:54:45\",\n" +
      "            \"building\": \"\",\n" +
      "            \"web_service_access_only\": \"false\",\n" +
      "            \"notification\": \"2\",\n" +
      "            \"enable_multifactor_authn\": \"false\",\n" +
      "            \"sys_updated_by\": \"system\",\n" +
      "            \"sys_created_on\": \"2019-04-05 21:09:12\",\n" +
      "            \"sys_domain\": {\n" +
      "                \"link\": \"https://ven05127.service-now.com/api/now/table/sys_user_group/global\",\n" +
      "                \"value\": \"global\"\n" +
      "            },\n" +
      "            \"state\": \"\",\n" +
      "            \"vip\": \"false\",\n" +
      "            \"sys_created_by\": \"admin\",\n" +
      "            \"zip\": \"\",\n" +
      "            \"home_phone\": \"\",\n" +
      "            \"time_format\": \"\",\n" +
      "            \"last_login\": \"2019-04-05\",\n" +
      "            \"active\": \"true\",\n" +
      "            \"sys_domain_path\": \"/\",\n" +
      "            \"cost_center\": \"\",\n" +
      "            \"phone\": \"\",\n" +
      "            \"name\": \"survey user\",\n" +
      "            \"employee_number\": \"\",\n" +
      "            \"gender\": \"\",\n" +
      "            \"city\": \"\",\n" +
      "            \"failed_attempts\": \"0\",\n" +
      "            \"user_name\": \"survey.user\",\n" +
      "            \"title\": \"\",\n" +
      "            \"sys_class_name\": \"sys_user\",\n" +
      "            \"sys_id\": \"005d500b536073005e0addeeff7b12f4\",\n" +
      "            \"internal_integration_user\": \"false\",\n" +
      "            \"ldap_server\": \"\",\n" +
      "            \"mobile_phone\": \"\",\n" +
      "            \"street\": \"\",\n" +
      "            \"company\": \"\",\n" +
      "            \"department\": \"\",\n" +
      "            \"first_name\": \"survey\",\n" +
      "            \"email\": \"survey.user@email.com\",\n" +
      "            \"introduction\": \"\",\n" +
      "            \"preferred_language\": \"\",\n" +
      "            \"manager\": \"\",\n" +
      "            \"sys_mod_count\": \"1\",\n" +
      "            \"last_name\": \"user\",\n" +
      "            \"photo\": \"\",\n" +
      "            \"avatar\": \"\",\n" +
      "            \"middle_name\": \"\",\n" +
      "            \"sys_tags\": \"\",\n" +
      "            \"time_zone\": \"\",\n" +
      "            \"schedule\": \"\",\n" +
      "            \"date_format\": \"\",\n" +
      "            \"location\": \"\"]\n" +
      "}";
    RestAPIResponse restAPIResponse = new RestAPIResponse(httpStatus, headers, responseBody);
    Mockito.when(restApi.executeGet(Mockito.any())).thenReturn(restAPIResponse);
    Mockito.when(restApi.parseResponseToResultListOfMap(restAPIResponse.getResponseBody())).thenReturn(result);
    serviceNowMultiSourceConfig.validate(mockFailureCollector);
    Assert.assertEquals(0, mockFailureCollector.getValidationFailures().size());

  }

  @Test
  public void testValidateWhenTableIsEmpty() throws Exception {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig =
      ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("referenceName")
        .setRestApiEndpoint("https://example.com")
        .setUser("user")
        .setPassword("password")
        .setClientId("client_id")
        .setClientSecret("client_secret")
        .setTableNames("sys_user")
        .setValueType("Actual")
        .setStartDate("2021-12-30")
        .setEndDate("2021-12-31")
        .setTableNameField("tablename")
        .buildMultiSource();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    Mockito.when(restApi.getAccessToken()).thenReturn("token");
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).
      withArguments(Mockito.any(ServiceNowBaseSourceConfig.class)).thenReturn(restApi);

    int httpStatus = HttpStatus.SC_OK;
    Map<String, String> headers = new HashMap<>();
    String responseBody = "{\n" +
      "    \"result\": []\n" +
      "}";
    RestAPIResponse restAPIResponse = new RestAPIResponse(httpStatus, headers, responseBody);
    Mockito.when(restApi.executeGet(Mockito.any())).thenReturn(restAPIResponse);
    serviceNowMultiSourceConfig.validate(mockFailureCollector);
    Assert.assertEquals(1, mockFailureCollector.getValidationFailures().size());
    Assert.assertEquals("Table: sys_user is empty.", mockFailureCollector.getValidationFailures().get(0).getMessage());
  }

  @Test
  public void testValidateReferenceName() throws Exception {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig =
      ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("Reference Name")
        .setRestApiEndpoint("https://example.com")
        .setUser("user")
        .setPassword("password")
        .setClientId("client_id")
        .setClientSecret("client_secret")
        .setTableNames("sys_user")
        .setValueType("Actual")
        .setStartDate("2021-12-30")
        .setEndDate("2021-12-31")
        .setTableNameField("tablename")
        .buildMultiSource();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    Mockito.when(restApi.getAccessToken()).thenReturn("token");
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).
      withArguments(Mockito.any(ServiceNowBaseSourceConfig.class)).thenReturn(restApi);
    int httpStatus = HttpStatus.SC_OK;
    Map<String, String> headers = new HashMap<>();
    Map<String, Object> map = new HashMap<>();
    List<Map<String, Object>> result = new ArrayList<>();
    map.put("key", "value");
    result.add(map);
    String responseBody = "{\n" +
      "    \"result\": [{\n" +
      "            \"calendar_integration\": \"1\",\n" +
      "            \"country\": \"\",\n" +
      "            \"last_login_time\": \"2019-04-05 22:16:30\",\n" +
      "            \"source\": \"\",\n" +
      "            \"sys_updated_on\": \"2019-04-05 21:54:45\",\n" +
      "            \"building\": \"\",\n" +
      "            \"web_service_access_only\": \"false\",\n" +
      "            \"notification\": \"2\",\n" +
      "            \"enable_multifactor_authn\": \"false\",\n" +
      "            \"sys_updated_by\": \"system\",\n" +
      "            \"sys_created_on\": \"2019-04-05 21:09:12\",\n" +
      "            \"sys_domain\": {\n" +
      "                \"link\": \"https://ven05127.service-now.com/api/now/table/sys_user_group/global\",\n" +
      "                \"value\": \"global\"\n" +
      "            },\n" +
      "            \"state\": \"\",\n" +
      "            \"vip\": \"false\",\n" +
      "            \"sys_created_by\": \"admin\",\n" +
      "            \"zip\": \"\",\n" +
      "            \"home_phone\": \"\",\n" +
      "            \"time_format\": \"\",\n" +
      "            \"last_login\": \"2019-04-05\",\n" +
      "            \"active\": \"true\",\n" +
      "            \"sys_domain_path\": \"/\",\n" +
      "            \"cost_center\": \"\",\n" +
      "            \"phone\": \"\",\n" +
      "            \"name\": \"survey user\",\n" +
      "            \"employee_number\": \"\",\n" +
      "            \"gender\": \"\",\n" +
      "            \"city\": \"\",\n" +
      "            \"failed_attempts\": \"0\",\n" +
      "            \"user_name\": \"survey.user\",\n" +
      "            \"title\": \"\",\n" +
      "            \"sys_class_name\": \"sys_user\",\n" +
      "            \"sys_id\": \"005d500b536073005e0addeeff7b12f4\",\n" +
      "            \"internal_integration_user\": \"false\",\n" +
      "            \"ldap_server\": \"\",\n" +
      "            \"mobile_phone\": \"\",\n" +
      "            \"street\": \"\",\n" +
      "            \"company\": \"\",\n" +
      "            \"department\": \"\",\n" +
      "            \"first_name\": \"survey\",\n" +
      "            \"email\": \"survey.user@email.com\",\n" +
      "            \"introduction\": \"\",\n" +
      "            \"preferred_language\": \"\",\n" +
      "            \"manager\": \"\",\n" +
      "            \"sys_mod_count\": \"1\",\n" +
      "            \"last_name\": \"user\",\n" +
      "            \"photo\": \"\",\n" +
      "            \"avatar\": \"\",\n" +
      "            \"middle_name\": \"\",\n" +
      "            \"sys_tags\": \"\",\n" +
      "            \"time_zone\": \"\",\n" +
      "            \"schedule\": \"\",\n" +
      "            \"date_format\": \"\",\n" +
      "            \"location\": \"\"]\n" +
      "}";
    RestAPIResponse restAPIResponse = new RestAPIResponse(httpStatus, headers, responseBody);
    Mockito.when(restApi.executeGet(Mockito.any())).thenReturn(restAPIResponse);
    Mockito.when(restApi.parseResponseToResultListOfMap(restAPIResponse.getResponseBody())).thenReturn(result);
    serviceNowMultiSourceConfig.validate(mockFailureCollector);
    Assert.assertEquals(1, mockFailureCollector.getValidationFailures().size());
    Assert.assertEquals("Invalid reference name 'Reference Name'.",
      mockFailureCollector.getValidationFailures().get(0).getMessage());

  }

  @Test
  public void testValidateWhenTableFieldNameIsEmpty() throws Exception {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig =
      ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("referenceName")
        .setRestApiEndpoint("https://example.com")
        .setUser("user")
        .setPassword("password")
        .setClientId("client_id")
        .setClientSecret("client_secret")
        .setTableNames("sys_user")
        .setValueType("Actual")
        .setStartDate("2021-12-30")
        .setEndDate("2021-12-31")
        .setTableNameField("")
        .buildMultiSource();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    Mockito.when(restApi.getAccessToken()).thenReturn("token");
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).
      withArguments(Mockito.any(ServiceNowBaseSourceConfig.class)).thenReturn(restApi);
    int httpStatus = HttpStatus.SC_OK;
    Map<String, String> headers = new HashMap<>();
    Map<String, Object> map = new HashMap<>();
    List<Map<String, Object>> result = new ArrayList<>();
    map.put("key", "value");
    result.add(map);
    String responseBody = "{\n" +
      "    \"result\": [{\n" +
      "            \"calendar_integration\": \"1\",\n" +
      "            \"country\": \"\",\n" +
      "            \"last_login_time\": \"2019-04-05 22:16:30\",\n" +
      "            \"source\": \"\",\n" +
      "            \"sys_updated_on\": \"2019-04-05 21:54:45\",\n" +
      "            \"building\": \"\",\n" +
      "            \"web_service_access_only\": \"false\",\n" +
      "            \"notification\": \"2\",\n" +
      "            \"enable_multifactor_authn\": \"false\",\n" +
      "            \"sys_updated_by\": \"system\",\n" +
      "            \"sys_created_on\": \"2019-04-05 21:09:12\",\n" +
      "            \"sys_domain\": {\n" +
      "                \"link\": \"https://ven05127.service-now.com/api/now/table/sys_user_group/global\",\n" +
      "                \"value\": \"global\"\n" +
      "            },\n" +
      "            \"state\": \"\",\n" +
      "            \"vip\": \"false\",\n" +
      "            \"sys_created_by\": \"admin\",\n" +
      "            \"zip\": \"\",\n" +
      "            \"home_phone\": \"\",\n" +
      "            \"time_format\": \"\",\n" +
      "            \"last_login\": \"2019-04-05\",\n" +
      "            \"active\": \"true\",\n" +
      "            \"sys_domain_path\": \"/\",\n" +
      "            \"cost_center\": \"\",\n" +
      "            \"phone\": \"\",\n" +
      "            \"name\": \"survey user\",\n" +
      "            \"employee_number\": \"\",\n" +
      "            \"gender\": \"\",\n" +
      "            \"city\": \"\",\n" +
      "            \"failed_attempts\": \"0\",\n" +
      "            \"user_name\": \"survey.user\",\n" +
      "            \"title\": \"\",\n" +
      "            \"sys_class_name\": \"sys_user\",\n" +
      "            \"sys_id\": \"005d500b536073005e0addeeff7b12f4\",\n" +
      "            \"internal_integration_user\": \"false\",\n" +
      "            \"ldap_server\": \"\",\n" +
      "            \"mobile_phone\": \"\",\n" +
      "            \"street\": \"\",\n" +
      "            \"company\": \"\",\n" +
      "            \"department\": \"\",\n" +
      "            \"first_name\": \"survey\",\n" +
      "            \"email\": \"survey.user@email.com\",\n" +
      "            \"introduction\": \"\",\n" +
      "            \"preferred_language\": \"\",\n" +
      "            \"manager\": \"\",\n" +
      "            \"sys_mod_count\": \"1\",\n" +
      "            \"last_name\": \"user\",\n" +
      "            \"photo\": \"\",\n" +
      "            \"avatar\": \"\",\n" +
      "            \"middle_name\": \"\",\n" +
      "            \"sys_tags\": \"\",\n" +
      "            \"time_zone\": \"\",\n" +
      "            \"schedule\": \"\",\n" +
      "            \"date_format\": \"\",\n" +
      "            \"location\": \"\"]\n" +
      "}";
    RestAPIResponse restAPIResponse = new RestAPIResponse(httpStatus, headers, responseBody);
    Mockito.when(restApi.executeGet(Mockito.any())).thenReturn(restAPIResponse);
    Mockito.when(restApi.parseResponseToResultListOfMap(restAPIResponse.getResponseBody())).thenReturn(result);
    serviceNowMultiSourceConfig.validate(mockFailureCollector);
    Assert.assertEquals(1, mockFailureCollector.getValidationFailures().size());
    Assert.assertEquals("Table name field must be specified.",
      mockFailureCollector.getValidationFailures().get(0).getMessage());
  }

  @Test
  public void testValidateTableNamesWhenTableNamesAreEmpty() throws NoSuchFieldException {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig =
      ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("referenceName")
        .setRestApiEndpoint("https://example.com")
        .setUser("user")
        .setPassword("password")
        .setClientId("client_id")
        .setClientSecret("client_secret")
        .setTableNames("")
        .setValueType("Actual")
        .setStartDate("2021-12-30")
        .setEndDate("2021-12-31")
        .setTableNameField("tablename")
        .buildMultiSource();

    serviceNowMultiSourceConfig.validateTableNames(mockFailureCollector);
    List<ValidationFailure> validationFailures = mockFailureCollector.getValidationFailures();
    Assert.assertEquals(1, validationFailures.size());
    Assert.assertEquals("Table names must be specified.", validationFailures.get(0).getMessage());
  }

}
