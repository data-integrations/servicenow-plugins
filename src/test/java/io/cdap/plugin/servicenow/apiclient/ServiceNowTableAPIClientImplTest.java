package io.cdap.plugin.servicenow.apiclient;

import com.github.rholder.retry.Retryer;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.servicenow.connector.ServiceNowConnectorConfig;
import io.cdap.plugin.servicenow.restapi.RestAPIRequest;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.util.SchemaType;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceValueType;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ServiceNowTableAPIClientImplTest {

  private static RestAPIResponse buildErrorResponse(int statusCode, String message) {
    HttpResponse mockResponse = Mockito.mock(HttpResponse.class);
    StatusLine statusLine = Mockito.mock(StatusLine.class);
    Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
    Mockito.when(mockResponse.getStatusLine()).thenReturn(statusLine);
    return new RestAPIResponse(
        Collections.emptyMap(), null, new ServiceNowAPIException(message, mockResponse));
  }

  private static class FakeTableAPIClient extends ServiceNowTableAPIClientImpl {
    private final Queue<RestAPIResponse> responses;
    private final RestAPIResponse fallbackResponse;
    private int fetchTableRecordsCalls;
    private int executeGetCalls;

    FakeTableAPIClient(
        ServiceNowConnectorConfig conf,
        RestAPIResponse fallbackResponse,
        RestAPIResponse... scriptedResponses) {
      super(conf, true);
      this.responses = new ArrayDeque<>(Arrays.asList(scriptedResponses));
      this.fallbackResponse = fallbackResponse;
    }

    @Override
    public String getAccessToken() {
      return "dummy-access-token";
    }

    @Override
    public List<Map<String, String>> fetchTableRecords(
        String tableName, SourceValueType valueType, String filterQuery, int offset, int limit)
        throws ServiceNowAPIException {
      fetchTableRecordsCalls++;
      return super.fetchTableRecords(tableName, valueType, filterQuery, offset, limit);
    }

    @Override
    public RestAPIResponse executeGet(RestAPIRequest request) {
      executeGetCalls++;
      return responses.isEmpty() ? fallbackResponse : responses.poll();
    }

    @Override
    protected Retryer<RestAPIResponse> getRetryer() {
      return getRetryer(attempt -> { });
    }
  }

  @Test
  public void testFetchTableRecordsRetryableMode_RetriesAndSucceeds() throws ServiceNowAPIException {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    Mockito.when(mockConfig.getRestApiEndpoint()).thenReturn("https://example.service-now.com");
    RestAPIResponse timeoutResponse =
        buildErrorResponse(HttpStatus.SC_REQUEST_TIMEOUT, "Retryable Error");
    RestAPIResponse successResponse =
        new RestAPIResponse(
            Collections.emptyMap(), "{\"result\":[{\"keyTest\":\"valueTest\"}]}", null);
    FakeTableAPIClient client =
        new FakeTableAPIClient(mockConfig, successResponse, timeoutResponse, successResponse);

    List<Map<String, String>> receivedResults =
        client.fetchTableRecordsRetryableMode(
            "test", SourceValueType.SHOW_DISPLAY_VALUE, "", 0, 10);

    Assert.assertEquals(1, client.fetchTableRecordsCalls);
    Assert.assertEquals(2, client.executeGetCalls);
    Assert.assertEquals(
        Collections.singletonList(Collections.singletonMap("keyTest", "valueTest")),
        receivedResults);
  }

  @Test
  public void testFetchTableRecordsRetryableMode_RetriesExhausted_DoesNotMultiplyRetries() {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    Mockito.when(mockConfig.getRestApiEndpoint()).thenReturn("https://example.service-now.com");
    RestAPIResponse rateLimitResponse = buildErrorResponse(429, "Too Many Requests");
    FakeTableAPIClient client = new FakeTableAPIClient(mockConfig, rateLimitResponse);

    ServiceNowAPIException thrownException = null;
    try {
      client.fetchTableRecordsRetryableMode(
          "test", SourceValueType.SHOW_DISPLAY_VALUE, "", 0, 100);
    } catch (ServiceNowAPIException e) {
      thrownException = e;
    }

    Assert.assertNotNull(thrownException);
    Assert.assertEquals("Data Recovery failed for batch 0 to 100.", thrownException.getMessage());
    Assert.assertFalse(thrownException.isErrorRetryable());
    Assert.assertEquals(1, client.fetchTableRecordsCalls);
    Assert.assertEquals(ServiceNowConstants.MAX_NUMBER_OF_RETRY_ATTEMPTS, client.executeGetCalls);
  }

  @Test
  public void testFetchTableRecordsRetryableMode_nonRetryable() throws ServiceNowAPIException {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    Mockito.when(mockConfig.getRestApiEndpoint()).thenReturn("https://example.service-now.com");
    RestAPIResponse serverErrorResponse =
        buildErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Non-retryable Error");
    FakeTableAPIClient client = new FakeTableAPIClient(mockConfig, serverErrorResponse);

    ServiceNowAPIException thrownException = null;
    try {
      client.fetchTableRecordsRetryableMode(
          "test", SourceValueType.SHOW_DISPLAY_VALUE, "", 0, 0);
    } catch (ServiceNowAPIException e) {
      thrownException = e;
    }

    Assert.assertNotNull(thrownException);
    Assert.assertEquals("Data Recovery failed for batch 0 to 0.", thrownException.getMessage());
    Assert.assertFalse(thrownException.isErrorRetryable());
    Assert.assertEquals(1, client.fetchTableRecordsCalls);
    Assert.assertEquals(1, client.executeGetCalls);
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
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("sys_user", "dummy-access-token",
      SourceValueType.SHOW_ACTUAL_VALUE, SchemaType.SCHEMA_API_BASED, false);
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

    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());

    Schema schema = implSpy.fetchTableSchema("u_custom_table",
      "dummy-access-token", SourceValueType.SHOW_ACTUAL_VALUE, SchemaType.METADATA_API_BASED, false);

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
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("sys_user", "dummy-access-token",
      SourceValueType.SHOW_DISPLAY_VALUE, SchemaType.SCHEMA_API_BASED, false);
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
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("incident", "dummy-access-token",
      SourceValueType.SHOW_DISPLAY_VALUE, SchemaType.METADATA_API_BASED, false);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("business_stc").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("calendar_stc").getSchema().getUnionSchemas().get(0).getType());
  }

  @Test
  public void testFetchTableSchema_WithGlide_ListType_ParseAsArray() throws Exception {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    String jsonResponse = "{\n" +
      "  \"result\": {\n" +
      "    \"columns\": {\n" +
      "      \"u_glide_array2\": {\n" +
      "         \"label\": \"glide_array2\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array2\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       },\n" +
      "         \"u_glide_array3\": {\n" +
      "         \"label\": \"glide_array3\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array3\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("u_custom_13", "dummy-access-token",
      SourceValueType.SHOW_ACTUAL_VALUE, SchemaType.METADATA_API_BASED, false);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.ARRAY,
                        schema.getField("u_glide_array2").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.ARRAY,
                        schema.getField("u_glide_array3").getSchema().getUnionSchemas().get(0).getType());
  }

  @Test
  public void testFetchTableSchema_WithGlide_ListType_ParseAsString() throws Exception {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    String jsonResponse = "{\n" +
      "  \"result\": {\n" +
      "    \"columns\": {\n" +
      "      \"u_glide_array2\": {\n" +
      "         \"label\": \"glide_array2\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array2\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       },\n" +
      "         \"u_glide_array3\": {\n" +
      "         \"label\": \"glide_array3\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array3\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("u_custom_13", "dummy-access-token",
     SourceValueType.SHOW_ACTUAL_VALUE, SchemaType.METADATA_API_BASED, true);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("u_glide_array2").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("u_glide_array3").getSchema().getUnionSchemas().get(0).getType());
  }

  @Test
  public void testFetchTableSchema_WithGlide_Static_ListType_ParseAsArray() throws Exception {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    String jsonResponse = "{\n" +
      "  \"result\": {\n" +
      "    \"columns\": {\n" +
      "      \"u_glide_array2\": {\n" +
      "         \"label\": \"glide_array2\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array2\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       },\n" +
      "         \"u_glide_array3\": {\n" +
      "         \"label\": \"glide_array3\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array3\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("u_custom_13", "dummy-access-token",
      SourceValueType.SHOW_DISPLAY_VALUE, SchemaType.METADATA_API_BASED, false);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.ARRAY,
                        schema.getField("u_glide_array2").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.ARRAY,
                        schema.getField("u_glide_array3").getSchema().getUnionSchemas().get(0).getType());
  }

  @Test
  public void testFetchTableSchema_WithGlide_Static_ListType_ParseAsString() throws Exception {
    ServiceNowConnectorConfig mockConfig = Mockito.mock(ServiceNowConnectorConfig.class);
    ServiceNowTableAPIClientImpl impl = new ServiceNowTableAPIClientImpl(mockConfig, true);
    ServiceNowTableAPIClientImpl implSpy = Mockito.spy(impl);
    String jsonResponse = "{\n" +
      "  \"result\": {\n" +
      "    \"columns\": {\n" +
      "      \"u_glide_array2\": {\n" +
      "         \"label\": \"glide_array2\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array2\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       },\n" +
      "         \"u_glide_array3\": {\n" +
      "         \"label\": \"glide_array3\",\n" +
      "         \"type\": \"glide_static_list\",\n" +
      "         \"base_type\": \"string\",\n" +
      "         \"name\": \"u_glide_array3\",\n" +
      "         \"internal_type\": \"glide_list\"\n" +
      "       }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    RestAPIResponse mockResponse = new RestAPIResponse(Collections.emptyMap(), jsonResponse, null);
    Mockito.doReturn(mockResponse).when(implSpy).executeGetWithRetries(Mockito.any());
    Schema schema = implSpy.fetchTableSchema("u_custom_13", "dummy-access-token",
      SourceValueType.SHOW_DISPLAY_VALUE, SchemaType.METADATA_API_BASED, true);
    Assert.assertNotNull(schema);
    Assert.assertEquals("record", schema.getDisplayName());
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("u_glide_array2").getSchema().getUnionSchemas().get(0).getType());
    Assert.assertEquals(Schema.Type.STRING,
                        schema.getField("u_glide_array3").getSchema().getUnionSchemas().get(0).getType());
  }
}
