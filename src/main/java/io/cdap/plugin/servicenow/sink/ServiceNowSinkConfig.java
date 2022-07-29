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

import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.servicenow.ServiceNowBaseConfig;
import io.cdap.plugin.servicenow.ServiceNowConstants;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.source.util.SchemaBuilder;
import io.cdap.plugin.servicenow.source.util.ServiceNowColumn;
import io.cdap.plugin.servicenow.source.util.ServiceNowTableInfo;
import io.cdap.plugin.servicenow.source.util.SourceValueType;
import io.cdap.plugin.servicenow.source.util.Util;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Configuration for the {@link ServiceNowSink}.
 */
public class ServiceNowSinkConfig extends ServiceNowBaseConfig {

  private static final String SERVICENOW_ID_FIELD = "sys_id";

  public static final String PROPERTY_EXTERNAL_ID_FIELD = "externalIdField";

  @Name(ServiceNowConstants.PROPERTY_TABLE_NAME)
  @Macro
  @Description("The name of the ServiceNow table to which data needs to be inserted.")
  private String tableName;

  @Name(ServiceNowConstants.PROPERTY_OPERATION)
  @Macro
  @Description("The type of the operation to be performed in ServiceNow tables. Note, for Update operation, sys_id " +
    "must be present.")
  private String operation;

  @Name(ServiceNowConstants.PROPERTY_MAX_RECORDS_PER_BATCH)
  @Macro
  @Description("No. of requests that will be sent to ServiceNow Batch API as a payload. Default value is 200. Rest " +
    "API property in Transaction quota section \"REST Batch API request timeout\" should be increased to use higher " +
    "records in a batch. By default this property has a value of 30 sec which can handle approximately 200 records " +
    "in a batch. To use a bigger batch size, set it to a higher value. ")
  private Long maxRecordsPerBatch;

  @Name(ServiceNowConstants.NAME_SCHEMA)
  @Macro
  @Nullable
  @Description("The schema of the table to read.")
  private String schema; 

  /**
   * Constructor for ServiceNowSourceConfig object.
   *
   * @param referenceName The reference name
   * @param clientId The Client Id for ServiceNow
   * @param clientSecret The Client Secret for ServiceNow
   * @param restApiEndpoint The rest API endpoint for ServiceNow
   * @param user The user id for ServiceNow
   * @param password The password for ServiceNow
   * @param tableName The table name
   * @param operation The type of operation to be performed
   * @param maxRecordsPerBatch The maximum number of records per batch
   */
  public ServiceNowSinkConfig(String referenceName, String clientId, String clientSecret, String restApiEndpoint,
                              String user, String password, String tableName,
                              String operation, Long maxRecordsPerBatch) {
    super(referenceName, clientId, clientSecret, restApiEndpoint, user, password);
    this.tableName = tableName;
    this.operation = operation;
    this.maxRecordsPerBatch = maxRecordsPerBatch;
  }

  @Nullable
  public String getTableName() {
    return tableName;
  }

  public String getOperation() {
    return operation;
  }

  public Long getMaxRecordsPerBatch() {
    return maxRecordsPerBatch;
  }
    
  /**
   * Validates {@link ServiceNowSinkConfig} instance.
   */
  public void validate(FailureCollector collector) {
    super.validate(collector);
    validateMaxRecordsPerBatch(collector);
    validateTable(collector);
  }

