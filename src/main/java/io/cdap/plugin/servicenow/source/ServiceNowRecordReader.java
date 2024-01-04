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
import io.cdap.plugin.servicenow.connector.ServiceNowRecordConverter;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.SourceQueryMode;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
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
  private ServiceNowTableAPIClientImpl restApi;

  public ServiceNowRecordReader(ServiceNowSourceConfig pluginConf) {
    super();
    this.pluginConf = pluginConf;
  }

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context) {
    this.split = (ServiceNowInputSplit) split;
    this.pos = 0;
    restApi = new ServiceNowTableAPIClientImpl(pluginConf.getConnection());
    tableName = ((ServiceNowInputSplit) split).getTableName();
    tableNameField = pluginConf.getTableNameField();
    fetchSchema(restApi);
  }

  /**
   * Initialize with only the provided split.
   * This method should not be called directly from the code,
   * as Hadoop runtime initialize internally during execution.
   *
   * @param split Split to read by the current reader.
   */
  public void initialize(InputSplit split) {
    initialize(split, null);
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
        ServiceNowRecordConverter.convertToValue(fieldName, field.getSchema(), row, recordBuilder);
      }
    } catch (Exception e) {
      LOG.error("Error decoding row from table " + tableName, e);
      throw new IOException("Error decoding row from table " + tableName, e);
    }
    return recordBuilder.build();
  }

  private void fetchData() throws IOException {
    // Get the table data
    results = restApi.fetchTableRecordsRetryableMode(tableName, pluginConf.getValueType(), pluginConf.getStartDate(),
                                                     pluginConf.getEndDate(), split.getOffset(),
                                                     pluginConf.getPageSize());
    LOG.debug("Results size={}", results.size());

    iterator = results.iterator();
  }

  private void fetchSchema(ServiceNowTableAPIClientImpl restApi) {
    try {
      Schema tempSchema = restApi.fetchTableSchema(tableName);
      tableFields = tempSchema.getFields();
      List<Schema.Field> schemaFields = new ArrayList<>(tableFields);

      if (pluginConf.getQueryMode() == SourceQueryMode.REPORTING) {
        schemaFields.add(Schema.Field.of(tableNameField, Schema.of(Schema.Type.STRING)));
      }

      schema = Schema.recordOf(tableName, schemaFields);
    } catch (OAuthProblemException | OAuthSystemException | IOException e) {
      throw new RuntimeException(e);
    }
  }

}
