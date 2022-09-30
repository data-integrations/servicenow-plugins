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

import io.cdap.plugin.servicenow.sink.output.ServiceNowOutputFormat;
import io.cdap.plugin.servicenow.source.ServiceNowJobConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobContext;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import java.io.IOException;

public class ServicenowOutputFormatTest {

  @Test
  public void testNeedsTaskCommit() throws IOException, InterruptedException {
    TaskAttemptContext taskAttemptContext = Mockito.mock(TaskAttemptContext.class);
    ServiceNowOutputFormat outputFormat = new ServiceNowOutputFormat();
    Assert.assertFalse(outputFormat.getOutputCommitter(taskAttemptContext).needsTaskCommit(taskAttemptContext));
  }

  @Test
  public void testGetRecordWriter() throws Exception {
    JobContext context1 = Mockito.mock(JobContext.class);
    Configuration configuration = new Configuration();
    TaskAttemptContext taskAttemptContext = Mockito.mock(TaskAttemptContext.class);
    Mockito.when(taskAttemptContext.getConfiguration()).thenReturn(configuration);
    ServiceNowJobConfiguration jobConfig = Mockito.mock(ServiceNowJobConfiguration.class);
    ServiceNowSinkConfig serviceNowSinkConfig = Mockito.mock(ServiceNowSinkConfig.class);
    Mockito.when(jobConfig.getSinkPluginConf()).thenReturn(serviceNowSinkConfig);
    ServiceNowOutputFormat outputFormat = new ServiceNowOutputFormat();
    outputFormat.checkOutputSpecs(context1);
    Assert.assertNotNull(outputFormat.getRecordWriter(taskAttemptContext));
  }
}