  private void validateMaxRecordsPerBatch(FailureCollector collector) {
    if (containsMacro(ServiceNowConstants.PROPERTY_MAX_RECORDS_PER_BATCH)) {
      return;
    }

    if (Objects.isNull(maxRecordsPerBatch)) {
      collector.addFailure("Max records per batch must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_MAX_RECORDS_PER_BATCH);
    } else if (maxRecordsPerBatch > 500 || maxRecordsPerBatch < 200) {
      collector.addFailure("Max records per batch must not be greater than 500 or less than 200.",
                           null).withConfigProperty(ServiceNowConstants.PROPERTY_MAX_RECORDS_PER_BATCH);
    }
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
    } else {
        validateTable(tableName, SourceValueType.SHOW_DISPLAY_VALUE, collector);
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

  public void validateSchema(Schema schema, FailureCollector collector) {
    List<Schema.Field> fields = schema.getFields();
    if (fields == null || fields.isEmpty()) {
      collector.addFailure("Sink schema must contain at least one field", null);
      throw collector.getOrThrowException();
    }

    if (!shouldConnect() || containsMacro(ServiceNowConstants.PROPERTY_TABLE_NAME)
      || containsMacro(ServiceNowConstants.PROPERTY_OPERATION) || containsMacro(PROPERTY_EXTERNAL_ID_FIELD)) {
      return;
    }

    Schema tableSchema = getTableMetaData(tableName, this).getSchema();
    Set<String> tableFields = getTableFields(tableSchema);

    Set<String> inputFields = schema.getFields()
      .stream()
      .map(Schema.Field::getName)
      .collect(Collectors.toSet());

    String externalIdFieldName = null;
    switch (operation) {
      case ServiceNowConstants.INSERT_OPERATION:
        break;
      case ServiceNowConstants.UPDATE_OPERATION:
        externalIdFieldName = SERVICENOW_ID_FIELD;
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
    validateInputSchema(schema, collector);
  }

  @Nullable
  private ServiceNowTableInfo getTableMetaData(String tableName, ServiceNowSinkConfig conf) {
    // Call API to fetch first record from the table
    ServiceNowTableAPIClientImpl restApi = new ServiceNowTableAPIClientImpl(conf);
    ServiceNowTableDataResponse response = restApi.fetchTableSchema(tableName, SourceValueType.SHOW_DISPLAY_VALUE,
      null, null, true);
    if (response == null) {
      return null;
    }
    List<ServiceNowColumn> columns = response.getColumns();
    if (columns == null || columns.isEmpty()) {
      return null;
    }
    Schema schema = SchemaBuilder.constructSchema(tableName, columns);
    return new ServiceNowTableInfo(tableName, schema, response.getTotalRecordCount());
  }

  private Set<String> getTableFields(Schema schema) {
    Set<String> tableFields = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    for (Schema.Field field : schema.getFields()) {
      tableFields.add(field.getName());
    }
    return tableFields;
  }

  /**
   * Checks that input schema is correct. Which means:
   * 1. All fields in it are present in table
   * 2. Field types are in accordance with the actual types in table.
   *
   * @param schema input schema to check
   */
  private void validateInputSchema(Schema schema, FailureCollector collector) {
    Schema tableActualSchema = getTableMetaData(tableName, this).getSchema();
    checkCompatibility(tableActualSchema, schema, collector);
  }

  /**
   * Checks two schemas compatibility based on the following rules:
   * <ul>
   *   <li>Actual schema must have fields indicated in the provided schema.</li>
   *   <li>Fields types in both schema must match.</li>
   * </ul>
   *
   * @param actualSchema schema calculated based on ServiceNow metadata information
   * @param providedSchema schema provided in the configuration
   */
  private void checkCompatibility(Schema actualSchema, Schema providedSchema, FailureCollector collector) {
    for (Schema.Field providedField : Objects.requireNonNull(providedSchema.getFields())) {
      Schema.Field actualField = actualSchema.getField(providedField.getName(), true);
      if (actualField == null) {
        collector.addFailure(
            String.format("Field '%s' does not exist in ServiceNow", providedField.getName()), null)
          .withInputSchemaField(providedField.getName());
      }

      Schema providedFieldSchema = providedField.getSchema();
      Schema actualFieldSchema = actualField.getSchema();

      boolean isActualFieldNullable = actualFieldSchema.isNullable();
      boolean isProvidedFieldNullable = providedFieldSchema.isNullable();

      actualFieldSchema = isActualFieldNullable ? actualFieldSchema.getNonNullable() : actualFieldSchema;
      providedFieldSchema = isProvidedFieldNullable ? providedFieldSchema.getNonNullable() : providedFieldSchema;

      if (!actualFieldSchema.equals(providedFieldSchema)
        || !Objects.equals(actualFieldSchema.getLogicalType(), providedFieldSchema.getLogicalType())) {
        collector.addFailure(
            String.format("Expected field '%s' to be of '%s', but it is of '%s'",
                          providedField.getName(), providedFieldSchema, actualFieldSchema), null)
          .withInputSchemaField(providedField.getName());
      }

    }
  }
  
}
