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
import io.cdap.cdap.api.macro.Macros;
import io.cdap.cdap.api.plugin.PluginProperties;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.connector.ServiceNowConnectorConfig;
import io.cdap.plugin.servicenow.util.ServiceNowColumn;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceQueryMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceNowTableAPIClientImpl.class, ServiceNowSourceConfig.class, ServiceNowRecordReader.class})
public class ServiceNowRecordReaderTest {

  private static final String CLIENT_ID = "client_id";
  private static final String CLIENT_SECRET = "client_secret";
  private static final String REST_API_ENDPOINT = "https://ven05127.service-now.com";
  private static final String USER = "user";
  private static final String PASSWORD = "password";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private ServiceNowSourceConfig serviceNowSourceConfig;
  private ServiceNowRecordReader serviceNowRecordReader;

  @Before
  public void initializeTests() {
    serviceNowSourceConfig = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setReferenceName("referenceName")
      .setRestApiEndpoint(REST_API_ENDPOINT)
      .setUser(USER)
      .setPassword(PASSWORD)
      .setClientId(CLIENT_ID)
      .setClientSecret(CLIENT_SECRET)
      .setTableName("sys_user")
      .setValueType("Actual")
      .setStartDate("2021-12-30")
      .setEndDate("2021-12-31")
      .setTableNameField("tablename")
      .build();

    serviceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
  }

  @Test
  public void testConstructor() throws IOException {
    serviceNowRecordReader.close();
    Assert.assertEquals(0, serviceNowRecordReader.pos);
  }

  @Test
  public void testConstructor2() throws IOException {
    ServiceNowSourceConfig serviceNowSourceConfig = new ServiceNowSourceConfig("referenceName",
                                                                               "Query Mode",
                                                                               "Application Name",
                                                                               "tablename",
                                                                               "tablename",
                                                                               "42",
                                                                               "Client Secret",
                                                                               "https://ven05127." +
                                                                                 "service-now.com/", "User",
                                                                               "password",
                                                                               "Actual", "2021-12-30"
                                                                               , "2021-12-31");

    serviceNowRecordReader.close();
    Assert.assertEquals(0, serviceNowRecordReader.pos);
    Assert.assertEquals("tablename", serviceNowSourceConfig.getTableNameField());
    Assert.assertEquals("tablename", serviceNowSourceConfig.getTableName());
    Assert.assertEquals("2021-12-30", serviceNowSourceConfig.getStartDate());
    Assert.assertEquals("referenceName", serviceNowSourceConfig.getReferenceName());
    Assert.assertEquals("2021-12-31", serviceNowSourceConfig.getEndDate());
    PluginProperties properties = serviceNowSourceConfig.getProperties();
    Assert.assertEquals("PluginProperties{properties={}, macros=Macros{lookupProperties=[], " +
                          "macroFunctions=[]}}", properties.toString());
    Assert.assertTrue(properties.getProperties().isEmpty());
    Macros macros = properties.getMacros();
    Assert.assertEquals("Macros{lookupProperties=[], macroFunctions=[]}", macros.toString());
    Assert.assertTrue(macros.getMacroFunctions().isEmpty());
    Assert.assertTrue(macros.getLookups().isEmpty());
  }

  @Test
  public void testConvertToValue() {
    Schema fieldSchema = Schema.of(Schema.LogicalType.TIMESTAMP_MILLIS);
    thrown.expect(IllegalStateException.class);
    serviceNowSourceConfig.getConnection().convertToValue("Field Name", fieldSchema, new HashMap<>(1));
  }

  @Test
  public void testConvertToStringValue() {
    Assert.assertEquals("Field Value", serviceNowSourceConfig.getConnection().convertToStringValue("Field Value"));
  }

  @Test
  public void testConvertToDoubleValue() {
    Assert.assertEquals(42.0, serviceNowSourceConfig.getConnection().convertToDoubleValue("42").doubleValue(),
                        0.0);
    Assert.assertEquals(42.0, serviceNowSourceConfig.getConnection().convertToDoubleValue(42).doubleValue(),
                        0.0);
    Assert.assertNull(serviceNowSourceConfig.getConnection().convertToDoubleValue(""));
  }

  @Test
  public void testConvertToIntegerValue() {
    Assert.assertEquals(42, serviceNowSourceConfig.getConnection().convertToIntegerValue("42").intValue());
    Assert.assertEquals(42, serviceNowSourceConfig.getConnection().convertToIntegerValue(42).intValue());
    Assert.assertNull(serviceNowSourceConfig.getConnection().convertToIntegerValue(""));
  }

  @Test
  public void testConvertToBooleanValue() {
    Assert.assertFalse(serviceNowSourceConfig.getConnection().convertToBooleanValue("Field Value"));
    Assert.assertFalse(serviceNowSourceConfig.getConnection().convertToBooleanValue(42));
    Assert.assertNull(serviceNowSourceConfig.getConnection().convertToBooleanValue(""));
  }

