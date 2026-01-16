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

import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.format.UnexpectedFormatException;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.macro.Macros;
import io.cdap.cdap.api.plugin.PluginProperties;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.connector.ServiceNowConnectorConfig;
import io.cdap.plugin.servicenow.connector.ServiceNowRecordConverter;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.util.ServiceNowColumn;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceQueryMode;
import io.cdap.plugin.servicenow.util.SourceValueType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
      .setPageSize(10)
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
                                                                               "Actual",
                                                                               "2021-12-30",
                                                                               "2021-12-31", 10);

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

    Schema fieldSchema = Schema.recordOf("record", Schema.Field.of("TimeField",
                                                                   Schema.of(Schema.LogicalType.TIMESTAMP_MILLIS)));
    StructuredRecord.Builder recordBuilder = StructuredRecord.builder(fieldSchema);
    Map<String, String> map = new HashMap<>();
    map.put("TimeField", "value");
    thrown.expect(IllegalStateException.class);
    ServiceNowRecordConverter.convertToValue("TimeField", fieldSchema, map, recordBuilder);
  }

  @Test
  public void testConvertToDateTimeValue() {
    Schema recordSchema = Schema.recordOf(
        "record",
        Schema.Field.of("DateTimeField", Schema.of(Schema.LogicalType.DATETIME))
    );
    Schema fieldSchema = recordSchema.getField("DateTimeField").getSchema();

    List<String> dateTimeValues = Arrays.asList(
        "2025-05-14 13:45:30",
        "2025-05-14 13:45",
        "14-04-2025 13.45.32",
        "14-04-25 13.45.34",
        "14/05/2025 13:45:30",
        "14.05.2025 01.45.30 PM",
        "14.05.2025 01:45:30 PM",
        "28/10/2017 01:00:01"
    );

    for (String value : dateTimeValues) {
      Map<String, String> inputMap = new HashMap<>();
      inputMap.put("DateTimeField", value);

      StructuredRecord.Builder recordBuilder = StructuredRecord.builder(recordSchema);
      try {
        ServiceNowRecordConverter.convertToValue("DateTimeField", fieldSchema, inputMap, recordBuilder);
        StructuredRecord record = recordBuilder.build();
        Assert.assertNotNull("Parsed datetime should not be null for input: " + value,
            record.get("DateTimeField"));
      } catch (UnexpectedFormatException e) {
        Assert.fail("Failed to parse valid datetime format: " + value + " - " + e.getMessage());
      }
    }
  }

  @Test
  public void testConvertToDateValue() {
    Schema recordSchema = Schema.recordOf(
        "record",
        Schema.Field.of("DateField", Schema.of(Schema.LogicalType.DATE))
    );
    Schema fieldSchema = recordSchema.getField("DateField").getSchema();

    List<String> dateValues = Arrays.asList(
        "2025-05-14",
        "14/05/2025",
        "14.05.2025",
        "02/10/2017"
    );

    for (String value : dateValues) {
      Map<String, String> inputMap = new HashMap<>();
      inputMap.put("DateField", value);

      StructuredRecord.Builder recordBuilder = StructuredRecord.builder(recordSchema);
      try {
        ServiceNowRecordConverter.convertToValue("DateField", fieldSchema, inputMap, recordBuilder);
        StructuredRecord record = recordBuilder.build();
        Assert.assertNotNull("Parsed date should not be null for input: " + value,
            record.get("DateField"));
      } catch (UnexpectedFormatException e) {
        Assert.fail("Failed to parse valid date format: " + value + " - " + e.getMessage());
      }
    }
  }

  @Test
  public void testConvertToTimeValue() {
    Schema recordSchema = Schema.recordOf(
        "record",
        Schema.Field.of("TimeField", Schema.of(Schema.LogicalType.TIME_MICROS))
    );
    Schema fieldSchema = recordSchema.getField("TimeField").getSchema();

    List<String> timeValues = Arrays.asList(
        "13:45:30",
        "13:45"
    );

    for (String value : timeValues) {
      Map<String, String> inputMap = new HashMap<>();
      inputMap.put("TimeField", value);

      StructuredRecord.Builder recordBuilder = StructuredRecord.builder(recordSchema);
      try {
        ServiceNowRecordConverter.convertToValue("TimeField", fieldSchema, inputMap, recordBuilder);
        StructuredRecord record = recordBuilder.build();
        Assert.assertNotNull("Parsed date should not be null for input: " + value,
            record.get("TimeField"));
      } catch (UnexpectedFormatException e) {
        Assert.fail("Failed to parse valid date format: " + value + " - " + e.getMessage());
      }
    }
  }

  @Test
  public void testConvertToDoubleValue() throws ParseException {
    Assert.assertEquals(42.0, ServiceNowRecordConverter.convertToDoubleValue("42"),
                        0.0);
  }

  @Test
  public void testConvertToIntegerValue() throws ParseException {
    Assert.assertEquals(42, ServiceNowRecordConverter.convertToIntegerValue("42").intValue());
  }

  @Test
  public void testConvertToBooleanValue() {
    Assert.assertTrue(ServiceNowRecordConverter.convertToBooleanValue("true"));
  }

  @Test(expected = UnexpectedFormatException.class)
  public void testConvertToBooleanValueForInvalidFieldValue() {
    Assert.assertTrue(ServiceNowRecordConverter.convertToBooleanValue("1"));
  }

  @Test
  public void testFetchData() throws Exception {
    String tableName = serviceNowSourceConfig.getTableName();
    SourceValueType valueType = serviceNowSourceConfig.getValueType();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);
    ServiceNowRecordReader serviceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    RestAPIResponse mockResponse = Mockito.mock(RestAPIResponse.class);
    String responseBody = "{\n" +
      "  \"result\": [\n" +
      "    {\n" +
      "      \"bill_to\": \"\",\n" +
      "      \"init_request\": \"\",\n" +
      "      \"short_description\": \"\",\n" +
      "      \"total_cost\": \"0\",\n" +
      "      \"due_by\": \"\",\n" +
      "      \"description\": \"\",\n" +
      "      \"requested_for\": \"\",\n" +
      "      \"sys_updated_on\": \"2022-06-16 18:56:23\",\n" +
      "      \"budget_number\": \"\",\n" +
      "      \"number\": \"RCS397871\",\n" +
      "      \"sys_id\": \"00000b7287405910827733373cbb35d5\",\n" +
      "      \"sys_updated_by\": \"pipeline.user.1\",\n" +
      "      \"shipping\": \"\",\n" +
      "      \"terms\": \"\",\n" +
      "      \"sys_created_on\": \"2022-06-16 18:56:23\",\n" +
      "      \"vendor\": \"\",\n" +
      "      \"sys_domain\": \"global\",\n" +
      "      \"department\": \"\",\n" +
      "      \"sys_created_by\": \"pipeline.user.1\",\n" +
      "      \"assigned_to\": \"\",\n" +
      "      \"ordered\": \"\",\n" +
      "      \"po_date\": \"2022-06-16 18:56:23\",\n" +
      "      \"vendor_contract\": \"\",\n" +
      "      \"contract\": \"\",\n" +
      "      \"expected_delivery\": \"\",\n" +
      "      \"sys_mod_count\": \"0\",\n" +
      "      \"received\": \"2158-05-10 17:14:20\",\n" +
      "      \"asset_operation\": \"\",\n" +
      "      \"sys_tags\": \"\",\n" +
      "      \"requested\": \"2022-06-16 18:56:23\",\n" +
      "      \"requested_by\": \"\",\n" +
      "      \"ship_rate\": \"0\",\n" +
      "      \"location\": \"\",\n" +
      "      \"vendor_account\": \"\",\n" +
      "      \"ship_to\": \"\",\n" +
      "      \"status\": \"requested\"\n" +
      "    }\n" +
      "  ]\n" +
      "}";
    byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(body);
    RestAPIResponse restAPIResponse = new RestAPIResponse(Collections.emptyMap(), inputStream, null);
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).withAnyArguments().thenReturn(restApi);
    Mockito.when(restApi.fetchTableRecordsRetryableMode(tableName, serviceNowSourceConfig.getValueType(),
      serviceNowSourceConfig.getStartDate(), serviceNowSourceConfig.getEndDate(), split.getOffset(),
        serviceNowSourceConfig.getPageSize())).thenReturn(restAPIResponse);
    Mockito.when(restApi.fetchTableSchema(tableName, valueType))
      .thenReturn(Schema.recordOf(Schema.Field.of("calendar_integration", Schema.of(Schema.Type.STRING))));
    serviceNowRecordReader.initialize(split);
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
    String responseBody = "{\n" +
      "  \"result\": [\n" +
      "    {\n" +
      "      \"bill_to\": \"\",\n" +
      "      \"init_request\": \"\",\n" +
      "      \"short_description\": \"\",\n" +
      "      \"total_cost\": \"0\",\n" +
      "      \"due_by\": \"\",\n" +
      "      \"description\": \"\",\n" +
      "      \"requested_for\": \"\",\n" +
      "      \"sys_updated_on\": \"2022-06-16 18:56:23\",\n" +
      "      \"budget_number\": \"\",\n" +
      "      \"number\": \"RCS397871\",\n" +
      "      \"sys_id\": \"00000b7287405910827733373cbb35d5\",\n" +
      "      \"sys_updated_by\": \"pipeline.user.1\",\n" +
      "      \"shipping\": \"\",\n" +
      "      \"terms\": \"\",\n" +
      "      \"sys_created_on\": \"2022-06-16 18:56:23\",\n" +
      "      \"vendor\": \"\",\n" +
      "      \"sys_domain\": \"global\",\n" +
      "      \"department\": \"\",\n" +
      "      \"sys_created_by\": \"pipeline.user.1\",\n" +
      "      \"assigned_to\": \"\",\n" +
      "      \"ordered\": \"\",\n" +
      "      \"po_date\": \"2022-06-16 18:56:23\",\n" +
      "      \"vendor_contract\": \"\",\n" +
      "      \"contract\": \"\",\n" +
      "      \"expected_delivery\": \"\",\n" +
      "      \"sys_mod_count\": \"0\",\n" +
      "      \"received\": \"2158-05-10 17:14:20\",\n" +
      "      \"asset_operation\": \"\",\n" +
      "      \"sys_tags\": \"\",\n" +
      "      \"requested\": \"2022-06-16 18:56:23\",\n" +
      "      \"requested_by\": \"\",\n" +
      "      \"ship_rate\": \"0\",\n" +
      "      \"location\": \"\",\n" +
      "      \"vendor_account\": \"\",\n" +
      "      \"ship_to\": \"\",\n" +
      "      \"status\": \"requested\"\n" +
      "    }\n" +
      "  ]\n" +
      "}";
    byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(body);
    RestAPIResponse restAPIResponse = new RestAPIResponse(Collections.emptyMap(), inputStream, null);
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).withAnyArguments().thenReturn(restApi);
    Mockito.when(restApi.fetchTableRecordsRetryableMode(tableName, serviceNowSourceConfig.getValueType(),
      serviceNowSourceConfig.getStartDate(), serviceNowSourceConfig.getEndDate(), split.getOffset(),
        serviceNowSourceConfig.getPageSize())).thenReturn(restAPIResponse);
    Mockito.when(restApi.fetchTableSchema(tableName, serviceNowSourceConfig.getValueType()))
      .thenReturn(Schema.recordOf(Schema.Field.of("calendar_integration", Schema.of(Schema.Type.STRING))));
    serviceNowRecordReader.initialize(split);
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
      .setTableName("abc")
      .setValueType("Actual")
      .setStartDate("2021-01-01")
      .setEndDate("2022-02-18")
      .setTableNameField("tablename")
      .build();

    String tableName = serviceNowSourceConfig.getTableName();
    ServiceNowTableAPIClientImpl restApi = Mockito.mock(ServiceNowTableAPIClientImpl.class);
    ServiceNowInputSplit split = new ServiceNowInputSplit(tableName, 1);
    ServiceNowRecordReader serviceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    List<Map<String, String>> results = new ArrayList<>();
    String responseBody = "{\n    " +
      "\"error\": " +
      "{\n        " +
      "\"message\": \"Invalid table abc\",\n" +
      "        \"detail\": null\n    " +
      "},\n    " +
      "\"status\": \"failure\"\n" +
      "}";
    byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(body);
    RestAPIResponse restAPIResponse = new RestAPIResponse(Collections.emptyMap(), inputStream, null);
    PowerMockito.whenNew(ServiceNowTableAPIClientImpl.class).withAnyArguments().thenReturn(restApi);
    Mockito.when(restApi.fetchTableRecordsRetryableMode(tableName, serviceNowSourceConfig.getValueType(),
                                           serviceNowSourceConfig.getStartDate(), serviceNowSourceConfig.getEndDate(),
                                           split.getOffset(),
                                           serviceNowSourceConfig.getPageSize())).thenReturn(restAPIResponse);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setResult(results);
    Mockito.when(restApi.fetchTableSchema(tableName, serviceNowSourceConfig.getValueType()))
      .thenReturn(Schema.recordOf(Schema.Field.of("calendar_integration", Schema.of(Schema.Type.STRING))));
    serviceNowRecordReader.initialize(split);
    Assert.assertFalse(serviceNowRecordReader.nextKeyValue());
  }
}
