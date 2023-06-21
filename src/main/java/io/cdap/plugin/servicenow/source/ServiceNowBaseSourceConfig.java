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
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.IdUtils;
import io.cdap.plugin.servicenow.ServiceNowBaseConfig;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceValueType;
import io.cdap.plugin.servicenow.util.Util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;

/**
 * Base ServiceNow Batch Source config. Contains common configuration properties and methods.
 */
public class ServiceNowBaseSourceConfig extends ServiceNowBaseConfig {
  @Name("referenceName")
  @Description("This will be used to uniquely identify this source for lineage, annotating metadata, etc.")
  public String referenceName;

  @Name(ServiceNowConstants.PROPERTY_VALUE_TYPE)
  @Macro
  @Description("The type of values to be returned."
    + "`Actual` -  will fetch the actual values from the ServiceNow tables"
    + "`Display` - will fetch the display values from the ServiceNow tables."
    + "Default is Actual.")
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

  /**
   * Constructor for ServiceNowSourceConfig object.
   *
   * @param referenceName   The reference name
   * @param clientId        The Client Id for ServiceNow
   * @param clientSecret    The Client Secret for ServiceNow
   * @param restApiEndpoint The rest API endpoint for ServiceNow
   * @param user            The user id for ServiceNow
   * @param password        The password for ServiceNow
   * @param tableNameField  The field name to hold the table name value
   * @param valueType       The value type
   * @param startDate       The start date
   * @param endDate         The end date
   */
  public ServiceNowBaseSourceConfig(String referenceName, String clientId, String clientSecret, String restApiEndpoint,
                                    String user, String password, String tableNameField, String valueType,
                                    @Nullable String startDate, @Nullable String endDate) {
    super(clientId, clientSecret, restApiEndpoint, user, password);
    this.referenceName = referenceName;
    this.tableNameField = tableNameField;
    this.valueType = valueType;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public String getReferenceName() {
    return referenceName;
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
   * Validates {@link ServiceNowBaseSourceConfig} instance.
   */
  public void validate(FailureCollector collector) {
    super.validate(collector);
    IdUtils.validateReferenceName(referenceName, collector);
    validateValueType(collector);
    validateDateRange(collector);
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
      containsMacro(ServiceNowConstants.PROPERTY_END_DATE) ||
      (Util.isNullOrEmpty(startDate) && Util.isNullOrEmpty(endDate))) {
      return;
    }

    if (Util.isNullOrEmpty(startDate) || Util.isNullOrEmpty(endDate)) {
      collector.addFailure("Enter values for both Start date and End date.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_START_DATE)
        .withConfigProperty(ServiceNowConstants.PROPERTY_END_DATE);
      return;
    }

    // validate the date formats for both start date & end date
    if (!Util.isNullOrEmpty(startDate) && !Util.isValidDateFormat(ServiceNowConstants.DATE_FORMAT, startDate)) {
      collector.addFailure("Invalid format for Start date. Correct Format: " +
                             ServiceNowConstants.DATE_FORMAT, null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_START_DATE);
    }

    if (!Util.isNullOrEmpty(endDate) && !Util.isValidDateFormat(ServiceNowConstants.DATE_FORMAT, endDate)) {
      collector.addFailure("Invalid format for End date. Correct Format:" +
                             ServiceNowConstants.DATE_FORMAT, null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_END_DATE);
    }
    collector.getOrThrowException();

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

  public boolean shouldGetSchema() {
    return !containsMacro(ServiceNowConstants.PROPERTY_QUERY_MODE)
      && !containsMacro(ServiceNowConstants.PROPERTY_APPLICATION_NAME)
      && !containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME_FIELD)
      && !containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME)
      && !containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAMES)
      && shouldConnect()
      && !containsMacro(ServiceNowConstants.PROPERTY_VALUE_TYPE);
  }

}
