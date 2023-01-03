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
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base Record reader class that provides a basic structure for Derived Record Reader classes.
 */
public abstract class ServiceNowBaseRecordReader extends RecordReader<NullWritable, StructuredRecord> {
  protected ServiceNowInputSplit split;
  protected int pos;
  protected List<Schema.Field> tableFields;
  protected Schema schema;

  protected String tableName;
  protected String tableNameField;
  protected List<Map<String, Object>> results;
  protected Iterator<Map<String, Object>> iterator;
  protected Map<String, Object> row;

  public ServiceNowBaseRecordReader() {
  }

  public void initialize(InputSplit split, TaskAttemptContext context) {
    this.split = (ServiceNowInputSplit) split;
    this.pos = 0;
  }

  public abstract boolean nextKeyValue() throws IOException;

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
