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

package io.cdap.plugin.servicenow.source;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.source.util.SourceApplication;
import io.cdap.plugin.servicenow.source.util.SourceQueryMode;
import io.cdap.plugin.servicenow.source.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Configuration for the {@link ServiceNowSource}.
 */
public class ServiceNowSourceConfig extends ServiceNowBaseSourceConfig {

  @Name(ServiceNowConstants.PROPERTY_QUERY_MODE)
  @Macro
  @Description("Mode of query. The mode can be one of two values: "
    + "`Reporting` - will allow user to choose application for which data will be fetched for all tables, "
    + "`Table` - will allow user to enter table name for which data will be fetched.")
  private String queryMode;

  @Name(ServiceNowConstants.PROPERTY_APPLICATION_NAME)
  @Macro
  @Nullable
  @Description("Application name for which data to be fetched. The application can be one of three values: " +
    "`Contract Management` - will fetch data for all tables under Contract Management application, " +
    "`Product Catalog` - will fetch data for all tables under Product Catalog application, " +
    "`Procurement` - will fetch data for all tables under Procurement application. " +
    "Note, the Application name value will be ignored if the Mode is set to `Table`.")
  private String applicationName;

  @Name(ServiceNowConstants.PROPERTY_TABLE_NAME)
  @Macro
  @Nullable
  @Description("The name of the ServiceNow table from which data to be fetched. Note, the Table name value " +
    "will be ignored if the Mode is set to `Reporting`.")
  private String tableName;


  /**
   * Constructor for ServiceNowSourceConfig object.
   *
   * @param referenceName The reference name
   * @param queryMode The query mode
   * @param applicationName The application name
   * @param tableNameField The field name to hold the table name value
   * @param tableName The table name
   * @param clientId The Client Id for ServiceNow
   * @param clientSecret The Client Secret for ServiceNow
   * @param restApiEndpoint The rest API endpoint for ServiceNow
   * @param user The user id for ServiceNow
   * @param password The password for ServiceNow
   * @param valueType The value type
   * @param startDate The start date
   * @param endDate The end date
   */
  public ServiceNowSourceConfig(String referenceName, String queryMode, @Nullable String applicationName,
                                @Nullable String tableNameField, @Nullable String tableName, String clientId,
                                String clientSecret, String restApiEndpoint, String user, String password,
                                String valueType, @Nullable String startDate, @Nullable String endDate) {
    super(referenceName, tableNameField, clientId, clientSecret, restApiEndpoint, user, password, valueType, startDate,
      endDate);
    this.referenceName = referenceName;
    this.queryMode = queryMode;
    this.applicationName = applicationName;
    this.tableName = tableName;
  }

  /**
   * Returns the query mode chosen.
   *
   * @param collector The failure collector to collect the errors
   * @return An instance of SourceQueryMode
   */
  public SourceQueryMode getQueryMode(FailureCollector collector) {
    SourceQueryMode mode = getQueryMode();
    if (mode != null) {
      return mode;
    }

    collector.addFailure("Unsupported query mode value: " + queryMode,
      String.format("Supported modes are: %s", SourceQueryMode.getSupportedModes()))
      .withConfigProperty(ServiceNowConstants.PROPERTY_QUERY_MODE);
    collector.getOrThrowException();
    return null;
  }

  /**
   * Returns the query mode chosen.
   *
   * @return An instance of SourceQueryMode
   */
  public SourceQueryMode getQueryMode() {
    Optional<SourceQueryMode> sourceQueryMode = SourceQueryMode.fromValue(queryMode);

    return sourceQueryMode.isPresent() ? sourceQueryMode.get() : null;
  }

  /**
   * Returns the application name chosen.
   *
   * @param collector The failure collector to collect the errors
   * @return An instance of SourceApplication
   */
  public SourceApplication getApplicationName(FailureCollector collector) {
    SourceApplication application = getApplicationName();
    if (application != null) {
      return application;
    }

    collector.addFailure("Unsupported application name value: " + applicationName,
      String.format("Supported applications are: %s", SourceApplication.getSupportedApplications()))
      .withConfigProperty(ServiceNowConstants.PROPERTY_APPLICATION_NAME);
    collector.getOrThrowException();
    return null;
  }

  /**
   * Returns the application name chosen.
   *
   * @return An instance of SourceApplication
   */
  @Nullable
  public SourceApplication getApplicationName() {
    Optional<SourceApplication> sourceApplication = SourceApplication.fromValue(applicationName);

    return sourceApplication.isPresent() ? sourceApplication.get() : null;
  }

  @Nullable
  public String getTableName() {
    return tableName;
  }

  /**
   * Validates {@link ServiceNowSourceConfig} instance.
   */
  public void validate(FailureCollector collector) {
    super.validate(collector);
    validateQueryMode(collector);
  }

  private void validateQueryMode(FailureCollector collector) {
    // according to query mode check if either table name/application exists or not
    if (containsMacro(ServiceNowConstants.PROPERTY_QUERY_MODE)) {
      return;
    }

    SourceQueryMode mode = getQueryMode(collector);

    if (mode == SourceQueryMode.REPORTING) {
      validateReportingQueryMode(collector);
    } else {
      validateTableQueryMode(collector);
    }
  }

  private void validateReportingQueryMode(FailureCollector collector) {
    if (!containsMacro(ServiceNowConstants.PROPERTY_APPLICATION_NAME)) {
      getApplicationName(collector);
    }

    if (containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD)) {
      return;
    }

    if (Util.isNullOrEmpty(tableNameField)) {
      collector.addFailure("Table name field must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD);
    }

  }

  private void validateTableQueryMode(FailureCollector collector) {
    if (containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME)) {
      return;
    }

    if (!shouldConnect()) {
      return;
    }

    if (Util.isNullOrEmpty(tableName)) {
      collector.addFailure("Table name must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAME);
    } else {
        validateTable(tableName, collector);
    }
  }

}
