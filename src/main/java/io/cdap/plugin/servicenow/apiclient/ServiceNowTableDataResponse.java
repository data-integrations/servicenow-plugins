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

package io.cdap.plugin.servicenow.apiclient;

import io.cdap.plugin.servicenow.util.ServiceNowColumn;

import java.util.List;
import java.util.Map;

/**
 * A Pojo class to wrap the success response for ServiceNow Table data.
 */
public class ServiceNowTableDataResponse {
  private int totalRecordCount;

  private List<ServiceNowColumn> columns;

  private List<Map<String, Object>> result;

  public int getTotalRecordCount() {
    return totalRecordCount;
  }

  public void setTotalRecordCount(int totalRecordCount) {
    this.totalRecordCount = totalRecordCount;
  }

  public List<ServiceNowColumn> getColumns() {
    return columns;
  }

  public void setColumns(List<ServiceNowColumn> columns) {
    this.columns = columns;
  }

  public List<Map<String, Object>> getResult() {
    return result;
  }

  public void setResult(List<Map<String, Object>> result) {
    this.result = result;
  }
}
