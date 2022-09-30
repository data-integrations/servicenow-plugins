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
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.servicenow.ServiceNowBaseConfig;
import io.cdap.plugin.servicenow.restapi.RestAPIClient;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.sink.model.SchemaResponse;
import io.cdap.plugin.servicenow.util.SchemaBuilder;
import io.cdap.plugin.servicenow.util.ServiceNowColumn;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceValueType;
import io.cdap.plugin.servicenow.util.Util;
import org.apache.http.HttpEntity;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

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
  private static final Gson gson = new Gson();
  public static JsonArray serviceNowJsonResultArray;
  private final ServiceNowBaseConfig conf;

  public ServiceNowTableAPIClientImpl(ServiceNowBaseConfig conf) {
    this.conf = conf;
  }

  public String getAccessToken() throws OAuthSystemException, OAuthProblemException {
    return generateAccessToken(String.format(OAUTH_URL_TEMPLATE, conf.getRestApiEndpoint()), conf.getClientId(),
                               conf.getClientSecret(), conf.getUser(), conf.getPassword());
  }

  /**
   * Retries to get the access token and returns the same when OAuthSystemException is thrown
   */
  public String getAccessTokenRetryableMode() throws ExecutionException, RetryException {

    Callable fetchToken = this::getAccessToken;

    Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
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
  public List<Map<String, Object>> fetchTableRecords(String tableName, SourceValueType valueType, String startDate,
                                                     String endDate, int offset, int limit) {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false)
      .setExcludeReferenceLink(true)
      .setDisplayValue(valueType)
      .setLimit(limit);

    if (offset > 0) {
      requestBuilder.setOffset(offset);
    }

    applyDateRangeToRequest(requestBuilder, startDate, endDate);

    RestAPIResponse apiResponse = null;

    try {
      String accessToken = getAccessToken();
      requestBuilder.setAuthHeader(accessToken);
      apiResponse = executeGet(requestBuilder.build());
      if (!apiResponse.isSuccess()) {
        if (apiResponse.isRetryable()) {
          throw new RetryableException();
        }
        return Collections.emptyList();
      }

      return parseResponseToResultListOfMap(apiResponse.getResponseBody());
    } catch (OAuthSystemException e) {
      throw new RetryableException();
    } catch (OAuthProblemException e) {
      LOG.error("Error in fetchTableRecords", e);
      return Collections.emptyList();
    }
  }

  /**
   * Create a new record in the ServiceNow Table
   *
   * @param tableName ServiceNow Table name
   * @param entity    Details of the Record to be created
   */
  public String createRecord(String tableName, HttpEntity entity) throws IOException {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false);
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

      if (!apiResponse.isSuccess()) {
        LOG.error("Error - {}", getErrorMessage(apiResponse.getResponseBody()));
      } else {
        LOG.info(apiResponse.getResponseBody());
      }
    } catch (OAuthSystemException | OAuthProblemException | UnsupportedEncodingException e) {
      LOG.error("Error in creating a new record", e);
      throw new RuntimeException("Error in creating a new record");
    }

    return systemID;
  }

  /**
   * Fetches the System Id of a new Record.
   *
   * @param apiResponse API response after Creating a record
   */

  private String getSystemId(RestAPIResponse apiResponse) {
    JsonObject jsonObject = gson.fromJson(apiResponse.getResponseBody(), JsonObject.class);
    JsonObject result = (JsonObject) jsonObject.get(ServiceNowConstants.RESULT);

    return result.get(ServiceNowConstants.SYSTEM_ID).getAsString();
  }

  /**
   * Return a record from ServiceNow application.
   *
   * @param tableName The ServiceNow table name
   * @param query     The query
   */
  public Map<String, String> getRecordFromServiceNowTable(String tableName, String query)
    throws OAuthProblemException, OAuthSystemException {

    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false)
      .setQuery(query);

    RestAPIResponse apiResponse = null;
    String accessToken = getAccessToken();
    requestBuilder.setAuthHeader(accessToken);
    apiResponse = executeGet(requestBuilder.build());
    JsonObject jsonObject = gson.fromJson(apiResponse.getResponseBody(), JsonObject.class);
    serviceNowJsonResultArray = jsonObject.getAsJsonArray(ServiceNowConstants.RESULT);
    Map<String, String> responseMap = gson.fromJson(serviceNowJsonResultArray.get(0), Map.class);

    return responseMap;
  }


  /**
   * Fetches the table schema for ServiceNow table.
   *
   * @param tableName        The ServiceNow table name
   * @param valueType        The value type
   * @param startDate        The start date
   * @param endDate          The end date
   * @param fetchRecordCount A flag that decides whether to fetch total record count or not
   * @return
   */
  public ServiceNowTableDataResponse fetchTableSchema(String tableName, SourceValueType valueType, String startDate,
                                                      String endDate, boolean fetchRecordCount) {
    return fetchTableSchemaUsingFirstRecord(tableName, valueType, startDate, endDate, fetchRecordCount);
  }

  private ServiceNowTableDataResponse fetchTableSchemaUsingFirstRecord(String tableName, SourceValueType valueType,
                                                                       String startDate, String endDate,
                                                                       boolean fetchRecordCount) {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, false)
      .setExcludeReferenceLink(true)
      .setDisplayValue(valueType)
      .setLimit(1);
    applyDateRangeToRequest(requestBuilder, startDate, endDate);

    RestAPIResponse apiResponse = null;

    try {
      String accessToken = getAccessToken();
      requestBuilder.setAuthHeader(accessToken);

      // Get the response JSON and fetch the header X-Total-Count. Set the value to recordCount
      if (fetchRecordCount) {
        requestBuilder.setResponseHeaders(ServiceNowConstants.HEADER_NAME_TOTAL_COUNT);
      }

      apiResponse = executeGet(requestBuilder.build());
      if (!apiResponse.isSuccess()) {
        LOG.error("Error - {}", getErrorMessage(apiResponse.getResponseBody()));
        return null;
      }

      ServiceNowTableDataResponse tableDataResponse = new ServiceNowTableDataResponse();

      List<Map<String, Object>> result = parseResponseToResultListOfMap(apiResponse.getResponseBody());
      List<ServiceNowColumn> columns = new ArrayList<>();

      if (result != null && !result.isEmpty()) {
        Map<String, Object> firstRecord = result.get(0);
        for (String key : firstRecord.keySet()) {
          columns.add(new ServiceNowColumn(key, "string"));
        }
      }

      tableDataResponse.setColumns(columns);
      if (fetchRecordCount) {
        tableDataResponse.setTotalRecordCount(getRecordCountFromHeader(apiResponse));
      }

      return tableDataResponse;
    } catch (OAuthSystemException | OAuthProblemException e) {
      LOG.error("Error in fetchFirstRecordFromTable", e);
      return null;
    }
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

  public List<Map<String, Object>> parseResponseToResultListOfMap(String responseBody) {


    JsonObject jo = gson.fromJson(responseBody, JsonObject.class);
    JsonArray ja = jo.getAsJsonArray(ServiceNowConstants.RESULT);

    Type type = new TypeToken<List<Map<String, Object>>>() {
    }.getType();
    return gson.fromJson(ja, type);
  }

  private String getErrorMessage(String responseBody) {
    try {
      JsonObject jo = gson.fromJson(responseBody, JsonObject.class);
      return jo.getAsJsonObject(ServiceNowConstants.ERROR).get(ServiceNowConstants.MESSAGE).getAsString();
    } catch (Exception e) {
      return e.getMessage();
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
  public List<Map<String, Object>> fetchTableRecordsRetryableMode(String tableName, SourceValueType valueType,
                                                                  String startDate, String endDate, int offset,
                                                                  int limit) {
    final List<Map<String, Object>> results = new ArrayList<>();
    Callable<Boolean> fetchRecords = () -> {
      results.addAll(fetchTableRecords(tableName, valueType, startDate, endDate, offset, limit));
      return true;
    };

    Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
      .retryIfExceptionOfType(RetryableException.class)
      .withWaitStrategy(WaitStrategies.exponentialWait(ServiceNowConstants.WAIT_TIME, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(ServiceNowConstants.MAX_NUMBER_OF_RETRY_ATTEMPTS))
      .build();

    try {
      retryer.call(fetchRecords);
    } catch (RetryException | ExecutionException e) {
      LOG.error("Data Recovery failed for batch {} to {}.", offset,
                (offset + limit));
    }

    return results;
  }

  /**
   * @param tableName ServiceNow table name for which schema is getting fetched
   * @param collector FailureCollector
   * @return schema for given ServiceNow table
   */
  @Nullable
  public Schema fetchServiceNowTableSchema(String tableName, FailureCollector collector) {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName, true)
      .setExcludeReferenceLink(true);

    RestAPIResponse apiResponse;
    try {
      String accessToken = getAccessToken();
      requestBuilder.setAuthHeader(accessToken);
      apiResponse = executeGet(requestBuilder.build());
      if (!apiResponse.isSuccess()) {
        LOG.error("Error - {}", getErrorMessage(apiResponse.getResponseBody()));
        collector.addFailure("Unable to fetch schema for table " + tableName, null).
          withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAME);
        return null;
      }
      List<SchemaResponse> result = parseSchemaResponse(apiResponse.getResponseBody());
      List<ServiceNowColumn> columns = new ArrayList<>();

      if (result != null && !result.isEmpty()) {
        for (SchemaResponse field : result) {
          columns.add(new ServiceNowColumn(field.getName(), field.getInternalType()));
        }
      }
      return SchemaBuilder.constructSchema(tableName, columns);
    } catch (OAuthSystemException | OAuthProblemException e) {
      LOG.error("Error in connection - {}", e);
      collector.addFailure("Connection failed. Unable to fetch schema for table " + tableName, null);
    }
    return null;
  }

  @VisibleForTesting
  public List<SchemaResponse> parseSchemaResponse(String responseBody) {
    JsonObject jo = gson.fromJson(responseBody, JsonObject.class);
    JsonArray ja = jo.getAsJsonArray(ServiceNowConstants.RESULT);
    Type type = new TypeToken<List<SchemaResponse>>() {
    }.getType();
    return gson.fromJson(ja, type);
  }

}
