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

package io.cdap.plugin.servicenow;

import com.google.common.annotations.VisibleForTesting;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.IdUtils;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIRequestBuilder;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.source.ServiceNowSourceConfig;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceValueType;
import io.cdap.plugin.servicenow.util.Util;
import org.apache.http.HttpStatus;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

/**
 * ServiceNow Base Config. Contains connection properties and methods.
 */
public class ServiceNowBaseConfig extends PluginConfig {

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

  public ServiceNowBaseConfig(String referenceName, String clientId, String clientSecret, String restApiEndpoint,
                              String user, String password) {

    this.referenceName = referenceName;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.restApiEndpoint = restApiEndpoint;
    this.user = user;
    this.password = password;
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

  /**
   * Validates {@link ServiceNowSourceConfig} instance.
   */
  public void validate(FailureCollector collector) {
    // Validates the given referenceName to consists of characters allowed to represent a dataset.
    IdUtils.validateReferenceName(referenceName, collector);
    validateCredentials(collector);
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
  public void validateServiceNowConnection(FailureCollector collector) {
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

  public void validateTable(String tableName, SourceValueType valueType, FailureCollector collector) {
    // Call API to fetch first record from the table
    ServiceNowTableAPIRequestBuilder requestBuilder = new ServiceNowTableAPIRequestBuilder(
      this.getRestApiEndpoint(), tableName, false)
      .setExcludeReferenceLink(true)
      .setDisplayValue(valueType)
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
          collector.addFailure("Bad Request. Table: " + tableName + " is invalid.", "")
            .withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAME);
        }
      } else if (serviceNowTableAPIClient.parseResponseToResultListOfMap(apiResponse.getResponseBody()).isEmpty()) {
        collector.addFailure("Table: " + tableName + " is empty.", "")
          .withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAME);
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
