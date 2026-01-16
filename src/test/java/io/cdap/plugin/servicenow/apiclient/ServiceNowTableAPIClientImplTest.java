package io.cdap.plugin.servicenow.apiclient;

import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.servicenow.connector.ServiceNowConnectorConfig;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.util.SchemaType;
import io.cdap.plugin.servicenow.util.SourceValueType;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ServiceNowTableAPIClientImplTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();
  @Test
  public void testFetchTableRecordsRetryableMode_RetriesAndSucceeds() throws ServiceNowAPIException {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    List<Map<String, String>> mockResults = new ArrayList<>();
    mockResults.add(new HashMap<String, String>() {{
      put("keyTest", "valueTest");
    }});
    HttpResponse mockResponse = Mockito.mock(HttpResponse.class);
    RestAPIResponse mockApiResponse = new RestAPIResponse(Collections.emptyMap(), null, null);
    Mockito.when(mockResponse.getStatusLine()).thenReturn(Mockito.mock(StatusLine.class));
    Mockito.when(mockResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_REQUEST_TIMEOUT);
    Mockito.doThrow(new ServiceNowAPIException("Retryable Error", mockResponse))
            .doReturn(mockApiResponse)
                .when(implSpy).fetchTableRecords(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyInt(),
            Mockito.anyInt());
    RestAPIResponse restAPIResponse =
        implSpy.fetchTableRecordsRetryableMode(
            "test", SourceValueType.SHOW_DISPLAY_VALUE, "", "", 0, 0);
    Mockito.verify(implSpy, Mockito.times(2)).fetchTableRecords(
        Mockito.anyString(),
        Mockito.any(),
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyInt(),
        Mockito.anyInt());
    Assert.assertEquals(restAPIResponse, mockApiResponse);
  }

  @Test
  public void testFetchTableRecordsRetryableMode_nonRetryable()
      throws ServiceNowAPIException {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    HttpResponse mockResponse = Mockito.mock(HttpResponse.class);
    RestAPIResponse mockAPIResponse = new RestAPIResponse(Collections.emptyMap(), null, null);
    Mockito.when(mockResponse.getStatusLine()).thenReturn(Mockito.mock(StatusLine.class));
    Mockito.when(mockResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    Mockito.doThrow(
        new ServiceNowAPIException("Non-retryable Error", mockResponse))
        .doReturn(mockAPIResponse)
        .when(implSpy).fetchTableRecords(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyInt(),
            Mockito.anyInt());
    exceptionRule.expect(ServiceNowAPIException.class);
    exceptionRule.expectMessage("Data Recovery failed for batch 0 to 0.");
    implSpy.fetchTableRecordsRetryableMode(
        "test", SourceValueType.SHOW_DISPLAY_VALUE, "", "", 0, 0);
  }

  @Test
  public void testFetchTableSchema_ActualValueType() throws Exception {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    String jsonResponse = "{\n" +
      "  \"result\": [\n" +
      "    {\n" +
      "      \"internalType\": \"boolean\",\n" +
      "      \"label\": \"Active\",\n" +
      "      \"exampleValue\": \"\",\n" +
      "      \"name\": \"active\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"internalType\": \"string\",\n" +
      "      \"label\": \"Username\",\n" +
      "      \"exampleValue\": \"\",\n" +
      "      \"name\": \"user_name\"\n" +
      "    }\n" +
      "  ]\n" +
      "}";
    byte[] body = jsonResponse.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(body);
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), inputStream, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("sys_user", "dummy-access-token",
                                             SourceValueType.SHOW_ACTUAL_VALUE, SchemaType.SCHEMA_API_BASED);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.BOOLEAN,
      schema.getField("active").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.STRING,
      schema.getField("user_name").getSchema().getUnionSchemas().get(0).getType());
  }

  @Test
  public void testFetchTableSchema_GlideTimeFieldWithActualValueType() throws Exception {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);

    String jsonResponse = "{\n" +
      "  \"result\": {\n" +
      "    \"columns\": {\n" +
      "      \"u_start_time\": {\n" +
      "        \"label\": \"Start Time\",\n" +
      "        \"name\": \"u_start_time\",\n" +
      "        \"type\": \"string\",\n" +
      "        \"internal_type\": \"glide_time\"\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";

    byte[] body = jsonResponse.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(body);
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), inputStream, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());

    Schema schema = implSpy.fetchTableSchema("u_custom_table",
      "dummy-access-token", SourceValueType.SHOW_ACTUAL_VALUE, SchemaType.METADATA_API_BASED);

    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(1, schema.getFields().size());

    Schema.Field field = schema.getField("u_start_time");
    Assert.assertNotNull(field);

    Schema fieldSchema = field.getSchema().getUnionSchemas().get(0);
    Assert.assertEquals(Schema.LogicalType.DATETIME, fieldSchema.getLogicalType());
  }

  @Test
  public void testFetchTableSchema_DisplayValueType() throws Exception {

    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    String jsonResponse = "{\n" +
      "  \"result\": [\n" +
      "    {\n" +
      "      \"internalType\": \"boolean\",\n" +
      "      \"label\": \"Active\",\n" +
      "      \"exampleValue\": \"\",\n" +
      "      \"name\": \"active\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"internalType\": \"string\",\n" +
      "      \"label\": \"Username\",\n" +
      "      \"exampleValue\": \"\",\n" +
      "      \"name\": \"user_name\"\n" +
      "    }\n" +
      "  ]\n" +
      "}";
    byte[] body = jsonResponse.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(body);
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), inputStream, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("sys_user", "dummy-access-token",
      SourceValueType.SHOW_DISPLAY_VALUE, SchemaType.SCHEMA_API_BASED);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.BOOLEAN,
      schema.getField("active").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.STRING,
      schema.getField("user_name").getSchema().getUnionSchemas().get(0).getType());
  }

  @Test
  public void testFetchTableSchema_StcFieldsWithDisplayValueType_ParseAsString() throws Exception {

    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    String jsonResponse = "{\n" +
      "  \"result\": {\n" +
      "    \"columns\": {\n" +
      "      \"calendar_stc\": {\n" +
      "        \"label\": \"Resolve time\",\n" +
      "        \"type\": \"integer\",\n" +
      "        \"name\": \"calendar_stc\",\n" +
      "        \"internal_type\": \"integer\"\n" +
      "      },\n" +
      "      \"business_stc\": {\n" +
      "        \"label\": \"Business resolve time\",\n" +
      "        \"type\": \"integer\",\n" +
      "        \"name\": \"business_stc\",\n" +
      "        \"internal_type\": \"integer\"\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    byte[] body = jsonResponse.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(body);
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), inputStream, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("incident", "dummy-access-token",
                                             SourceValueType.SHOW_DISPLAY_VALUE, SchemaType.METADATA_API_BASED);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("business_stc").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("calendar_stc").getSchema().getUnionSchemas().get(0).getType());
  }
}
