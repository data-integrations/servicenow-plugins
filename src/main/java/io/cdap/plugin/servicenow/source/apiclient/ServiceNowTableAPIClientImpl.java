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

package io.cdap.plugin.servicenow.source.apiclient;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.cdap.plugin.servicenow.restapi.RestAPIClient;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.source.ServiceNowSourceConfig;
import io.cdap.plugin.servicenow.source.util.ServiceNowColumn;
import io.cdap.plugin.servicenow.source.util.Util;
import org.apache.http.HttpEntity;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation class for ServiceNow Table API.
 */
public class ServiceNowTableAPIClientImpl extends RestAPIClient {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowTableAPIClientImpl.class);
  private static final String DATE_RANGE_TEMPLATE = "%sBETWEENjavascript:gs.dateGenerate('%s','start')" +
    "@javascript:gs.dateGenerate('%s','end')";
  private static final String FIELD_CREATED_ON = "sys_created_on";
  private static final String FIELD_UPDATED_ON = "sys_updated_on";
  private static final String HEADER_NAME_TOTAL_COUNT = "X-Total-Count";
  private static final String OAUTH_URL_TEMPLATE = "%s/oauth_token.do";

  private ServiceNowSourceConfig conf;

  public ServiceNowTableAPIClientImpl(ServiceNowSourceConfig conf) {
    this.conf = conf;
  }

  public String getAccessToken() throws OAuthSystemException, OAuthProblemException {
    return generateAccessToken(String.format(OAUTH_URL_TEMPLATE, conf.getRestApiEndpoint()), conf.getClientId(),
      conf.getClientSecret(), conf.getUser(), conf.getPassword());
  }

  /**
   * Fetch the list of records from ServiceNow table.
   *
   * @param tableName The ServiceNow table name
   * @param startDate The start date
   * @param endDate The end date
   * @param offset The number of records to skip
   * @param limit The number of records to be fetched
   * @return The list of Map; each Map representing a table row
   */
  public List<Map<String, Object>> fetchTableRecords(String tableName, String startDate, String endDate, int offset,
                                                     int limit) {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName)
      .setExcludeReferenceLink(true)
      .setDisplayValue(conf.getValueType())
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
        LOG.error("Error - {}", getErrorMessage(apiResponse.getResponseBody()));
        return Collections.emptyList();
      }

      return parseResponseToResultListOfMap(apiResponse.getResponseBody());
    } catch (OAuthSystemException | OAuthProblemException e) {
      LOG.error("Error in fetchTableRecords", e);
      return Collections.emptyList();
    }
  }

  /**
   * Create a new record in the ServiceNow Table
   *
   * @param tableName ServiceNow Table name
   * @param entity Details of the Record to be created
   */
  public void createRecord(String tableName, HttpEntity entity) {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName);

    RestAPIResponse apiResponse = null;

    try {
      String accessToken = getAccessToken();
      requestBuilder.setAuthHeader(accessToken);
      requestBuilder.setAcceptHeader("application/json");
      requestBuilder.setContentTypeHeader("application/json");
      requestBuilder.setEntity(entity);
      apiResponse = executePost(requestBuilder.build());
      if (!apiResponse.isSuccess()) {
        LOG.error("Error - {}", getErrorMessage(apiResponse.getResponseBody()));
      } else {
        LOG.info(apiResponse.getResponseBody().toString());
      }
    } catch (OAuthSystemException | OAuthProblemException | UnsupportedEncodingException e) {
      LOG.error("Error in creating a new record", e);
      throw new RuntimeException("Error in creating a new record");
    }
  }

  /**
   * Fetches the table schema for ServiceNow table.
   *
   * @param tableName The ServiceNow table name
   * @param startDate The start date
   * @param endDate The end date
   * @param fetchRecordCount A flag that decides whether to fetch total record count or not
   * @return
   */
  public ServiceNowTableDataResponse fetchTableSchema(String tableName, String startDate, String endDate,
                                                      boolean fetchRecordCount) {
    return fetchTableSchemaUsingFirstRecord(tableName, startDate, endDate, fetchRecordCount);
  }

  private ServiceNowTableDataResponse fetchTableSchemaUsingFirstRecord(String tableName, String startDate,
                                                                       String endDate, boolean fetchRecordCount) {
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.conf.getRestApiEndpoint(), tableName)
      .setExcludeReferenceLink(true)
      .setDisplayValue(conf.getValueType())
      .setLimit(1);
    applyDateRangeToRequest(requestBuilder, startDate, endDate);

    RestAPIResponse apiResponse = null;

    try {
      String accessToken = getAccessToken();
      requestBuilder.setAuthHeader(accessToken);

      // Get the response JSON and fetch the header X-Total-Count. Set the value to recordCount
      if (fetchRecordCount) {
        requestBuilder.setResponseHeaders(HEADER_NAME_TOTAL_COUNT);
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
    String headerValue = apiResponse.getHeaders().get(HEADER_NAME_TOTAL_COUNT);
    return Strings.isNullOrEmpty(headerValue) ? 0 : Integer.parseInt(headerValue);
  }

  private List<Map<String, Object>> parseResponseToResultListOfMap(String responseBody) {
    Gson gson = new Gson();
    JsonObject jo = gson.fromJson(responseBody, JsonObject.class);
    JsonArray ja = jo.getAsJsonArray("result");

    Type type = new TypeToken<List<Map<String, Object>>>() {
    }.getType();

    return gson.fromJson(ja, type);
  }

  private String getErrorMessage(String responseBody) {
    try {
      Gson gson = new Gson();
      JsonObject jo = gson.fromJson(responseBody, JsonObject.class);
      return jo.getAsJsonObject("error").get("message").getAsString();
    } catch (Exception e) {
      return e.getMessage();
    }
  }
}
