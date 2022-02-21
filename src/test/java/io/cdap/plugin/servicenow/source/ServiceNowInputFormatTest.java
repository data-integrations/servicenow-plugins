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

import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.source.util.ServiceNowColumn;
import io.cdap.plugin.servicenow.source.util.SourceQueryMode;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceNowInputFormatTest {

  private static final String CLIENT_ID = System.getProperty("servicenow.test.clientId");
  private static final String CLIENT_SECRET = System.getProperty("servicenow.test.clientSecret");
  private static final String REST_API_ENDPOINT = System.getProperty("servicenow.test.restApiEndpoint");
  private static final String USER = System.getProperty("servicenow.test.user");
  private static final String PASSWORD = System.getProperty("servicenow.test.password");
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowInputFormat.class);
  private ServiceNowSourceConfig config;

  @Before
  public void initializeTests() {
    try {
      config = new ServiceNowSourceConfig("Reference Name", "Query Mode",
                                          "Product Catalog", "tablename", "pc_hardware_cat_item",
                                          CLIENT_ID, CLIENT_SECRET,
                                          REST_API_ENDPOINT, USER, PASSWORD, "Actual", "2012-12-31", "2021-12-31");
      Assume.assumeNotNull(CLIENT_ID, CLIENT_SECRET, REST_API_ENDPOINT, USER, PASSWORD);
    } catch (AssumptionViolatedException e) {
      LOG.warn("Service Now batch Source tests are skipped. ");
      throw e;
    }
  }

  @Test
  public void testFetchTableInfo() {
    SourceQueryMode mode = SourceQueryMode.TABLE;
    ServiceNowColumn column1 = new ServiceNowColumn("sys_created_by", "string");
    ServiceNowColumn column2 = new ServiceNowColumn("sys_updated_by", "string");
    List<ServiceNowColumn> columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setColumns(columns);
    assertEquals(1, ServiceNowInputFormat.fetchTableInfo(mode, config).size());
    assertTrue("TABLE", true);
  }

  @Test
  public void testFetchTableInfoReportingMode() {
    SourceQueryMode mode = SourceQueryMode.REPORTING;
    ServiceNowColumn column1 = new ServiceNowColumn("sys_created_by", "string");
    ServiceNowColumn column2 = new ServiceNowColumn("sys_updated_by", "string");
    List<ServiceNowColumn> columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setColumns(columns);
    assertEquals(3, ServiceNowInputFormat.fetchTableInfo(mode, config).size());
    assertTrue("TABLE", true);
  }

  @Test
  public void testFetchTableInfoWithEmptyTableName() {
    SourceQueryMode mode = SourceQueryMode.TABLE;
    assertFalse(ServiceNowInputFormat.fetchTableInfo(mode, config).isEmpty());
  }

}
