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

package io.cdap.plugin.servicenow.sink;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.servicenow.ServiceNowBaseConfig;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.Util;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Configuration for the {@link ServiceNowSink}.
 */
public class ServiceNowSinkConfig extends ServiceNowBaseConfig {

  public static final String PROPERTY_EXTERNAL_ID_FIELD = "externalIdField";

  @Name(ServiceNowConstants.PROPERTY_TABLE_NAME)
  @Macro
  @Description("The name of the ServiceNow table to which data needs to be inserted.")
  private final String tableName;

  @Name(ServiceNowConstants.PROPERTY_OPERATION)
  @Macro
  @Description("The type of the operation to be performed in ServiceNow tables. Note, for Update operation, sys_id " +
    "must be present.")
  private final String operation;

  @Name(ServiceNowConstants.NAME_SCHEMA)
  @Macro
  @Nullable
  @Description("The schema of the table to read.")
  private String schema;

  /**
   * Constructor for ServiceNowSinkConfig object.
   *
   * @param referenceName   The reference name
   * @param clientId        The Client Id for ServiceNow
   * @param clientSecret    The Client Secret for ServiceNow
   * @param restApiEndpoint The rest API endpoint for ServiceNow
   * @param user            The user id for ServiceNow
   * @param password        The password for ServiceNow
   * @param tableName       The table name
   * @param operation       The type of operation to be performed
   */
  public ServiceNowSinkConfig(String referenceName, String clientId, String clientSecret, String restApiEndpoint,
                              String user, String password, String tableName, String operation) {
    super(referenceName, clientId, clientSecret, restApiEndpoint, user, password);
    this.tableName = tableName;
    this.operation = operation;
  }

  @Nullable
  public String getTableName() {
    return tableName;
  }

  public String getOperation() {
    return operation;
  }

  /**
   * Validates {@link ServiceNowSinkConfig} instance.
   */
  public void validate(FailureCollector collector) {
    super.validate(collector);
    validateTable(collector);
  }

  private void validateTable(FailureCollector collector) {
    if (containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME)) {
      return;
    }

    if (!shouldConnect()) {
      return;
    }

    if (Util.isNullOrEmpty(tableName)) {
      collector.addFailure("Table name must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_TABLE_NAME);
    }
  }

  /**
   * @return the schema of the table
   */
  @Nullable
  public Schema getSchema(FailureCollector collector) {
    try {
      return Strings.isNullOrEmpty(schema) ? null : Schema.parseJson(schema);
    } catch (IOException e) {
      collector.addFailure("Invalid schema: " + e.getMessage(), null)
        .withConfigProperty(ServiceNowConstants.NAME_SCHEMA);
    }
    // if there was an error that was added, it will throw an exception, otherwise, this statement will not be executed
    throw collector.getOrThrowException();
  }

  void validateSchema(Schema schema, FailureCollector collector) {
    List<Schema.Field> fields = schema.getFields();
    if (fields == null || fields.isEmpty()) {
      collector.addFailure("Sink schema must contain at least one field", null);
      throw collector.getOrThrowException();
    }

    if (!shouldConnect() || containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME)
      || containsMacro(ServiceNowConstants.PROPERTY_OPERATION) || containsMacro(PROPERTY_EXTERNAL_ID_FIELD)) {
      return;
    }
    ServiceNowTableAPIClientImpl restApi = new ServiceNowTableAPIClientImpl(this);
    Schema tableSchema = restApi.fetchServiceNowTableSchema(tableName, collector);
    if (tableSchema == null) {
      throw collector.getOrThrowException();
    }
    Set<String> tableFields = getTableFields(tableSchema);

    Set<String> inputFields = getTableFields(schema);

    String externalIdFieldName = null;
    switch (operation) {
      case ServiceNowConstants.INSERT_OPERATION:
        break;
      case ServiceNowConstants.UPDATE_OPERATION:
        externalIdFieldName = ServiceNowConstants.SYS_ID;
        break;
      default:
        collector.addFailure("Unsupported value for operation: " + operation, null)
          .withConfigProperty(ServiceNowConstants.PROPERTY_OPERATION);
    }

