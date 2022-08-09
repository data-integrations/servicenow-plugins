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

package io.cdap.plugin.servicenow.util;

import io.cdap.cdap.api.data.schema.Schema;

/**
 * Information about a ServiceNow table.
 */
public class ServiceNowTableInfo {
  private final String tableName;
  private final Schema schema;
  private final int recordCount;

  /**
   * Constructor for ServiceNowTableInfo object.
   *
   * @param tableName The table name
   * @param schema The instance of Schema object
   * @param recordCount The record count
   */
  public ServiceNowTableInfo(String tableName, Schema schema, int recordCount) {
    this.tableName = tableName;
    this.schema = schema;
    this.recordCount = recordCount;
  }

  public String getTableName() {
    return tableName;
  }

  public Schema getSchema() {
    return schema;
  }

  public int getRecordCount() {
    return recordCount;
  }
}
