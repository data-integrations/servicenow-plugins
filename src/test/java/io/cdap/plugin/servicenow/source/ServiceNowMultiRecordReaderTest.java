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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceNowMultiRecordReaderTest {

  private static final String CLIENT_ID = "clientId";
  private static final String CLIENT_SECRET = "clientSecret";
  private static final String REST_API_ENDPOINT = "https://ven05127.service-now.com";
  private static final String USER = "user";
  private static final String PASSWORD = "password";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private ServiceNowMultiSourceConfig serviceNowMultiSourceConfig;
  private ServiceNowMultiRecordReader serviceNowMultiRecordReader;

  @Before
  public void initializeTests() {
      serviceNowMultiSourceConfig = Mockito.spy(new ServiceNowSourceConfigHelper.ConfigBuilder()
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
              .buildMultiSource());

      serviceNowMultiRecordReader = Mockito.spy(new ServiceNowMultiRecordReader(serviceNowMultiSourceConfig));
  }

  @Test
  public void testConstructor() throws IOException {
    Assert.assertEquals("tablename", serviceNowMultiSourceConfig.tableNameField);
    Assert.assertEquals("user", serviceNowMultiSourceConfig.getUser());
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

  @Test(expected = IllegalStateException.class)
  public void testConvertToValueInvalidFieldType() {
    Schema fieldSchema = Schema.of(Schema.LogicalType.TIMESTAMP_MILLIS);
    serviceNowMultiRecordReader.convertToValue("Field Name", fieldSchema, new HashMap<>(1));
  }

  @Test
  public void testConvertToValueInvalidRecord() {
    Schema fieldSchema = Schema.of(Schema.Type.BOOLEAN);
    Assert.assertEquals(Boolean.FALSE, serviceNowMultiRecordReader.convertToValue("Field Name", fieldSchema,
      new HashMap<>(1)));
  }

  @Test
  public void testConvertToStringValue() {
    Assert.assertEquals("Field Value", serviceNowMultiRecordReader.
      convertToStringValue("Field Value"));
  }

  @Test
  public void testConvertToDoubleValue() {
    Assert.assertEquals(42.0, serviceNowMultiRecordReader.
      convertToDoubleValue("42").doubleValue(), 0.0);
    Assert.assertEquals(42.0, serviceNowMultiRecordReader.
      convertToDoubleValue(42).doubleValue(), 0.0);
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
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);

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
    response.setResult(results);
    serviceNowMultiRecordReader.initialize(split, null);
    Mockito.doNothing().when(serviceNowMultiRecordReader).fetchData();
    Collections.singletonList(new Object());
    serviceNowMultiRecordReader.iterator = Collections.singletonList(Collections.singletonMap("key", new Object())).
      iterator();
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
    restApi.fetchTableRecords(tableName, serviceNowMultiSourceConfig.getStartDate(),
                                           serviceNowMultiSourceConfig.getEndDate(),
                                           split.getOffset(), ServiceNowConstants.PAGE_SIZE);

    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setResult(results);
    serviceNowMultiRecordReader.initialize(split, null);
    Assert.assertFalse(serviceNowMultiRecordReader.nextKeyValue());
  }
}
