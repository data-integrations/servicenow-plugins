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
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.source.util.SchemaBuilder;
import io.cdap.plugin.servicenow.source.util.ServiceNowConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Record reader that reads the entire contents of a ServiceNow table.
 */
public class ServiceNowMultiRecordReader extends ServiceNowBaseRecordReader {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowMultiRecordReader.class);
  private final ServiceNowMultiSourceConfig multiSourcePluginConf;

  ServiceNowMultiRecordReader(ServiceNowMultiSourceConfig multiSourcePluginConf) {
    super();
    this.multiSourcePluginConf = multiSourcePluginConf;
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
      throw new IOException("Exception in nextKeyValue", e);
    }
    return true;
  }

  @Override
  public StructuredRecord getCurrentValue() throws IOException {
    StructuredRecord.Builder recordBuilder = StructuredRecord.builder(schema);
    recordBuilder.set(tableNameField, tableName);

    try {
      for (Schema.Field field : tableFields) {
        String fieldName = field.getName();
        Object fieldValue = convertToValue(fieldName, field.getSchema(), row);
        recordBuilder.set(fieldName, fieldValue);
      }
    } catch (Exception e) {
      throw new IOException("Error decoding row from table " + tableName, e);
    }
    return recordBuilder.build();
  }

  @VisibleForTesting
  void fetchData() {
    tableName = split.getTableName();
    tableNameField = multiSourcePluginConf.getTableNameField();

    ServiceNowTableAPIClientImpl restApi = new ServiceNowTableAPIClientImpl(multiSourcePluginConf);

    // Get the table data
    results = restApi.fetchTableRecordsRetryableMode(tableName, multiSourcePluginConf.getStartDate(),
      multiSourcePluginConf.getEndDate(), split.getOffset(), ServiceNowConstants.PAGE_SIZE);
    LOG.debug("Results size={}", results.size());
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
    Schema tempSchema = SchemaBuilder.constructSchema(tableName, response.getColumns());
    tableFields = tempSchema.getFields();
    List<Schema.Field> schemaFields = new ArrayList<>(tableFields);
    schemaFields.add(Schema.Field.of(tableNameField, Schema.of(Schema.Type.STRING)));

    schema = Schema.recordOf(tableName, schemaFields);
  }

}
