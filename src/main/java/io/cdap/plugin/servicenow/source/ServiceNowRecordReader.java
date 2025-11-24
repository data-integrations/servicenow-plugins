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
import io.cdap.plugin.servicenow.util.ServiceNowTableInfo;
import io.cdap.plugin.servicenow.util.SourceQueryMode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Record reader that reads the entire contents of a ServiceNow table.
 */
public class ServiceNowRecordReader extends ServiceNowBaseRecordReader {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowRecordReader.class);
  private final ServiceNowSourceConfig pluginConf;
  private ServiceNowTableAPIClientImpl restApi;
  private final Gson gson = new Gson();
  private final Type mapType = new TypeToken<Map<String, String>>() { }.getType();
  private JsonReader jsonReader = null;

  public ServiceNowRecordReader(ServiceNowSourceConfig pluginConf) {
    super();
    this.pluginConf = pluginConf;
  }

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context) {
    initialize(split);
    fetchAndInitializeSchema(new ServiceNowJobConfiguration(context.getConfiguration()).getTableInfos(), tableName);
  }

  /**
   * Initialize with only the provided split and given schema
   * This method should not be called directly from the code,
   * as Hadoop runtime initialize internally during execution.
   *
   * @param split Split to read by the current reader.
   */
  public void initialize(InputSplit split, Schema schema) {
    initialize(split);
    initializeSchema(tableName, schema);
  }

  /**
   * The refactored nextKeyValue() — uses Gson JsonReader to stream one record at a time.
   * Returns true when it assigned `row` to the next Map<String,String> record.
   * Returns false only when there are no more pages/records (i.e., openNextPage() returns false).
   */
  @Override
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

    if (token == JsonToken.END_ARRAY) {
      // Current page exhausted. Close current page and try to open the next page.
      closeCurrentPage();

      // Attempt to open next page; if none, we must return false (end of data)
      boolean openedNext;
      try {
        openedNext = openNextPage();
        LOG.info("Opened next page for table {} at offset {}: {}", tableName, split.getOffset(), openedNext);
      } catch (ServiceNowAPIException e) {
        throw new IOException("Exception in nextKeyValue " + tableName, e);
      }
      if (!openedNext) {
        // No more pages
        return false;
      }
      // continue loop to attempt to read from newly opened page
    }

    if (token == JsonToken.BEGIN_OBJECT) {
      LOG.info("Reading record object for table {} at position {}", tableName, pos);
      // Read exactly one object from the stream into a Map<String,String>
      Map<String, String> recordMap = gson.fromJson(jsonReader, mapType);
      this.row = recordMap; // assign row
      pos++;
      return true;
    }
    return false;

    /*if (token == JsonToken.NULL) {
      // skip nulls if any and continue
      jsonReader.nextNull();
      continue;
    }*/

      // Skip any unexpected or non-object token and loop
      //jsonReader.skipValue();

    /*********************----------------**********************/
      /*// read the next record from the current page
      try {
        if (jsonreader.hasnext()) {
          // there is another record in the current page
          map<string, string> recordmap = new hashmap<>();
          jsonreader.beginobject();
          while (jsonreader.hasnext()) {
            string name = jsonreader.nextname();
            jsontoken token = jsonreader.peek();
            string value = null;
            if (token == jsontoken.null) {
              jsonreader.nextnull();
            } else {
              value = jsonreader.nextstring();
            }
            recordmap.put(name, value);
          }
          jsonreader.endobject();
          row = recordmap;
          pos++;
          return true;
        } else {
          // end of current page
          closecurrentpage();
        }
      } catch (ioexception e) {
        // cleanup on parse error
        closecurrentpage();
        log.error("error parsing json response from table " + tablename, e);
        throw e;
      }*/

    /*try {
        InputStream inputStream = fetchData();
        do {
          row = restApi.parseResponseStreamToRecord(inputStream);
          pos++;
          return true;
        } while (inputStream.available() > 0);
    } catch (Exception e) {
      LOG.error("Error in nextKeyValue", e);
      throw new IOException("Exception in nextKeyValue", e);
    }*/
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

  private RestAPIResponse fetchData() throws ServiceNowAPIException {
    // Get the table data
    RestAPIResponse restAPIResponse = restApi.fetchTableRecordsRetryableMode(tableName, pluginConf.getValueType(),
     pluginConf.getStartDate(), pluginConf.getEndDate(), split.getOffset(), pluginConf.getPageSize());
    

    // LOG.debug("Results size={}", results.size());
    return restAPIResponse;

    // iterator = results.iterator();
    // iterator =  record;
  }

  private boolean openNextPage() throws IOException, ServiceNowAPIException {
    LOG.info("Opening next page for table {} at offset {}", tableName, split.getOffset());
    closeCurrentPage();
    LOG.info("Fetching data for table {} at offset {}", tableName, split.getOffset());
    RestAPIResponse resp = fetchData();
    LOG.info("Fetched data for table {} at offset {}", tableName, split.getOffset());
    InputStream in = resp.getInputStream();
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
        LOG.info("openNextPage: found empty result array (no records). Closing and returning false.");
        // consume the END_ARRAY token to leave stream consistent (optional)
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

  protected void initialize(InputSplit split) {
    this.split = (ServiceNowInputSplit) split;
    this.pos = 0;
    restApi = new ServiceNowTableAPIClientImpl(pluginConf.getConnection(), pluginConf.getUseConnection());
    tableName = ((ServiceNowInputSplit) split).getTableName();
    tableNameField = pluginConf.getTableNameField();
  }

  /**
   * Fetches the schema of the given tableName from the tableInfos and initialize schema using it.
   *
   * @param tableInfos List of TableInfo objects containing TableName, RecordCount and Schema.
   * @param tableName Table Name to initialize this reader for.
   */
  private void fetchAndInitializeSchema(List<ServiceNowTableInfo> tableInfos, String tableName) {
    Schema tempSchema = tableInfos.stream()
        .filter((tableInfo) -> tableInfo.getTableName().equalsIgnoreCase(tableName))
        .findFirst().get().getSchema();

    initializeSchema(tableName, tempSchema);
  }

  private void initializeSchema(String tableName, Schema schema) {
    tableFields = schema.getFields();
    List<Schema.Field> schemaFields = new ArrayList<>(tableFields);

    if (pluginConf.getQueryMode() == SourceQueryMode.REPORTING) {
      schemaFields.add(Schema.Field.of(tableNameField, Schema.of(Schema.Type.STRING)));
    }

    this.schema = Schema.recordOf(tableName, schemaFields);
  }
}
