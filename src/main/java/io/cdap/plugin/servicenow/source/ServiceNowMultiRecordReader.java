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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.servicenow.apiclient.ServiceNowAPIException;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.connector.ServiceNowRecordConverter;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Record reader that reads the entire contents of a ServiceNow table.
 */
public class ServiceNowMultiRecordReader extends ServiceNowBaseRecordReader {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowMultiRecordReader.class);
  private final ServiceNowMultiSourceConfig multiSourcePluginConf;
  private ServiceNowTableAPIClientImpl restApi;
  private final Gson gson = new Gson();
  private final Type mapType = new TypeToken<Map<String, String>>() { }.getType();
  private JsonReader jsonReader = null;

  ServiceNowMultiRecordReader(ServiceNowMultiSourceConfig multiSourcePluginConf) {
    super();
    this.multiSourcePluginConf = multiSourcePluginConf;
  }

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context) {
    this.split = (ServiceNowInputSplit) split;
    this.pos = 0;
    restApi = new ServiceNowTableAPIClientImpl(multiSourcePluginConf.getConnection(),
                                               multiSourcePluginConf.getUseConnection());
    tableName = ((ServiceNowInputSplit) split).getTableName();
    tableNameField = multiSourcePluginConf.getTableNameField();
    fetchSchema(restApi);
  }

  @Override
  /**
   * The refactored nextKeyValue() — uses Gson JsonReader to stream one record at a time.
   * Returns true when it assigned `row` to the next record.
   * Returns false only when there are no more pages/records (i.e., openNextPage() returns false).
   */
  public boolean nextKeyValue() throws IOException {
    // Ensure we have an active page/jsonReader
    if (jsonReader == null) {
      // Need to open the next page
      boolean pageOpened;
      try {
        pageOpened = openNextPage();
      } catch (ServiceNowAPIException e) {
        throw new IOException("Exception in nextKeyValue" + tableName, e);
      }
      if (!pageOpened) {
        // No more pages
        return false;
      }
    }

    // At this point jsonReader is positioned inside the "result" array.
    JsonToken token = jsonReader.peek();

    if (token == JsonToken.BEGIN_OBJECT) {
      LOG.debug("Reading record object for table {} at position {}", tableName, pos);
      this.row = gson.fromJson(jsonReader, mapType); // assign row
      pos++;
      return true;
    }
    closeCurrentPage();
    return false;
  }

  @Override
  public StructuredRecord getCurrentValue() throws IOException {
    StructuredRecord.Builder recordBuilder = StructuredRecord.builder(schema);
    recordBuilder.set(tableNameField, tableName);

    try {
      for (Schema.Field field : tableFields) {
        String fieldName = field.getName();
        ServiceNowRecordConverter.convertToValue(fieldName, field.getSchema(), row,
                                                 recordBuilder);
      }
    } catch (Exception e) {
      throw new IOException("Error decoding row from table " + tableName, e);
    }
    return recordBuilder.build();
  }

  @VisibleForTesting
  RestAPIResponse fetchData() throws ServiceNowAPIException {
    // Get the table data
    RestAPIResponse restAPIResponse = restApi.fetchTableRecordsRetryableMode(tableName,
      multiSourcePluginConf.getValueType(), multiSourcePluginConf.getStartDate(), multiSourcePluginConf.getEndDate(),
        split.getOffset(), multiSourcePluginConf.getPageSize());
    
    return restAPIResponse;
  }

  private boolean openNextPage() throws IOException, ServiceNowAPIException {
    LOG.debug("Opening next page for table {} at offset {}", tableName, split.getOffset());
    closeCurrentPage();
    LOG.debug("Fetching data for table {} at offset {}", tableName, split.getOffset());
    RestAPIResponse resp = fetchData();
    LOG.debug("Fetched data for table {} at offset {}", tableName, split.getOffset());
    InputStream in = resp.getBodyAsStream();
    if (in == null) {
      return false;
    }
    this.jsonReader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    this.jsonReader.setLenient(true);

    // Position the reader to the "result" array: { "result": [ ... ], ... }
    try {
      JsonToken top;
      try {
        top = jsonReader.peek();
        LOG.info("Peeking JSON token for table {}: {}", tableName, top);
      } catch (IOException e) {
        LOG.warn("Unexpected closure of stream while peeking JSON token for table {}", tableName, e);
        closeCurrentPage();
        return false;
      }
      if (top == JsonToken.BEGIN_OBJECT) {
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
          String name = jsonReader.nextName();
          if (name.equals(ServiceNowConstants.RESULT)) {
            if (jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
              jsonReader.beginArray();
              return true;
            } else {
              jsonReader.skipValue();
              break;
            }
          } else {
            jsonReader.skipValue();
          }
        }
      } else if (top == JsonToken.BEGIN_ARRAY) {
        jsonReader.beginArray();
        return true;
      } else if (jsonReader.peek() == JsonToken.END_ARRAY) {
        // empty result array — treat as no-more-data for this split/page
        LOG.debug("openNextPage: found empty result array (no records). Closing and returning false.");
        jsonReader.endArray();
        // cleanup
        closeCurrentPage();
        return false;
      }
    } catch (IOException e) {
      closeCurrentPage();
      throw e;
    }

    // No "result" array not found, close the current page and return false
    closeCurrentPage();
    return false;
  }

  public void closeCurrentPage() {
    LOG.info("Closing current page for table {}", tableName);
    if (this.jsonReader != null) {
      try {
        this.jsonReader.close();
      } catch (IOException e) {
        LOG.warn("Error closing JSON reader", e);
      } finally {
        this.jsonReader = null;
      }
    }
  }

  private void fetchSchema(ServiceNowTableAPIClientImpl restApi) {
    // Fetch the schema
    try {
      Schema tempSchema = restApi.fetchTableSchema(tableName, multiSourcePluginConf.getValueType());
      if (tempSchema == null) {
        return;
      }
      tableFields = tempSchema.getFields();
      List<Schema.Field> schemaFields = new ArrayList<>(tableFields);
      schemaFields.add(Schema.Field.of(tableNameField, Schema.of(Schema.Type.STRING)));
      schema = Schema.recordOf(tableName, schemaFields);
    } catch (ServiceNowAPIException e) {
      throw new RuntimeException(e);
    }
  }

}
