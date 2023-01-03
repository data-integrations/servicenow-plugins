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

import com.google.gson.JsonObject;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Metadata;
import io.cdap.cdap.api.annotation.MetadataProperty;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.batch.Output;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.dataset.lib.KeyValue;
import io.cdap.cdap.etl.api.Emitter;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.PipelineConfigurer;
import io.cdap.cdap.etl.api.StageConfigurer;
import io.cdap.cdap.etl.api.batch.BatchRuntimeContext;
import io.cdap.cdap.etl.api.batch.BatchSink;
import io.cdap.cdap.etl.api.batch.BatchSinkContext;
import io.cdap.cdap.etl.api.connector.Connector;
import io.cdap.plugin.common.LineageRecorder;
import io.cdap.plugin.servicenow.sink.output.ServiceNowOutputFormat;
import io.cdap.plugin.servicenow.sink.output.ServiceNowOutputFormatProvider;
import io.cdap.plugin.servicenow.sink.transform.ServiceNowTransformer;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;

import java.util.stream.Collectors;

/**
 * A {@link BatchSink} that writes data into the specified table in ServiceNow.
 */
@Plugin(type = BatchSink.PLUGIN_TYPE)
@Name(ServiceNowConstants.PLUGIN_NAME)
@Description("Writes to the target table in ServiceNow.")
@Metadata(properties = {@MetadataProperty(key = Connector.PLUGIN_TYPE, value = ServiceNowConstants.PLUGIN_NAME)})
public class ServiceNowSink extends BatchSink<StructuredRecord, NullWritable, JsonObject> {

  private final ServiceNowSinkConfig conf;
  private ServiceNowTransformer transformer;

  public ServiceNowSink(ServiceNowSinkConfig conf) {
    this.conf = conf;
  }

  @Override
  public void configurePipeline(PipelineConfigurer pipelineConfigurer) {
    super.configurePipeline(pipelineConfigurer);
    StageConfigurer stageConfigurer = pipelineConfigurer.getStageConfigurer();
    FailureCollector collector = stageConfigurer.getFailureCollector();
    conf.validate(collector);
    if (collector.getValidationFailures().isEmpty() && stageConfigurer.getInputSchema() != null) {
      conf.validateSchema(stageConfigurer.getInputSchema(), collector);
    }
    Schema schema = conf.getSchema(collector);
    if (schema != null) {
      stageConfigurer.setOutputSchema(schema);
    } else {
      stageConfigurer.setOutputSchema(stageConfigurer.getInputSchema());
    }
  }

  @Override
  public void prepareRun(BatchSinkContext context) throws Exception {
    Schema inputSchema = context.getInputSchema();
    FailureCollector collector = context.getFailureCollector();
    conf.validate(collector);
    collector.getOrThrowException();
    Configuration hConf = new Configuration();
    ServiceNowOutputFormat.setOutput(hConf, conf);
    context.addOutput(Output.of(conf.referenceName, new ServiceNowOutputFormatProvider(hConf)));

    LineageRecorder lineageRecorder = new LineageRecorder(context, conf.referenceName);
    lineageRecorder.createExternalDataset(inputSchema);
    // Record the field level WriteOperation
    if (inputSchema.getFields() != null && !inputSchema.getFields().isEmpty()) {
      String operationDescription = String.format("Wrote to Servicenow %s", conf.getTableName());
      lineageRecorder.recordWrite("Write", operationDescription,
                                  inputSchema.getFields().stream()
                                    .map(Schema.Field::getName)
                                    .collect(Collectors.toList()));
    }

  }
  @Override
  public void initialize(BatchRuntimeContext context) throws Exception {
    super.initialize(context);
    this.transformer = new ServiceNowTransformer();
  }

  @Override
  public void transform(StructuredRecord record, Emitter<KeyValue<NullWritable, JsonObject>> emitter) {
    JsonObject jsonObject = transformer.transform(record);
    emitter.emit(new KeyValue<>(null, jsonObject));
  }

}