  @Test
  public void testFetchData() throws Exception {
    String tableName = serviceNowSourceConfig.getTableName();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);
    ServiceNowRecordReader serviceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("calendar_integration", "1");
    map.put("country", "India");
    map.put("sys_updated_on", "2019-04-05 21:54:45");
    map.put("web_service_access_only", "false");
    map.put("notification", "2");
    map.put("enable_multifactor_authn", "false");
    map.put("sys_updated_by", "system");
    map.put("sys_created_on", "2019-04-05 21:09:12");
    results.add(map);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    ServiceNowColumn column1 = new ServiceNowColumn("calendar_integration",  "integer");
    ServiceNowColumn column2 = new ServiceNowColumn("vip",  "boolean");
    List<ServiceNowColumn> columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    response.setColumns(columns);
    response.setResult(results);
    response.setTotalRecordCount(1);
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).withParameterTypes(ServiceNowConnectorConfig.class)
      .withArguments(Mockito.any(ServiceNowConnectorConfig.class)).thenReturn(restApi);
    Mockito.when(restApi.fetchTableRecordsRetryableMode(tableName, serviceNowSourceConfig.getValueType(),
                                                        serviceNowSourceConfig.getStartDate(), serviceNowSourceConfig.
                                                          getEndDate(), split.getOffset(),
                                                        ServiceNowConstants.PAGE_SIZE)).thenReturn(results);
    Mockito.when(restApi.fetchTableSchema(tableName, serviceNowSourceConfig.getValueType(), null, null,
                                          false)).thenReturn(response);

    serviceNowRecordReader.initialize(split, null);
    Assert.assertTrue(serviceNowRecordReader.nextKeyValue());
  }

  @Test
  public void testFetchDataReportingMode() throws Exception {
    serviceNowSourceConfig = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setReferenceName("referenceName")
      .setRestApiEndpoint(REST_API_ENDPOINT)
      .setUser(USER)
      .setPassword(PASSWORD)
      .setClientId(CLIENT_ID)
      .setClientSecret(CLIENT_SECRET)
      .setQueryMode(SourceQueryMode.REPORTING.getValue())
      .setApplicationName("Procurement")
      .setValueType("Actual")
      .setStartDate("2021-12-30")
      .setEndDate("2021-12-31")
      .setTableNameField("tablename")
      .build();

    serviceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    String tableName = serviceNowSourceConfig.getTableName();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);
    ServiceNowRecordReader serviceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("calendar_integration", "1");
    map.put("country", "India");
    map.put("sys_updated_on", "2019-04-05 21:54:45");
    map.put("web_service_access_only", "false");
    map.put("notification", "2");
    map.put("enable_multifactor_authn", "false");
    map.put("sys_updated_by", "system");
    map.put("sys_created_on", "2019-04-05 21:09:12");
    results.add(map);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    ServiceNowColumn column1 = new ServiceNowColumn("calendar_integration",  "integer");
    ServiceNowColumn column2 = new ServiceNowColumn("vip",  "boolean");
    List<ServiceNowColumn> columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    response.setColumns(columns);
    response.setResult(results);
    response.setTotalRecordCount(1);
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).withParameterTypes(ServiceNowConnectorConfig.class)
      .withArguments(Mockito.any(ServiceNowConnectorConfig.class)).thenReturn(restApi);
    Mockito.when(restApi.fetchTableRecordsRetryableMode(tableName, serviceNowSourceConfig.getValueType(),
                                                        serviceNowSourceConfig.getStartDate(),
                                                        serviceNowSourceConfig.getEndDate(), split.getOffset(),
                                                        ServiceNowConstants.PAGE_SIZE)).thenReturn(results);
    Mockito.when(restApi.fetchTableSchema(tableName, serviceNowSourceConfig.getValueType(), null, null,
                                          false)).thenReturn(response);

    serviceNowRecordReader.initialize(split, null);
    Assert.assertTrue(serviceNowRecordReader.nextKeyValue());
  }

  @Test
  public void testFetchDataOnInvalidTable() throws Exception {
    serviceNowSourceConfig = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setReferenceName("referenceName")
      .setRestApiEndpoint(REST_API_ENDPOINT)
      .setUser(USER)
      .setPassword(PASSWORD)
      .setClientId(CLIENT_ID)
      .setClientSecret(CLIENT_SECRET)
      .setTableName("")
      .setValueType("Actual")
      .setStartDate("2021-01-01")
      .setEndDate("2022-02-18")
      .setTableNameField("tablename")
      .build();

    String tableName = serviceNowSourceConfig.getTableName();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);
    ServiceNowRecordReader serviceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    List<Map<String, Object>> results = new ArrayList<>();
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).withParameterTypes(ServiceNowConnectorConfig.class)
      .withArguments(Mockito.any(ServiceNowConnectorConfig.class)).thenReturn(restApi);
    Mockito.when(restApi.fetchTableRecords(tableName, serviceNowSourceConfig.getValueType(),
                                           serviceNowSourceConfig.getStartDate(), serviceNowSourceConfig.getEndDate(),
                                           split.getOffset(),
                                           ServiceNowConstants.PAGE_SIZE)).thenReturn(results);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setResult(results);
    serviceNowRecordReader.initialize(split, null);
    Assert.assertFalse(serviceNowRecordReader.nextKeyValue());
  }
}
