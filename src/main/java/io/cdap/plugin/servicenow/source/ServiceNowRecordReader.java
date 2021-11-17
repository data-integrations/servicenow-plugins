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

import com.google.common.base.Strings;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.source.util.SchemaBuilder;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Record reader that reads the entire contents of a ServiceNow table.
 */
public class ServiceNowRecordReader extends RecordReader<NullWritable, StructuredRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowRecordReader.class);
  private final ServiceNowSourceConfig pluginConf;
  private ServiceNowInputSplit split;
  private int pos;
  private List<Schema.Field> tableFields;
  private Schema schema;

  private String tableName;
  private List<Map<String, Object>> results;
  private Iterator<Map<String, Object>> iterator;
  private Map<String, Object> row;

  ServiceNowRecordReader(ServiceNowSourceConfig pluginConf) {
    this.pluginConf = pluginConf;
  }

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context) {
    this.split = (ServiceNowInputSplit) split;
    this.pos = 0;
  }

  @Override
  public boolean nextKeyValue() throws IOException {
    try {
      if (results == null) {
        fetchData();
      }

      if (!iterator.hasNext()) {
        return false;
      }

      row = iterator.next();

      pos++;
    } catch (Exception e) {
      LOG.error("Error in nextKeyValue", e);
      throw new IOException("Exception in nextKeyValue", e);
    }
    return true;
  }

  @Override
  public NullWritable getCurrentKey() {
    return NullWritable.get();
  }

  @Override
  public StructuredRecord getCurrentValue() throws IOException {
    StructuredRecord.Builder recordBuilder = StructuredRecord.builder(schema);

    try {
      for (Schema.Field field : tableFields) {
        String fieldName = field.getName();
        Object fieldValue = convertToValue(fieldName, field.getSchema(), row);
        recordBuilder.set(fieldName, fieldValue);
      }
    } catch (Exception e) {
      LOG.error("Error decoding row from table " + tableName, e);
      throw new IOException("Error decoding row from table " + tableName, e);
    }
    return recordBuilder.build();
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    return pos / (float) split.getLength();
  }

  @Override
  public void close() throws IOException {
  }

  private void fetchData() {
    tableName = split.getTableName();

    ServiceNowTableAPIClientImpl restApi = new ServiceNowTableAPIClientImpl(pluginConf);

    // Get the table data
    results = restApi.fetchTableRecords(tableName, pluginConf.getStartDate(), pluginConf.getEndDate(),
      split.getOffset(), ServiceNowConstants.PAGE_SIZE);

    LOG.debug("size={}", results.size());
    if (!results.isEmpty()) {
      fetchSchema(restApi);
    }

    iterator = results.iterator();
  }

  private void fetchSchema(ServiceNowTableAPIClientImpl restApi) {
    // Fetch the column definition
    ServiceNowTableDataResponse response = restApi.fetchTableSchema(tableName, null, null,
      false);
    if (response == null) {
      return;
    }

    // Build schema
    SchemaBuilder schemaBuilder = new SchemaBuilder();
    Schema tempSchema = schemaBuilder.constructSchema(tableName, response.getColumns());
    tableFields = tempSchema.getFields();
    List<Schema.Field> schemaFields = new ArrayList<>(tableFields);

    schema = Schema.recordOf(tableName, schemaFields);
  }

  private Object convertToValue(String fieldName, Schema fieldSchema, Map<String, Object> record) {
    Schema.Type fieldType = fieldSchema.getType();
    Object fieldValue = record.get(fieldName);

    switch (fieldType) {
      case STRING:
        return convertToStringValue(fieldValue);
      case DOUBLE:
        return convertToDoubleValue(fieldValue);
      case INT:
        return convertToIntegerValue(fieldValue);
      case BOOLEAN:
        return convertToBooleanValue(fieldValue);
      case UNION:
        if (fieldSchema.isNullable()) {
          return convertToValue(fieldName, fieldSchema.getNonNullable(), record);
        }
        throw new IllegalStateException(
          String.format("Field '%s' is of unexpected type '%s'. Declared 'complex UNION' types: %s",
            fieldName, record.get(fieldName).getClass().getSimpleName(), fieldSchema.getUnionSchemas()));
      default:
        throw new IllegalStateException(
          String.format("Record type '%s' is not supported for field '%s'", fieldType.name(), fieldName));
    }
  }

  private String convertToStringValue(Object fieldValue) {
    return String.valueOf(fieldValue);
  }

  private Double convertToDoubleValue(Object fieldValue) {
    if (fieldValue instanceof String && Strings.isNullOrEmpty(String.valueOf(fieldValue))) {
      return null;
    }

    return Double.parseDouble(String.valueOf(fieldValue));
  }

  private Integer convertToIntegerValue(Object fieldValue) {
    if (fieldValue instanceof String && Strings.isNullOrEmpty(String.valueOf(fieldValue))) {
      return null;
    }

    return Integer.parseInt(String.valueOf(fieldValue));
  }

  private Boolean convertToBooleanValue(Object fieldValue) {
    if (fieldValue instanceof String && Strings.isNullOrEmpty(String.valueOf(fieldValue))) {
      return null;
    }

    return Boolean.parseBoolean(String.valueOf(fieldValue));
  }
}