    if (externalIdFieldName != null && !inputFields.remove(externalIdFieldName)) {
      collector.addFailure(String.format("Schema must contain external id field '%s'", externalIdFieldName), null)
        .withConfigProperty(ServiceNowSinkConfig.PROPERTY_EXTERNAL_ID_FIELD);
    }
    inputFields.removeAll(tableFields);

    if (!inputFields.isEmpty()) {
      for (String inputField : inputFields) {
        collector.addFailure(
            String.format("Field '%s' is not present or not creatable in target ServiceNow table.", inputField), null)
          .withInputSchemaField(inputField);
      }
    }
    checkCompatibility(tableSchema, schema, collector);
  }

  private Set<String> getTableFields(Schema schema) {
    return schema.getFields().stream().map(field -> field.getName()).collect(Collectors.toSet());
  }

  /**
   * Checks two schemas compatibility based on the following rules:
   * <ul>
   *   <li>Actual schema must have fields indicated in the provided schema.</li>
   *   <li>Fields types in both schema must match.</li>
   * </ul>
   *
   * @param actualSchema   schema calculated based on ServiceNow metadata information
   * @param providedSchema schema provided in the configuration
   */
  @VisibleForTesting
  void checkCompatibility(Schema actualSchema, Schema providedSchema, FailureCollector collector) {
    for (Schema.Field providedField : Objects.requireNonNull(providedSchema.getFields())) {
      Schema.Field actualField = actualSchema.getField(providedField.getName(), true);
      if (actualField == null) {
        collector.addFailure(
            String.format("Field '%s' does not exist in ServiceNow", providedField.getName()), null)
          .withInputSchemaField(providedField.getName());
        continue;
      }
      Schema providedFieldSchema = providedField.getSchema();
      Schema actualFieldSchema = actualField.getSchema();

      boolean isActualFieldNullable = actualFieldSchema.isNullable();
      boolean isProvidedFieldNullable = providedFieldSchema.isNullable();

      actualFieldSchema = isActualFieldNullable ? actualFieldSchema.getNonNullable() : actualFieldSchema;
      providedFieldSchema = isProvidedFieldNullable ? providedFieldSchema.getNonNullable() : providedFieldSchema;

      providedFieldSchema = convertToServiceNowCompatibleDataTypes(providedFieldSchema);

      if (!actualFieldSchema.isCompatible(providedFieldSchema)
        || !Objects.equals(actualFieldSchema.getLogicalType(), providedFieldSchema.getLogicalType())) {
        collector.addFailure(
            String.format("Expected field '%s' to be of '%s', but it is of '%s'",
              providedField.getName(), providedFieldSchema, actualFieldSchema), null)
          .withInputSchemaField(providedField.getName());
      }
    }
    collector.getOrThrowException();
  }

  /**
   * Converts to ServiceNow compatible data types
   *
   * @param providedFieldSchema
   * @return
   */
  private Schema convertToServiceNowCompatibleDataTypes(Schema providedFieldSchema) {
    switch (providedFieldSchema.getType()) {
      case FLOAT:
      case DOUBLE:
        providedFieldSchema = Schema.decimalOf(ServiceNowConstants.DEFAULT_PRECISION,
                                               ServiceNowConstants.DEFAULT_SCALE);
    }

    if (providedFieldSchema.getLogicalType() != null) {
      switch (providedFieldSchema.getLogicalType()) {
        case TIMESTAMP_MICROS:
        case TIMESTAMP_MILLIS:
          providedFieldSchema = Schema.of(Schema.LogicalType.DATETIME);
          break;
        case TIME_MICROS:
        case TIME_MILLIS:
          providedFieldSchema = Schema.of(Schema.LogicalType.TIME_MICROS);
      }
    }

    return providedFieldSchema;
  }

}
