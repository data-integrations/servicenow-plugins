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
import io.cdap.cdap.api.plugin.PluginProperties;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceNowMultiRecordReaderTest {

  private static final String CLIENT_ID = System.getProperty("servicenow.test.clientId");
  private static final String CLIENT_SECRET = System.getProperty("servicenow.test.clientSecret");
  private static final String REST_API_ENDPOINT = System.getProperty("servicenow.test.restApiEndpoint");
  private static final String USER = System.getProperty("servicenow.test.user");
  private static final String PASSWORD = System.getProperty("servicenow.test.password");
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowMultiInputFormatTest.class);

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private ServiceNowMultiSourceConfig serviceNowMultiSourceConfig;
  private ServiceNowMultiRecordReader serviceNowMultiRecordReader;

  @Before
  public void initializeTests() {
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

      serviceNowMultiRecordReader = new ServiceNowMultiRecordReader(serviceNowMultiSourceConfig);

    } catch (AssumptionViolatedException e) {
      LOG.warn("Service Now Batch Multi Source tests are skipped. ");
      throw e;
    }
  }

  @Test
  public void testConstructor() throws IOException {
    Assert.assertEquals("tablename", serviceNowMultiSourceConfig.tableNameField);
    Assert.assertEquals("pipeline.user.1", serviceNowMultiSourceConfig.getUser());
    Assert.assertEquals("sys_user", serviceNowMultiSourceConfig.getTableNames());
    Assert.assertEquals("tablename", serviceNowMultiSourceConfig.getTableNameField());
    Assert.assertEquals("2021-01-01", serviceNowMultiSourceConfig.getStartDate());
    Assert.assertEquals("https://ven05127.service-now.com", serviceNowMultiSourceConfig.getRestApiEndpoint());
    Assert.assertEquals("referenceName", serviceNowMultiSourceConfig.getReferenceName());
    Assert.assertEquals("2022-02-18", serviceNowMultiSourceConfig.getEndDate());
    PluginProperties properties = serviceNowMultiSourceConfig.getProperties();
    Assert.assertTrue(properties.getProperties().isEmpty());
    serviceNowMultiRecordReader.close();
    Assert.assertEquals(0, serviceNowMultiRecordReader.pos);
  }

  @Test
  public void testConvertToValue() {
    Schema fieldSchema = Schema.of(Schema.LogicalType.TIMESTAMP_MILLIS);
    thrown.expect(IllegalStateException.class);
    serviceNowMultiRecordReader.convertToValue("Field Name", fieldSchema, new HashMap<>(1));
  }

  @Test
  public void testConvertToValue2() {
    // TODO: This test is incomplete.

    Schema fieldSchema = Schema.of(Schema.Type.BOOLEAN);
    serviceNowMultiRecordReader.convertToValue("Field Name", fieldSchema, new HashMap<>(1));
  }

  @Test
  public void testConvertToStringValue() {
    Assert.assertEquals("Field Value", serviceNowMultiRecordReader.convertToStringValue("Field Value"));
  }

  @Test
  public void testConvertToDoubleValue() {
    Assert.assertEquals(42.0, serviceNowMultiRecordReader.convertToDoubleValue("42").doubleValue(), 0.0);
    Assert.assertEquals(42.0, serviceNowMultiRecordReader.convertToDoubleValue(42).doubleValue(), 0.0);
    Assert.assertNull(serviceNowMultiRecordReader.convertToDoubleValue(""));
  }

  @Test
  public void testConvertToIntegerValue() {
    Assert.assertEquals(42, serviceNowMultiRecordReader.convertToIntegerValue("42").intValue());
    Assert.assertEquals(42, serviceNowMultiRecordReader.convertToIntegerValue(42).intValue());
    Assert.assertNull(serviceNowMultiRecordReader.convertToIntegerValue(""));
  }

  @Test
  public void testConvertToBooleanValue() {
    Assert.assertFalse(serviceNowMultiRecordReader.convertToBooleanValue("Field Value"));
    Assert.assertFalse(serviceNowMultiRecordReader.convertToBooleanValue(42));
    Assert.assertNull(serviceNowMultiRecordReader.convertToBooleanValue(""));
  }

  @Test
  public void testFetchData() throws IOException {
    String tableName = serviceNowMultiSourceConfig.getTableNames();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);
    ServiceNowMultiRecordReader serviceNowMultiRecordReader =
      new ServiceNowMultiRecordReader(serviceNowMultiSourceConfig);
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
    Mockito.when(restApi.fetchTableRecords(tableName, serviceNowMultiSourceConfig.getStartDate(),
                                           serviceNowMultiSourceConfig.getEndDate(),
                                           split.getOffset(), ServiceNowConstants.PAGE_SIZE)).thenReturn(results);

    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setResult(results);
    serviceNowMultiRecordReader.initialize(split, null);
    Assert.assertTrue(serviceNowMultiRecordReader.nextKeyValue());
  }

  @Test
  public void testFetchDataOnInvalidTable() throws IOException {
    serviceNowMultiSourceConfig = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setReferenceName("referenceName")
      .setRestApiEndpoint(REST_API_ENDPOINT)
      .setUser(USER)
      .setPassword(PASSWORD)
      .setClientId(CLIENT_ID)
      .setClientSecret(CLIENT_SECRET)
      .setTableNames("")
      .setValueType("Actual")
      .setStartDate("2021-01-01")
      .setEndDate("2022-02-18")
      .setTableNameField("tablename")
      .buildMultiSource();

    String tableName = serviceNowMultiSourceConfig.getTableNames();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);
    ServiceNowMultiRecordReader serviceNowMultiRecordReader =
      new ServiceNowMultiRecordReader(serviceNowMultiSourceConfig);
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
    Mockito.when(restApi.fetchTableRecords(tableName, serviceNowMultiSourceConfig.getStartDate(),
                                           serviceNowMultiSourceConfig.getEndDate(),
                                           split.getOffset(), ServiceNowConstants.PAGE_SIZE)).thenReturn(results);

    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setResult(results);
    serviceNowMultiRecordReader.initialize(split, null);
    Assert.assertFalse(serviceNowMultiRecordReader.nextKeyValue());
  }

}
