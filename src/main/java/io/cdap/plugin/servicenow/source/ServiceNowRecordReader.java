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

import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.util.SchemaBuilder;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceQueryMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Record reader that reads the entire contents of a ServiceNow table.
 */
public class ServiceNowRecordReader extends ServiceNowBaseRecordReader {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowRecordReader.class);
  private final ServiceNowSourceConfig pluginConf;

  ServiceNowRecordReader(ServiceNowSourceConfig pluginConf) {
    super();
    this.pluginConf = pluginConf;
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
  public StructuredRecord getCurrentValue() throws IOException {
    StructuredRecord.Builder recordBuilder = StructuredRecord.builder(schema);

    if (pluginConf.getQueryMode() == SourceQueryMode.REPORTING) {
      recordBuilder.set(tableNameField, tableName);
    }

    try {
      for (Schema.Field field : tableFields) {
        String fieldName = field.getName();
        Object fieldValue = pluginConf.getConnection().convertToValue(fieldName, field.getSchema(), row);
        recordBuilder.set(fieldName, fieldValue);
      }
    } catch (Exception e) {
      LOG.error("Error decoding row from table " + tableName, e);
      throw new IOException("Error decoding row from table " + tableName, e);
    }
    return recordBuilder.build();
  }

  private void fetchData() {
    tableName = split.getTableName();
    tableNameField = pluginConf.getTableNameField();

    ServiceNowTableAPIClientImpl restApi = new ServiceNowTableAPIClientImpl(pluginConf.getConnection());

    // Get the table data
    results = restApi.fetchTableRecordsRetryableMode(tableName, pluginConf.getValueType(), pluginConf.getStartDate(),
                                                     pluginConf.getEndDate(), split.getOffset(),
                                                     ServiceNowConstants.PAGE_SIZE);
    LOG.debug("Results size={}", results.size());
    if (!results.isEmpty()) {
      fetchSchema(restApi);
    }

    iterator = results.iterator();
  }

  private void fetchSchema(ServiceNowTableAPIClientImpl restApi) {
    // Fetch the column definition
    ServiceNowTableDataResponse response = restApi.fetchTableSchema(tableName, pluginConf.getValueType(), null,
                                                                    null, false);
    if (response == null) {
      return;
    }

    // Build schema
    Schema tempSchema = SchemaBuilder.constructSchema(tableName, response.getColumns());
    tableFields = tempSchema.getFields();
    List<Schema.Field> schemaFields = new ArrayList<>(tableFields);

    if (pluginConf.getQueryMode() == SourceQueryMode.REPORTING) {
      schemaFields.add(Schema.Field.of(tableNameField, Schema.of(Schema.Type.STRING)));
    }

    schema = Schema.recordOf(tableName, schemaFields);
  }

}
