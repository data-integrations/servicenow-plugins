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

import org.junit.Assert;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServiceNowInputSplitTest {

  @Test
  public void testInputSplitWithNonEmptyTableName() throws IOException, InterruptedException {
    ServiceNowInputSplit actualServiceNowInputSplit = new ServiceNowInputSplit("Table Name", 2);
    assertEquals(0L, actualServiceNowInputSplit.getLength());
    assertEquals(2, actualServiceNowInputSplit.getOffset());
    assertEquals("Table Name", actualServiceNowInputSplit.getTableName());
  }

  @Test
  public void testInputSplitWithEmptyTableName() throws IOException, InterruptedException {
    ServiceNowInputSplit actualServiceNowInputSplit = new ServiceNowInputSplit();
    assertEquals(0L, actualServiceNowInputSplit.getLength());
    assertEquals(0, actualServiceNowInputSplit.getOffset());
    Assert.assertNull(actualServiceNowInputSplit.getTableName());
  }

  @Test
  public void testReadFields() throws IOException {
    ServiceNowInputSplit serviceNowInputSplit = new ServiceNowInputSplit("Table Name", 2);
    ObjectInputStream objectInputStream = mock(ObjectInputStream.class);
    when(objectInputStream.readInt()).thenReturn(1);
    when(objectInputStream.readUTF()).thenReturn("Utf");
    serviceNowInputSplit.readFields(objectInputStream);
    verify(objectInputStream).readInt();
    verify(objectInputStream).readUTF();
    assertEquals("Utf", serviceNowInputSplit.getTableName());
    assertEquals(1, serviceNowInputSplit.getOffset());
  }

  @Test
  public void testGetLocations() throws IOException, InterruptedException {
    assertEquals(0, (new ServiceNowInputSplit("Table Name", 2)).getLocations().length);
  }


  @Test
  public void testGetTableName() {
    String expectedValue = "Table";
    String tableName = "Table";
    int offset = 0;

    ServiceNowInputSplit servicenowinputsplit = new ServiceNowInputSplit(tableName, offset);
    String actualValue = servicenowinputsplit.getTableName();
    assertEquals(expectedValue, actualValue);
  }

  @Test
  public void testGetOffset() {
    int expectedValue = 0;
    String tableName = "Table";
    int offset = 0;
    ServiceNowInputSplit servicenowinputsplit = new ServiceNowInputSplit(tableName, offset);
    int actualValue = servicenowinputsplit.getOffset();
    assertEquals(expectedValue, actualValue);

  }

  @Test
  public void testWriteWithNullData() {
    try {

      DataOutput dataOutput = null;
      String tableName = "";
      int offset = 0;

      ServiceNowInputSplit serviceNowInputSplit = new ServiceNowInputSplit(tableName, offset);
      serviceNowInputSplit.write(dataOutput);

    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  @Test
  public void testRead() {
    try {
      DataInput dataInput = null;
      String tableName = "";
      int offset = 0;

      ServiceNowInputSplit servicenowinputsplit = new ServiceNowInputSplit(tableName, offset);
      servicenowinputsplit.readFields(dataInput);

    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

}
