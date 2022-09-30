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
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.format.UnexpectedFormatException;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.validation.CauseAttributes;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.servicenow.sink.transform.ServiceNowTransformer;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Objects;

public class RecordToJsonTransformerTest {

  @Test
  public void testTransformWithValueNull() {
    Schema schema = Schema.recordOf("record",
                                    Schema.Field.of("id", Schema.of(Schema.Type.LONG)));
    StructuredRecord record = Mockito.mock(StructuredRecord.class);
    Mockito.when(record.getSchema()).thenReturn(schema);
    ServiceNowTransformer recordToJsonTransformer = new ServiceNowTransformer();
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(Objects.requireNonNull(schema.getField("id")).getName(), "null");
    Assert.assertEquals("The value displayed for the id will be null",
                         jsonObject, recordToJsonTransformer.transform(record));
  }

  @Test
  public void testTransformWithValue() {
    Schema schema = Schema.recordOf("record",
                                    Schema.Field.of("id", Schema.of(Schema.Type.LONG)),
                                    Schema.Field.of("price", Schema.of(Schema.Type.DOUBLE)),
                                    Schema.Field.of("stockSize", Schema.of(Schema.Type.INT)),
                                    Schema.Field.of("updated", Schema.of(Schema.Type.BOOLEAN)),
                                    Schema.Field.of("batchId", Schema.of(Schema.Type.STRING)),
                                    Schema.Field.of("tax", Schema.of(Schema.Type.FLOAT)),
                                    Schema.Field.of("timestampMillis",
                                                    Schema.nullableOf(Schema.of(Schema.LogicalType.TIMESTAMP_MILLIS))),
                                    Schema.Field.of("timeMillis",
                                                    Schema.nullableOf(Schema.of(Schema.LogicalType.TIME_MILLIS))),
                                    Schema.Field.of("timestamp",
                                                    Schema.nullableOf(Schema.of(Schema.LogicalType.TIMESTAMP_MICROS))),
                                    Schema.Field.of("timeMicros",
                                                    Schema.nullableOf(Schema.of(Schema.LogicalType.TIME_MICROS))),
                                    Schema.Field.of("date",
                                                    Schema.nullableOf(Schema.of(Schema.LogicalType.DATE))),
                                    Schema.Field.of("dateTime",
                                                    Schema.nullableOf(Schema.of(Schema.LogicalType.DATETIME))),
                                    Schema.Field.of("updatedPrice", (Schema.decimalOf(5, 2))));

    StructuredRecord record = Mockito.mock(StructuredRecord.class);
    Mockito.when(record.get("date")).thenReturn("20220610");
    Mockito.when(record.get("id")).thenReturn(1L);
    Mockito.when(record.get("price")).thenReturn(20.2008);
    Mockito.when(record.get("stockSize")).thenReturn(100);
    Mockito.when(record.get("updated")).thenReturn(Boolean.TRUE);
    Mockito.when(record.get("batchId")).thenReturn("12DER");
    Mockito.when(record.get("tax")).thenReturn(50.0000);
    Mockito.when(record.get("updatedPrice")).thenReturn(new BigDecimal(24.2).unscaledValue().toByteArray());
    Mockito.when(record.get("timestampMillis")).thenReturn(1508484583259L);
    Mockito.when(record.get("timestamp")).thenReturn(1508484583259L);
    Mockito.when(record.get("timeMillis")).thenReturn(85398);
    Mockito.when(record.get("timeMicros")).thenReturn(8638999L);
    Mockito.when(record.get("dateTime")).thenReturn("2022-06-10T10:43:56");
    Mockito.when(record.getSchema()).thenReturn(schema);
    ServiceNowTransformer recordToJsonTransformer = new ServiceNowTransformer();
    JsonObject jsonObject = recordToJsonTransformer.transform(record);
    Assert.assertEquals("1", jsonObject.get("id").getAsString());
    Assert.assertEquals("1970-01-18T11:01:24.583259Z[UTC]", jsonObject.get("timestamp").getAsString());
  }

  @Test (expected = UnexpectedFormatException.class)
  public void testTransformWithIncorrectDateTime() {
    Schema schema = Schema.recordOf("record",
                                    Schema.Field.of("dateTime",
                                                    Schema.nullableOf(Schema.of(Schema.LogicalType.DATETIME))));
    StructuredRecord record = Mockito.mock(StructuredRecord.class);
    Mockito.when(record.get("dateTime")).thenReturn("2022-06-1010:43:56");
    Mockito.when(record.getSchema()).thenReturn(schema);
    ServiceNowTransformer recordToJsonTransformer = new ServiceNowTransformer();
    recordToJsonTransformer.transform(record);
  }
}
