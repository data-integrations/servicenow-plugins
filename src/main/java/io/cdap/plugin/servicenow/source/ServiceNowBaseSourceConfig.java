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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.IdUtils;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIRequestBuilder;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.source.util.SourceValueType;
import io.cdap.plugin.servicenow.source.util.Util;
import org.apache.http.HttpStatus;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nullable;

/**
 * Base ServiceNow Batch Source config. Contains common configuration properties and methods.
 */
public class ServiceNowBaseSourceConfig extends PluginConfig {

  @Name("referenceName")
  @Description("This will be used to uniquely identify this source/sink for lineage, annotating metadata, etc.")
  public String referenceName;

  @Name(ServiceNowConstants.PROPERTY_CLIENT_ID)
  @Macro
  @Description(" The Client ID for ServiceNow Instance.")
  private String clientId;

  @Name(ServiceNowConstants.PROPERTY_CLIENT_SECRET)
  @Macro
  @Description("The Client Secret for ServiceNow Instance.")
  private String clientSecret;

  @Name(ServiceNowConstants.PROPERTY_API_ENDPOINT)
  @Macro
  @Description("The REST API Endpoint for ServiceNow Instance. For example, https://instance.service-now.com")
  private String restApiEndpoint;

  @Name(ServiceNowConstants.PROPERTY_USER)
  @Macro
  @Description("The user name for ServiceNow Instance.")
  private String user;

  @Name(ServiceNowConstants.PROPERTY_PASSWORD)
  @Macro
  @Description("The password for ServiceNow Instance.")
  private String password;

  @Name(ServiceNowConstants.PROPERTY_VALUE_TYPE)
  @Macro
  @Description("The type of values to be returned. The type can be one of two values: "
    + "`Actual` -  will fetch the actual values from the ServiceNow tables, "
    + "`Display` - will fetch the display values from the ServiceNow tables.")
  private String valueType;

  @Name(ServiceNowConstants.PROPERTY_START_DATE)
  @Macro
  @Nullable
  @Description("The Start date to be used to filter the data. The format must be 'yyyy-MM-dd'.")
  private String startDate;

  @Name(ServiceNowConstants.PROPERTY_END_DATE)
  @Macro
  @Nullable
  @Description("The End date to be used to filter the data. The format must be 'yyyy-MM-dd'.")
  private String endDate;

  @Name(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD)
  @Macro
  @Nullable
  @Description("The name of the field that holds the table name. Must not be the name of any table column that " +
    "will be read. Defaults to `tablename`. Note, the Table name field value will be ignored if the Mode " +
    "is set to `Table`.")
  protected String tableNameField;

