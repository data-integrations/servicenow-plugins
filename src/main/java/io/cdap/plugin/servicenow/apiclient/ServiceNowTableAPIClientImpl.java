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

package io.cdap.plugin.servicenow.apiclient;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.servicenow.connector.ServiceNowConnectorConfig;
import io.cdap.plugin.servicenow.model.APIResponse;
import io.cdap.plugin.servicenow.model.CreateRecordAPIResponse;
import io.cdap.plugin.servicenow.model.MetadataAPISchemaField;
import io.cdap.plugin.servicenow.model.MetadataAPISchemaResponse;
import io.cdap.plugin.servicenow.model.SchemaAPISchemaField;
import io.cdap.plugin.servicenow.model.SchemaAPISchemaResponse;
import io.cdap.plugin.servicenow.restapi.RestAPIClient;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.util.SchemaBuilder;
import io.cdap.plugin.servicenow.util.SchemaType;
import io.cdap.plugin.servicenow.util.ServiceNowColumn;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceValueType;
import io.cdap.plugin.servicenow.util.Util;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import static io.cdap.plugin.servicenow.util.ServiceNowConstants.STC_FIELD_SUFFIX;

/**
 * Implementation class for ServiceNow Table API.
 */
public class ServiceNowTableAPIClientImpl extends RestAPIClient {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowTableAPIClientImpl.class);
  private static final String DATE_RANGE_TEMPLATE = "%sBETWEENjavascript:gs.dateGenerate('%s','start')" +
    "@javascript:gs.dateGenerate('%s','end')";
  private static final String FIELD_CREATED_ON = "sys_created_on";
  private static final String FIELD_UPDATED_ON = "sys_updated_on";
  private static final String OAUTH_URL_TEMPLATE = "%s/oauth_token.do";
  private static final String GLIDE_TIME_DATATYPE = "glide_time";
  private static final String GLIDE_DATE_TIME_DATATYPE = "glide_date_time";
  private static final Gson GSON = new Gson();
  private final ServiceNowConnectorConfig conf;
  public final SchemaType schemaType;
  public static JsonArray serviceNowJsonResultArray;

  public ServiceNowTableAPIClientImpl(ServiceNowConnectorConfig conf, Boolean useConnection) {
    this.conf = conf;
    this.schemaType = getSchemaTypeBasedOnUseConnection(useConnection);
  }

  public String getAccessToken() throws ServiceNowAPIException {
    try {
      return generateAccessToken(String.format(OAUTH_URL_TEMPLATE, conf.getRestApiEndpoint()),
          conf.getClientId(),
          conf.getClientSecret(), conf.getUser(), conf.getPassword());
    } catch (OAuthProblemException | OAuthSystemException e) {
      throw new ServiceNowAPIException("An error occurred while authenticating.", e, null, false);
    }
  }

  private boolean isExceptionRetryable(Throwable t) {
    return t instanceof ServiceNowAPIException && ((ServiceNowAPIException) t).isErrorRetryable();
  }

  /**
   * Retries to get the access token and returns the same when OAuthSystemException is thrown
   */
  public String getAccessTokenRetryableMode() throws ExecutionException, RetryException {

    Callable fetchToken = this::getAccessToken;

    Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
      .retryIfException(this::isExceptionRetryable)
      .retryIfExceptionOfType(OAuthSystemException.class)
      .withWaitStrategy(WaitStrategies.fixedWait(ServiceNowConstants.BASE_DELAY, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(ServiceNowConstants.MAX_NUMBER_OF_RETRY_ATTEMPTS))
      .build();

    return retryer.call(fetchToken);
  }

  /**
   * Fetch the list of records from ServiceNow table.
   *
   * @param tableName The ServiceNow table name
   * @param valueType The value type
   * @param startDate The start date
   * @param endDate   The end date
   * @param offset    The number of records to skip
   * @param limit     The number of records to be fetched
   * @return The list of Map; each Map representing a table row
   */
  public RestAPIResponse fetchTableRecords(
      String tableName,
      SourceValueType valueType,
      String startDate,
      String endDate,
      int offset,
      int limit)
      throws ServiceNowAPIException {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false, schemaType)
      .setExcludeReferenceLink(true)
      .setDisplayValue(valueType)
      .setLimit(limit);

    if (offset > 0) {
      requestBuilder.setOffset(offset);
    }

    applyDateRangeToRequest(requestBuilder, startDate, endDate);

    String accessToken = getAccessToken();
    requestBuilder.setAuthHeader(accessToken);
    RestAPIResponse apiResponse = executeGetWithRetries(requestBuilder.build());
    return apiResponse;
    //return parseResponseToResultListOfMap(apiResponse.getResponseBody());
    // return parseResponseStreamToRecord(apiResponse.getInputStream());

  }

  private void applyDateRangeToRequest(ServiceNowTableAPIRequestBuilder requestBuilder, String startDate,
                                       String endDate) {
    String dateRange = generateDateRangeQuery(startDate, endDate);
    if (!Strings.isNullOrEmpty(dateRange)) {
      requestBuilder.setQuery(dateRange);
    }
  }

  private String generateDateRangeQuery(String startDate, String endDate) {
    if (Util.isNullOrEmpty(startDate) || Util.isNullOrEmpty(endDate)) {
      return "";
    }

    String dateRange = "";
    try {
      String createdOnDateRange = String.format(DATE_RANGE_TEMPLATE, FIELD_CREATED_ON, startDate, endDate);
      String updatedOnDateRange = String.format(DATE_RANGE_TEMPLATE, FIELD_UPDATED_ON, startDate, endDate);
      dateRange = String.format("%s^OR%s", createdOnDateRange, updatedOnDateRange);
    } catch (Exception e) {
      LOG.error("Error in generateDateRangeQuery, hence ignoring the date range", e);
    }

    return dateRange;
  }

  private int getRecordCountFromHeader(RestAPIResponse apiResponse) {
    String headerValue = apiResponse.getHeaders().get(ServiceNowConstants.HEADER_NAME_TOTAL_COUNT);
    return Strings.isNullOrEmpty(headerValue) ? 0 : Integer.parseInt(headerValue);
  }

  public List<Map<String, String>> parseResponseToResultListOfMap(String responseBody) {

    JsonObject jo = GSON.fromJson(responseBody, JsonObject.class);
    JsonArray ja = jo.getAsJsonArray(ServiceNowConstants.RESULT);

    Type type = new TypeToken<List<Map<String, Object>>>() {
    }.getType();
    return GSON.fromJson(ja, type);
  }

  public Map<String, String> parseResponseStreamToRecord(InputStream in) throws ServiceNowAPIException {
    // List<Map<String, String>> records = new ArrayList<>();
    // InputStream in = httpResponse.getEntity().getContent();
    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
         JsonReader jsonReader = new JsonReader(reader)) {
      jsonReader.setLenient(true);
      jsonReader.beginObject();
      Map<String, String> record = new HashMap<>();
      while (jsonReader.hasNext()) {
        String name = jsonReader.nextName();
        if (ServiceNowConstants.RESULT.equals(name) && jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
          jsonReader.beginArray();
          while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
              String field = jsonReader.nextName();
              JsonToken token = jsonReader.peek();
              // JsonElement resultElement = GSON.fromJson(jsonReader, JsonElement.class);
              // responseBody = resultElement.toString();
              record.put(field, token == JsonToken.NULL ? null : jsonReader.nextString());
            }
            jsonReader.endObject();
            // records.add(record);
          }
          jsonReader.endArray();
        } else {
          // skip other fields (e.g., metadata like result_count)
          jsonReader.skipValue();
        }
      }
      jsonReader.endObject();
      return record;
    } catch (IOException e) {
      throw new ServiceNowAPIException(e, null);
    }
  }

  private String getErrorMessage(String responseBody) {
    try {
      JsonObject jo = GSON.fromJson(responseBody, JsonObject.class);
      JsonObject error = jo.getAsJsonObject(ServiceNowConstants.ERROR);
      if (error != null) {
        String errorMessage = error.get(ServiceNowConstants.MESSAGE).getAsString();
        String errorDetail = error.get(ServiceNowConstants.ERROR_DETAIL).getAsString();
        if (errorMessage != null && errorDetail != null) {
          return String.format("%s:%s",
                               jo.getAsJsonObject(ServiceNowConstants.ERROR).get(ServiceNowConstants.MESSAGE)
                                 .getAsString(),
                               jo.getAsJsonObject(ServiceNowConstants.ERROR).get(ServiceNowConstants.ERROR_DETAIL)
                                 .getAsString());
        }
      }
      return null;

    } catch (Exception e) {
      return String.format("%s:%s", e.getMessage(), responseBody);
    }
  }

  /**
   * Attempt four times with an exponential delay of 120 seconds to fetch the list of records from ServiceNow table when
   * RetryableException is thrown          .
   *
   * @param tableName The ServiceNow table name
   * @param valueType The value type
   * @param startDate The start date
   * @param endDate   The end date
   * @param offset    The number of records to skip
   * @param limit     The number of records to be fetched
   * @return The list of Map; each Map representing a table row
   */
  public RestAPIResponse fetchTableRecordsRetryableMode(String tableName, SourceValueType valueType,
                                                                  String startDate, String endDate, int offset,
                                                                  int limit) throws ServiceNowAPIException {
    //final List<Map<String, String>> results = new ArrayList<>();
    final RestAPIResponse[] restAPIResponse = new RestAPIResponse[1];
    Callable<Boolean> fetchRecords = () -> {
      // results.addAll(fetchTableRecords(tableName, valueType, startDate, endDate, offset, limit));
      restAPIResponse[0] = fetchTableRecords(tableName, valueType, startDate, endDate, offset, limit);
      return true;
    };

    Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
      .retryIfException(this::isExceptionRetryable)
      .withWaitStrategy(WaitStrategies.exponentialWait(ServiceNowConstants.WAIT_TIME, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(ServiceNowConstants.MAX_NUMBER_OF_RETRY_ATTEMPTS))
      .build();

    try {
      retryer.call(fetchRecords);
    } catch (RetryException | ExecutionException e) {
      throw new ServiceNowAPIException(
          String.format("Data Recovery failed for batch %s to %s.", offset, (offset + limit)),
          e, null, false);
    }

    return restAPIResponse[0];
  }

  /**
   * Fetch schema for actual value type
   * @param tableName ServiceNow table name for which schema is getting fetched
   * @param collector FailureCollector
   * @return schema for given ServiceNow table
   */
  @Nullable
  public Schema fetchTableSchema(String tableName, FailureCollector collector) {
    Schema schema = null;
    try {
      schema = fetchTableSchema(tableName, SourceValueType.SHOW_ACTUAL_VALUE);
    } catch (Exception e) {
      LOG.error("Failed to fetch schema on table {}", tableName, e);
      collector.addFailure(String.format("Connection failed. Unable to fetch schema for table: %s. Cause: %s",
                                         tableName, e.getMessage()), null);
    }
    return schema;
  }

  @VisibleForTesting
  public MetadataAPISchemaResponse parseSchemaResponse(String responseBody) {
    return GSON.fromJson(responseBody, MetadataAPISchemaResponse.class);
  }

  /**
   * Fetches the table schema from ServiceNow
   *
   * @param tableName ServiceNow table name for which schema is getting fetched
   * @param valueType Whether to fetch schema for actual value or display value
   * @return schema for given ServiceNow table
   * @throws ServiceNowAPIException
   */
  public Schema fetchTableSchema(String tableName, SourceValueType valueType)
      throws ServiceNowAPIException {
    return fetchTableSchema(tableName, getAccessToken(), valueType, schemaType);
  }

  private SchemaType getSchemaTypeBasedOnUseConnection(Boolean useConnection) {
    // use connection was added in release/1.2, so it will be null for users who are upgrading from release/1.1
    // This is added to support backward compatibility for 1.1 users.
    if (useConnection == null) {
      return SchemaType.STRING_BASED;
    }
    return SchemaType.SCHEMA_API_BASED;
  }

  /**
   * Fetches the table schema from ServiceNow
   *
   * @param tableName ServiceNow table name for which schema is getting fetched
   * @param accessToken Access Token to use
   * @param valueType Type of value (Actual/Display)
   * @param schemaType Enum to determine which approach to take to fetch schema.
   * @return schema for given ServiceNow table
   */
  public Schema fetchTableSchema(String tableName, String accessToken, SourceValueType valueType,
                                 SchemaType schemaType)
      throws ServiceNowAPIException {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, true, schemaType)
      .setExcludeReferenceLink(true);

    RestAPIResponse restAPIResponse;
    requestBuilder.setAuthHeader(accessToken);
    restAPIResponse = executeGetWithRetries(requestBuilder.build());
    List<ServiceNowColumn> columns = new ArrayList<>();

    if (schemaType == SchemaType.METADATA_API_BASED) {
      return prepareSchemaWithMetadataAPI(restAPIResponse, columns, tableName, valueType);
    } else if (schemaType == SchemaType.SCHEMA_API_BASED) {
      return prepareSchemaWithSchemaAPI(restAPIResponse, columns, tableName);
    } else {
      return prepareStringBasedSchema(restAPIResponse, columns, tableName);
    }
  }

  /**
   * Processes a schema response obtained from the ServiceNow Table API (without using Metadata API)
   * and constructs a {@link Schema} object based on the parsed column definitions.
   *
   * <p>This method parses the raw JSON response body into a list of schema fields,
   * extracts the internal column types, and appends them to the provided column list.
   * The final schema is constructed using the {@link SchemaBuilder} utility.</p>
   *
   * @param restAPIResponse The raw API response received from the ServiceNow Table API.
   * @param columns         A list to which parsed {@link ServiceNowColumn} objects will be added.
   * @param tableName       The name of the table for which the schema is being constructed.
   *
   * @return A {@link Schema} object representing the table structure as interpreted from the Schema API.
   *
   * @throws RuntimeException if the schema response is null or contains no result.
   */
  private Schema prepareSchemaWithSchemaAPI(RestAPIResponse restAPIResponse, List<ServiceNowColumn> columns,
                                            String tableName) throws ServiceNowAPIException {
    SchemaAPISchemaResponse schemaAPISchemaResponse =
      GSON.fromJson(restAPIResponse.getResponseBody(), SchemaAPISchemaResponse.class);

    if (schemaAPISchemaResponse.getResult() == null || schemaAPISchemaResponse.getResult().isEmpty()) {
      throw new ServiceNowAPIException(
       "Schema Response does not contain any result", null, null, false);
    }

    for (SchemaAPISchemaField field : schemaAPISchemaResponse.getResult()) {
      columns.add(new ServiceNowColumn(field.getName(), field.getInternalType()));
    }
    return SchemaBuilder.constructSchema(tableName, columns);
  }

  /**
   * Parses a ServiceNow schema response obtained via the Metadata API and constructs a
   * {@link Schema} object using the extracted field information and value type preferences.
   *
   * <p>This method reads the JSON response, extracts the column metadata including field names
   * and data types, and adds each as a {@link ServiceNowColumn} to the provided list. The choice
   * between internal values and display values is based on the {@code valueType} parameter.</p>
   *
   * @param restAPIResponse The response returned from the ServiceNow Metadata API.
   * @param columns         A list to which parsed {@link ServiceNowColumn} definitions will be added.
   * @param tableName       The name of the ServiceNow table for which the schema is being generated.
   * @param valueType       The value type preference (e.g., {@code SHOW_DISPLAY_VALUE} or {@code USE_INTERNAL_VALUE}).
   *                        Determines whether to use display types or internal types in the resulting schema.
   *
   * @return A {@link Schema} object representing the table structure as interpreted from the Metadata API.
   *
   * @throws RuntimeException if the response does not contain valid column information.
   */
  private Schema prepareSchemaWithMetadataAPI(RestAPIResponse restAPIResponse, List<ServiceNowColumn> columns,
                                              String tableName, SourceValueType valueType) throws
    ServiceNowAPIException {
    MetadataAPISchemaResponse metadataAPISchemaResponse = parseSchemaResponse(restAPIResponse.getResponseBody());

    if (metadataAPISchemaResponse.getResult() == null || metadataAPISchemaResponse.getResult().getColumns() == null ||
      metadataAPISchemaResponse.getResult().getColumns().isEmpty()) {
      throw new ServiceNowAPIException(
        "Schema Response does not contain any result", null, null, false);
    }

    for (MetadataAPISchemaField field : metadataAPISchemaResponse.getResult().getColumns().values()) {
      if (valueType.equals(SourceValueType.SHOW_DISPLAY_VALUE)) {
        if (!Objects.equals(field.getType(), field.getInternalType())) {
          columns.add(new ServiceNowColumn(field.getName(), field.getType()));
        } else if (field.getName().endsWith(STC_FIELD_SUFFIX) && field.getType().equals("integer")) {
          columns.add(new ServiceNowColumn(field.getName(), Schema.Type.STRING.name()));
        } else {
          columns.add(new ServiceNowColumn(field.getName(), field.getInternalType()));
        }
      } else if (valueType.equals(SourceValueType.SHOW_ACTUAL_VALUE) &&
        GLIDE_TIME_DATATYPE.equalsIgnoreCase(field.getInternalType())) {
        columns.add(new ServiceNowColumn(field.getName(), GLIDE_DATE_TIME_DATATYPE));
      } else {
        columns.add(new ServiceNowColumn(field.getName(), field.getInternalType()));
      }
    }
    return SchemaBuilder.constructSchema(tableName, columns);
  }

  /**
   * Get the total number of records in the table
   *
   * @param tableName ServiceNow table name for which record count is fetched.
   * @return the table record count
   * @throws ServiceNowAPIException
   */
  public int getTableRecordCount(String tableName)
      throws ServiceNowAPIException {
    return getTableRecordCount(tableName, getAccessToken());
  }

  /**
   * Get the total number of records in the table
   *
   * @param tableName ServiceNow table name for which record count is fetched.
   * @param accessToken Access Token for the call
   * @return the table record count
   * @throws ServiceNowAPIException
   */
  public int getTableRecordCount(String tableName, String accessToken) throws ServiceNowAPIException {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false, schemaType)
      .setExcludeReferenceLink(true)
      .setDisplayValue(SourceValueType.SHOW_DISPLAY_VALUE)
      .setLimit(1);
    RestAPIResponse apiResponse = null;
    requestBuilder.setResponseHeaders(ServiceNowConstants.HEADER_NAME_TOTAL_COUNT);
    requestBuilder.setAuthHeader(accessToken);
    apiResponse = executeGetWithRetries(requestBuilder.build());
    return getRecordCountFromHeader(apiResponse);
  }

  /**
   * Create a new record in the ServiceNow Table
   *
   * @param tableName ServiceNow Table name
   * @param entity    Details of the Record to be created
   * @description This function is being used in end-to-end (e2e) tests to fetch a record from the ServiceNow Table.
   */
  public String createRecord(String tableName, HttpEntity entity) throws IOException, ServiceNowAPIException {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false, schemaType);
    String systemID;
    RestAPIResponse apiResponse = null;
    try {
      String accessToken = getAccessToken();
      requestBuilder.setAuthHeader(accessToken);
      requestBuilder.setAcceptHeader("application/json");
      requestBuilder.setContentTypeHeader("application/json");
      requestBuilder.setEntity(entity);
      apiResponse = executePost(requestBuilder.build());

      systemID = String.valueOf(getSystemId(apiResponse));
    } catch (IOException e) {
      throw new ServiceNowAPIException("Error in creating a new record", e, null, false);
    }
    return systemID;
  }

  /**
   * Create a new record in the ServiceNow Table using display mode as true
   *
   * @param tableName ServiceNow Table name
   * @param entity    Details of the Record to be created
   * @description This function is being used in end-to-end (e2e) tests to fetch a record from the ServiceNow Table.
   */
  public String createRecordInDisplayMode(String tableName, HttpEntity entity) throws
    IOException, ServiceNowAPIException {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false, SchemaType.SCHEMA_API_BASED);
    String systemID;
    RestAPIResponse apiResponse = null;
    try {
      String accessToken = getAccessToken();
      requestBuilder.setAuthHeader(accessToken);
      requestBuilder.setAcceptHeader("application/json");
      requestBuilder.setContentTypeHeader("application/json");
      requestBuilder.setEntity(entity);
      requestBuilder.setDisplayValue(SourceValueType.SHOW_DISPLAY_VALUE);
      apiResponse = executePost(requestBuilder.build());

      systemID = String.valueOf(getSystemId(apiResponse));
    } catch (IOException e) {
      throw new ServiceNowAPIException("Error in creating a new record", e, null, false);
    }
    return systemID;
  }

  private String getSystemId(RestAPIResponse restAPIResponse) {
    CreateRecordAPIResponse apiResponse = GSON.fromJson(restAPIResponse.getResponseBody(),
                                                           CreateRecordAPIResponse.class);
    return apiResponse.getResult().get(ServiceNowConstants.SYSTEM_ID).toString();
  }

  /**
   * This function is being used in end-to-end (e2e) tests to fetch a record Return a record from
   * ServiceNow application.
   *
   * @param tableName The ServiceNow table name
   * @param query The query
   */
  public Map<String, String> getRecordFromServiceNowTable(String tableName, String query)
      throws ServiceNowAPIException {

    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false, schemaType)
      .setQuery(query);

    RestAPIResponse restAPIResponse;
    String accessToken = getAccessToken();
    requestBuilder.setAuthHeader(accessToken);
    restAPIResponse = executeGetWithRetries(requestBuilder.build());

    APIResponse apiResponse = GSON.fromJson(restAPIResponse.getResponseBody(), APIResponse.class);
    return apiResponse.getResult().get(0);
  }

  /**
   * Processes the response obtained from the ServiceNow Table API
   * and constructs a {@link Schema} object based on the first record.
   *
   * @param restAPIResponse The raw API response received from the ServiceNow Table API.
   * @param columns         A list to which parsed {@link ServiceNowColumn} objects will be added.
   * @param tableName       The name of the table for which the schema is being constructed.
   *
   * @return A {@link Schema} object representing the table structure as interpreted from the Schema API.
   * Return null if table has no  records
   *
   * @throws RuntimeException if the schema response is null or contains no result.
   */
  private Schema prepareStringBasedSchema(RestAPIResponse restAPIResponse, List<ServiceNowColumn> columns,
                                          String tableName) {
    List<Map<String, String>> result = parseResponseToResultListOfMap(restAPIResponse.getResponseBody());
    if (result != null && !result.isEmpty()) {
      Map<String, String> firstRecord = result.get(0);
      for (String key : firstRecord.keySet()) {
        columns.add(new ServiceNowColumn(key, "string"));
      }
      return SchemaBuilder.constructSchema(tableName, columns);
    }
    return null;
  }


  /**
   * This function is being used in end-to-end (e2e) tests to delete a record from
   * ServiceNow application.
   *
   * @param tableName The ServiceNow table name
   * @param sysId system Id of the record
   */
  public void deleteRecordFromServiceNowTable(String tableName, String sysId)
    throws ServiceNowAPIException, IOException {

    String accessToken = getAccessToken();
    String endpoint = String.format("%s/api/now/table/%s/%s", this.conf.getRestApiEndpoint(), tableName, sysId);

    HttpDelete deleteRequest = new HttpDelete(endpoint);
    deleteRequest.setHeader("Authorization", "Bearer " + accessToken);
    deleteRequest.setHeader("Accept", "application/json");

    try (CloseableHttpClient httpClient = HttpClients.createDefault();
         CloseableHttpResponse response = httpClient.execute(deleteRequest)) {

      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 204) {
        throw new ServiceNowAPIException(
          String.format("Failed to delete record. Status: %d", statusCode), null, null, false);
      }
      LOG.info("Record deleted successfully. sys_id: " + sysId);
    }
  }
}
