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
package io.cdap.plugin.servicenow.sink.output;

import com.google.gson.JsonObject;
import io.cdap.plugin.servicenow.sink.ServiceNowSinkConfig;
import io.cdap.plugin.servicenow.sink.transform.ServiceNowRecordWriter;
import io.cdap.plugin.servicenow.source.ServiceNowJobConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 * An OutputFormat that sends the output of a Hadoop job to the ServiceNow record writer, also
 * it defines the output committer.
 */
public class ServiceNowOutputFormat extends OutputFormat<NullWritable, JsonObject> {
  
  public static void setOutput(Configuration conf, ServiceNowSinkConfig pluginConf) {
    ServiceNowJobConfiguration jobConf = new ServiceNowJobConfiguration(conf);
    jobConf.setSinkPluginConfiguration(pluginConf);
  }

  @Override
  public RecordWriter<NullWritable, JsonObject> getRecordWriter(TaskAttemptContext taskAttemptContext) {
    ServiceNowJobConfiguration jobConfig = new ServiceNowJobConfiguration(taskAttemptContext.getConfiguration());
    ServiceNowSinkConfig pluginConf = jobConfig.getSinkPluginConf();
    try {
      return new ServiceNowRecordWriter(pluginConf);
    } catch (Exception e) {
      throw new RuntimeException("There was issue communicating with Servicenow", e);
    }
  }

  @Override
  public void checkOutputSpecs(JobContext jobContext) throws IOException, InterruptedException {

  }

  @Override
  public OutputCommitter getOutputCommitter(TaskAttemptContext taskAttemptContext) throws IOException,
    InterruptedException {
    return new OutputCommitter() {
      @Override
      public void setupJob(JobContext jobContext) throws IOException {

      }

      @Override
      public void setupTask(TaskAttemptContext taskAttemptContext) throws IOException {

      }

      @Override
      public boolean needsTaskCommit(TaskAttemptContext taskAttemptContext) throws IOException {
        return false;
      }

      @Override
      public void commitTask(TaskAttemptContext taskAttemptContext) throws IOException {

      }

      @Override
      public void abortTask(TaskAttemptContext taskAttemptContext) throws IOException {

      }
    };
  }
}
