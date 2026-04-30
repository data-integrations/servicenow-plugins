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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.servicenow.apiclient.ServiceNowAPIException;
import io.cdap.plugin.servicenow.restapi.RestAPIResponse;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base Record reader class that provides a basic structure for Derived Record Reader classes.
 */
public abstract class ServiceNowBaseRecordReader extends RecordReader<NullWritable, StructuredRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowRecordReader.class);
  protected ServiceNowInputSplit split;
  protected int pos;
  protected List<Schema.Field> tableFields;
  protected Schema schema;

  protected String tableName;
  protected String tableNameField;
  protected List<Map<String, String>> results;
  protected Iterator<Map<String, String>> iterator;
  protected JsonObject row;
  protected final Gson gson = new Gson();
  protected final Type mapType = new TypeToken<Map<String, String>>() { }.getType();
  protected JsonReader jsonReader = null;
  protected int expectedRecordsInPage = -1;

  public ServiceNowBaseRecordReader() {
  }

  protected abstract int getPageSize();

  /**
   * This method reads the next key/value pair from the input.
   * <p>
   * The nextKeyValue() uses the jsonReader to read the next record from the current page.
   * <p>
   * Returns true, when it assigned `row` to the next record.
   * Returns false, only when there are no more pages/records (i.e., openNextPage() returns false).
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
      try {
        this.row = gson.fromJson(jsonReader, JsonObject.class);
      } catch (RuntimeException e) {
        // JsonIOException wraps IOException; rethrow as checked so callers handle it uniformly
        throw new IOException("Stream error reading record at position " + pos + " for table " + tableName, e);
      }
      pos++;
      return true;
    }
    closeCurrentPage();
    if (expectedRecordsInPage > 0 && pos < expectedRecordsInPage) {
      throw new IOException(String.format(
        "Stream truncated for table %s: expected %d records but read only %d (offset %d)",
        tableName, expectedRecordsInPage, pos, split.getOffset()));
    }
    return false;
  }
  
  public boolean openNextPage() throws IOException, ServiceNowAPIException {
    closeCurrentPage();
    expectedRecordsInPage = -1;
    RestAPIResponse resp = fetchData();
    String totalCountStr = resp.getHeaders().get(ServiceNowConstants.HEADER_NAME_TOTAL_COUNT);
    if (totalCountStr != null && !totalCountStr.isEmpty()) {
      try {
        int totalCount = Integer.parseInt(totalCountStr);
        expectedRecordsInPage = Math.min(getPageSize(), Math.max(0, totalCount - split.getOffset()));
      } catch (NumberFormatException ignored) {
        // leave expectedRecordsInPage = -1 (unknown)
      }
    }
    InputStream in = resp.getResponseStream();
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
        LOG.debug("Peeking JSON token for table {}: {}", tableName, top);
      } catch (IOException e) {
        LOG.warn("Unexpected closure of stream while peeking JSON token for table {}", tableName, e);
        closeCurrentPage();
        throw e;
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
        closeRestAPIResponse(resp);
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

  public void closeRestAPIResponse(RestAPIResponse resp) {
    if (resp != null) {
      try {
        resp.close();
      } catch (IOException e) {
        LOG.warn("Error closing RestAPIResponse", e);
      }
    }
  }

  abstract RestAPIResponse fetchData() throws ServiceNowAPIException;

  public NullWritable getCurrentKey() {
    return NullWritable.get();
  }

  public abstract StructuredRecord getCurrentValue() throws IOException;

  public float getProgress() throws IOException, InterruptedException {
    return pos / (float) split.getLength();
  }

  public void close() throws IOException {
  }
}
