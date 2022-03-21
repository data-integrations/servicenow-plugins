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
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.source.util.Util;

import java.util.Set;
import javax.annotation.Nullable;

/**
 * Configuration for the {@link ServiceNowMultiSource}.
 */
public class ServiceNowMultiSourceConfig extends ServiceNowBaseSourceConfig {

  @Name(ServiceNowConstants.PROPERTY_TABLE_NAMES)
  @Macro
  @Description("The names of the ServiceNow tables from which data is fetched")
  private String tableNames;

  /**
   * Constructor for ServiceNowSourceConfig object.
   *
   * @param referenceName The reference name
   * @param tableNameField The field name to hold the table name value
   * @param clientId The Client Id for ServiceNow
   * @param clientSecret The Client Secret for ServiceNow
   * @param restApiEndpoint The rest API endpoint for ServiceNow
   * @param user The user id for ServiceNow
   * @param password The password for ServiceNow
   * @param valueType The value type
   * @param startDate The start date
   * @param endDate The end date
   */
  public ServiceNowMultiSourceConfig(String referenceName, String tableNameField, String clientId,
                                     String clientSecret, String restApiEndpoint, String user, String password,
                                     String valueType, @Nullable String startDate, @Nullable String endDate,
                                     String tableNames) {
    super(referenceName, tableNameField, clientId, clientSecret, restApiEndpoint, user, password, valueType, startDate,
      endDate);
    this.tableNames = tableNames;
  }

  @Nullable
  public String getTableNames() {
    return tableNames;
  }

  /**
   * Validates {@link ServiceNowMultiSourceConfig} instance.
   */
  public void validate(FailureCollector collector) {
    super.validate(collector);
    validateTableNames(collector);
    validateTableNameField(collector);
  }

  private void validateTableNameField(FailureCollector collector) {
    if (containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD)) {
      return;
    }

    if (Util.isNullOrEmpty(tableNameField)) {
      collector.addFailure("Table name field must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD);
    }
  }

  @VisibleForTesting
  void validateTableNames(FailureCollector collector) {
    if (containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAMES)) {
      return;
    }

    if (!shouldConnect()) {
      return;
    }

    if (Util.isNullOrEmpty(tableNames)) {
      collector.addFailure("Table names must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAMES);
    } else {
      Set<String> tableNames = ServiceNowMultiInputFormat.getList(getTableNames());
      for (String tableName : tableNames) {
        validateTable(tableName, collector);
      }
    }
  }

}
