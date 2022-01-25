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

import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.macro.Macros;
import io.cdap.cdap.api.plugin.PluginProperties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;

public class ServiceNowRecordReaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() throws IOException {
    ServiceNowRecordReader actualServiceNowRecordReader = new ServiceNowRecordReader();
    actualServiceNowRecordReader.close();
    Assert.assertEquals(0, actualServiceNowRecordReader.pos);
  }

  @Test
  public void testConstructor2() throws IOException {
    ServiceNowSourceConfig serviceNowSourceConfig = new ServiceNowSourceConfig("Reference Name", "Query Mode",
      "Application Name", "Table Name Field", "Table Name", "42", "Client Secret",
      "https://dev115725.service-now.com/", "User", "password", "Actual", "2021-12-30", "2021-12-31");

    ServiceNowRecordReader actualServiceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    actualServiceNowRecordReader.close();
    Assert.assertEquals(0, actualServiceNowRecordReader.pos);
    Assert.assertEquals("Table Name Field", serviceNowSourceConfig.tableNameField);
    Assert.assertEquals("User", serviceNowSourceConfig.getUser());
    Assert.assertEquals("Table Name Field", serviceNowSourceConfig.getTableNameField());
    Assert.assertEquals("Table Name", serviceNowSourceConfig.getTableName());
    Assert.assertEquals("2021-12-30", serviceNowSourceConfig.getStartDate());
    Assert.assertEquals("https://dev115725.service-now.com/", serviceNowSourceConfig.getRestApiEndpoint());
    Assert.assertEquals("Reference Name", serviceNowSourceConfig.getReferenceName());
    Assert.assertEquals("password", serviceNowSourceConfig.getPassword());
    Assert.assertEquals("42", serviceNowSourceConfig.getClientId());
    Assert.assertEquals("2021-12-31", serviceNowSourceConfig.getEndDate());
    Assert.assertEquals("Client Secret", serviceNowSourceConfig.getClientSecret());
    PluginProperties properties = serviceNowSourceConfig.getProperties();
    Assert.assertEquals("PluginProperties{properties={}, macros=Macros{lookupProperties=[], macroFunctions=[]}}",
      properties.toString());
    Assert.assertTrue(properties.getProperties().isEmpty());
    Macros macros = properties.getMacros();
    Assert.assertEquals("Macros{lookupProperties=[], macroFunctions=[]}", macros.toString());
    Assert.assertTrue(macros.getMacroFunctions().isEmpty());
    Assert.assertTrue(macros.getLookups().isEmpty());
  }

  @Test
  public void testGetCurrentKey() {
    // TODO: This test is incomplete.
    (new ServiceNowRecordReader()).getCurrentKey();
  }

  @Test
  public void testGetCurrentKey2() {
    // TODO: This test is incomplete.

    (new ServiceNowMultiRecordReader(new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42",
      "Client Secret", "https://dev115725.service-now.com/", "User", "password", "Actual", "2021-12-30", "2021-12-31",
      "Table Names"))).getCurrentKey();
  }

  @Test
  public void testConvertToValue() {
    ServiceNowRecordReader serviceNowRecordReader = new ServiceNowRecordReader();
    Schema fieldSchema = Schema.of(Schema.LogicalType.TIMESTAMP_MILLIS);
    thrown.expect(IllegalStateException.class);
    serviceNowRecordReader.convertToValue("Field Name", fieldSchema, new HashMap<>(1));
  }

  @Test
  public void testConvertToValue2() {
    // TODO: This test is incomplete.

    ServiceNowRecordReader serviceNowRecordReader = new ServiceNowRecordReader();
    Schema fieldSchema = Schema.of(Schema.Type.BOOLEAN);
    serviceNowRecordReader.convertToValue("Field Name", fieldSchema, new HashMap<>(1));
  }

  @Test
  public void testConvertToStringValue() {
    Assert.assertEquals("Field Value", (new ServiceNowRecordReader()).convertToStringValue("Field Value"));
  }

  @Test
  public void testConvertToDoubleValue() {
    Assert.assertEquals(42.0, (new ServiceNowRecordReader()).convertToDoubleValue("42").doubleValue(), 0.0);
    Assert.assertEquals(42.0, (new ServiceNowRecordReader()).convertToDoubleValue(42).doubleValue(), 0.0);
    Assert.assertNull(new ServiceNowRecordReader().convertToDoubleValue(""));
  }

  @Test
  public void testConvertToIntegerValue() {
    Assert.assertEquals(42, (new ServiceNowRecordReader()).convertToIntegerValue("42").intValue());
    Assert.assertEquals(42, (new ServiceNowRecordReader()).convertToIntegerValue(42).intValue());
    Assert.assertNull(new ServiceNowRecordReader().convertToIntegerValue(""));
  }

  @Test
  public void testConvertToBooleanValue() {
    Assert.assertFalse(new ServiceNowRecordReader().convertToBooleanValue("Field Value"));
    Assert.assertFalse(new ServiceNowRecordReader().convertToBooleanValue(42));
    Assert.assertNull(new ServiceNowRecordReader().convertToBooleanValue(""));
  }
}

