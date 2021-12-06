package io.cdap.plugin.servicenow.source;

import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.macro.Macros;
import io.cdap.cdap.api.plugin.PluginProperties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServiceNowRecordReaderTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() throws IOException {
    ServiceNowRecordReader actualServiceNowRecordReader = new ServiceNowRecordReader();
    actualServiceNowRecordReader.close();
    assertEquals(0, actualServiceNowRecordReader.pos);
  }

  @Test
  public void testConstructor2() throws IOException {
    ServiceNowSourceConfig serviceNowSourceConfig = new ServiceNowSourceConfig("Reference Name", "Query Mode",
      "Application Name", "Table Name Field", "Table Name", "42", "Client Secret",
      "https://dev115725.service-now.com/", "User", "password", "Actual", "2021-12-30", "2021-12-31");

    ServiceNowRecordReader actualServiceNowRecordReader = new ServiceNowRecordReader(serviceNowSourceConfig);
    actualServiceNowRecordReader.close();
    assertEquals(0, actualServiceNowRecordReader.pos);
    assertEquals("Table Name Field", serviceNowSourceConfig.tableNameField);
    assertEquals("User", serviceNowSourceConfig.getUser());
    assertEquals("Table Name Field", serviceNowSourceConfig.getTableNameField());
    assertEquals("Table Name", serviceNowSourceConfig.getTableName());
    assertEquals("2021-12-30", serviceNowSourceConfig.getStartDate());
    assertEquals("https://dev115725.service-now.com/", serviceNowSourceConfig.getRestApiEndpoint());
    assertEquals("Reference Name", serviceNowSourceConfig.getReferenceName());
    assertEquals("password", serviceNowSourceConfig.getPassword());
    assertEquals("42", serviceNowSourceConfig.getClientId());
    assertEquals("2021-12-31", serviceNowSourceConfig.getEndDate());
    assertEquals("Client Secret", serviceNowSourceConfig.getClientSecret());
    PluginProperties properties = serviceNowSourceConfig.getProperties();
    assertEquals("PluginProperties{properties={}, macros=Macros{lookupProperties=[], macroFunctions=[]}}",
      properties.toString());
    assertTrue(properties.getProperties().isEmpty());
    Macros macros = properties.getMacros();
    assertEquals("Macros{lookupProperties=[], macroFunctions=[]}", macros.toString());
    assertTrue(macros.getMacroFunctions().isEmpty());
    assertTrue(macros.getLookups().isEmpty());
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
    assertEquals("Field Value", (new ServiceNowRecordReader()).convertToStringValue("Field Value"));
  }

  @Test
  public void testConvertToDoubleValue() {
    assertEquals(42.0, (new ServiceNowRecordReader()).convertToDoubleValue("42").doubleValue(), 0.0);
    assertEquals(42.0, (new ServiceNowRecordReader()).convertToDoubleValue(42).doubleValue(), 0.0);
    assertNull((new ServiceNowRecordReader()).convertToDoubleValue(""));
  }

  @Test
  public void testConvertToIntegerValue() {
    assertEquals(42, (new ServiceNowRecordReader()).convertToIntegerValue("42").intValue());
    assertEquals(42, (new ServiceNowRecordReader()).convertToIntegerValue(42).intValue());
    assertNull((new ServiceNowRecordReader()).convertToIntegerValue(""));
  }

  @Test
  public void testConvertToBooleanValue() {
    assertFalse((new ServiceNowRecordReader()).convertToBooleanValue("Field Value"));
    assertFalse((new ServiceNowRecordReader()).convertToBooleanValue(42));
    assertNull((new ServiceNowRecordReader()).convertToBooleanValue(""));
  }
}