  public ServiceNowBaseSourceConfig(String referenceName, String tableNameField, String clientId,
                                    String clientSecret, String restApiEndpoint,
                                    String user, String password,
                                    String valueType, @Nullable String startDate, @Nullable String endDate) {

    this.referenceName = referenceName;
    this.tableNameField = tableNameField;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.restApiEndpoint = restApiEndpoint;
    this.user = user;
    this.password = password;
    this.valueType = valueType;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public String getReferenceName() {
    return referenceName;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getRestApiEndpoint() {
    return restApiEndpoint;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  @Nullable
  public String getStartDate() {
    return startDate;
  }

  @Nullable
  public String getEndDate() {
    return endDate;
  }

  public String getTableNameField() {
    return Strings.isNullOrEmpty(tableNameField) ? ServiceNowConstants.TABLE_NAME_FIELD_DEFAULT : tableNameField;
  }

  /**
   * Validates {@link ServiceNowSourceConfig} instance.
   */
  public void validate(FailureCollector collector) {
    // Validates the given referenceName to consists of characters allowed to represent a dataset.
    IdUtils.validateReferenceName(referenceName, collector);

    validateCredentials(collector);
    validateValueType(collector);
    validateDateRange(collector);
  }

  public void validateCredentials(FailureCollector collector) {
    if (!shouldConnect()) {
      return;
    }

    if (Util.isNullOrEmpty(clientId)) {
      collector.addFailure("Client ID must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_ID);
    }

    if (Util.isNullOrEmpty(clientSecret)) {
      collector.addFailure("Client Secret must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_SECRET);
    }

    if (Util.isNullOrEmpty(restApiEndpoint)) {
      collector.addFailure("API Endpoint must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_API_ENDPOINT);
    }

    if (Util.isNullOrEmpty(user)) {
      collector.addFailure("User name must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_USER);
    }

    if (Util.isNullOrEmpty(password)) {
      collector.addFailure("Password must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_PASSWORD);
    }

    validateServiceNowConnection(collector);
  }


  @VisibleForTesting
  void validateServiceNowConnection(FailureCollector collector) {
    try {
      ServiceNowTableAPIClientImpl restApi = new ServiceNowTableAPIClientImpl(this);
      restApi.getAccessToken();
    } catch (Exception e) {
      collector.addFailure("Unable to connect to ServiceNow Instance.",
          "Ensure properties like Client ID, Client Secret, API Endpoint, User Name, Password " +
            "are correct.")
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_ID)
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_SECRET)
        .withConfigProperty(ServiceNowConstants.PROPERTY_API_ENDPOINT)
        .withConfigProperty(ServiceNowConstants.PROPERTY_USER)
        .withConfigProperty(ServiceNowConstants.PROPERTY_PASSWORD)
        .withStacktrace(e.getStackTrace());
    }
  }

  /**
   * Returns the value type chosen.
   *
   * @param collector The failure collector to collect the errors
   * @return An instance of SourceValueType
   */

  @VisibleForTesting
  SourceValueType getValueType(FailureCollector collector) {
    SourceValueType type = getValueType();
    if (type != null) {
      return type;
    }

    collector.addFailure("Unsupported type value: " + valueType,
        String.format("Supported value types are: %s", SourceValueType.getSupportedValueTypes()))
      .withConfigProperty(ServiceNowConstants.PROPERTY_VALUE_TYPE);
    collector.getOrThrowException();
    return null;
  }

  /**
   * Returns the value type chosen.
   *
   * @return An instance of SourceValueType
   */
  @Nullable
  public SourceValueType getValueType() {
    return SourceValueType.fromValue(valueType).orElse(null);
  }

  private void validateValueType(FailureCollector collector) {
    if (containsMacro(ServiceNowConstants.PROPERTY_VALUE_TYPE)) {
      return;
    }

    getValueType(collector);
  }

  private void validateDateRange(FailureCollector collector) {
    if (containsMacro(ServiceNowConstants.PROPERTY_START_DATE) ||
      containsMacro(ServiceNowConstants.PROPERTY_END_DATE)) {
      return;
    }

    if (!Util.isNullOrEmpty(startDate) && !Util.isNullOrEmpty(endDate) &&
      !Util.isValidDateFormat(ServiceNowConstants.DATE_FORMAT, startDate) &&
      !Util.isValidDateFormat(ServiceNowConstants.DATE_FORMAT, endDate)) {
      collector.addFailure("Invalid format for Start date. Correct Format: " +
          ServiceNowConstants.DATE_FORMAT, null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_START_DATE);
      collector.addFailure("Invalid format for End date. Correct Format:" +
          ServiceNowConstants.DATE_FORMAT, null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_END_DATE);
      return;
    }
    // validate the date formats for both start date & end date
    if (!Util.isNullOrEmpty(startDate) && !Util.isValidDateFormat(ServiceNowConstants.DATE_FORMAT, startDate)) {
      collector.addFailure("Invalid format for Start date. Correct Format: " +
          ServiceNowConstants.DATE_FORMAT, null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_START_DATE);
      return;
    }

    if (!Util.isNullOrEmpty(endDate) && !Util.isValidDateFormat(ServiceNowConstants.DATE_FORMAT, endDate)) {
      collector.addFailure("Invalid format for End date. Correct Format:" +
          ServiceNowConstants.DATE_FORMAT, null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_END_DATE);
      return;
    }

    if (Util.isNullOrEmpty(startDate) || Util.isNullOrEmpty(endDate)) {
      return;
    }

    // validate the date range by checking if start date is smaller than end date
    LocalDate fromDate = LocalDate.parse(startDate);
    LocalDate toDate = LocalDate.parse(endDate);
    long noOfDays = ChronoUnit.DAYS.between(fromDate, toDate);

    if (noOfDays < 0) {
      collector.addFailure("End date must be greater than Start date.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_START_DATE)
        .withConfigProperty(ServiceNowConstants.PROPERTY_END_DATE);
    }
  }

  /**
   * Returns true if ServiceNow can be connected to.
   */
  public boolean shouldConnect() {
    return !containsMacro(ServiceNowConstants.PROPERTY_CLIENT_ID) &&
      !containsMacro(ServiceNowConstants.PROPERTY_CLIENT_SECRET) &&
      !containsMacro(ServiceNowConstants.PROPERTY_API_ENDPOINT) &&
      !containsMacro(ServiceNowConstants.PROPERTY_USER) &&
      !containsMacro(ServiceNowConstants.PROPERTY_PASSWORD);
  }

  public boolean shouldGetSchema() {
    return !containsMacro(ServiceNowConstants.PROPERTY_QUERY_MODE)
      && !containsMacro(ServiceNowConstants.PROPERTY_APPLICATION_NAME)
      && !containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD)
      && !containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME)
      && !containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAMES)
      && shouldConnect()
      && !containsMacro(ServiceNowConstants.PROPERTY_VALUE_TYPE);
  }

  void validateTable(String tableName, FailureCollector collector) {
    // Call API to fetch first record from the table
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.getRestApiEndpoint(), tableName)
      .setExcludeReferenceLink(true)
      .setDisplayValue(this.getValueType())
      .setLimit(1);

    RestAPIResponse apiResponse = null;
    ServiceNowTableAPIClientImpl serviceNowTableAPIClient = new ServiceNowTableAPIClientImpl(this);
    try {
      String accessToken = serviceNowTableAPIClient.getAccessToken();
      requestBuilder.setAuthHeader(accessToken);

      // Get the response JSON and fetch the header X-Total-Count. Set the value to recordCount
      requestBuilder.setResponseHeaders(ServiceNowConstants.HEADER_NAME_TOTAL_COUNT);

      apiResponse = serviceNowTableAPIClient.executeGet(requestBuilder.build());
      if (!apiResponse.isSuccess()) {
        if (apiResponse.getHttpStatus() == HttpStatus.SC_BAD_REQUEST) {
          collector.addFailure("Bad Request. Table: " + tableName + " is invalid.", "");
        }
      } else if (serviceNowTableAPIClient.parseResponseToResultListOfMap(apiResponse.getResponseBody()).isEmpty()) {
        collector.addFailure("Table: " + tableName + " is empty.", "");
      }
    } catch (OAuthSystemException | OAuthProblemException e) {
      collector.addFailure("Unable to connect to ServiceNow Instance.",
                           "Ensure properties like Client ID, Client Secret, API Endpoint, User Name, Password " +
                             "are correct.")
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_ID)
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_SECRET)
        .withConfigProperty(ServiceNowConstants.PROPERTY_API_ENDPOINT)
        .withConfigProperty(ServiceNowConstants.PROPERTY_USER)
        .withConfigProperty(ServiceNowConstants.PROPERTY_PASSWORD);
    }
  }

}
