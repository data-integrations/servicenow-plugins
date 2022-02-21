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
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServiceNowMultiInputFormatTest {

  protected static final String CLIENT_ID = System.getProperty("servicenow.test.clientId");
  protected static final String CLIENT_SECRET = System.getProperty("servicenow.test.clientSecret");
  protected static final String REST_API_ENDPOINT = System.getProperty("servicenow.test.restApiEndpoint");
  protected static final String USER = System.getProperty("servicenow.test.user");
  protected static final String PASSWORD = System.getProperty("servicenow.test.password");
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowMultiInputFormatTest.class);
  private ServiceNowMultiSourceConfig serviceNowMultiSourceConfig;

  @Before
  public void initializeTests() {
    try {
      serviceNowMultiSourceConfig = ServiceNowSourceConfigHelper.newConfigBuilder()
        .setReferenceName("referenceName")
        .setRestApiEndpoint(REST_API_ENDPOINT)
        .setUser(USER)
        .setPassword(PASSWORD)
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setTableNames("sys_user")
        .setValueType("Actual")
        .setStartDate("2021-01-01")
        .setEndDate("2022-02-18")
        .setTableNameField("tablename")
        .buildMultiSource();
      Assume.assumeNotNull(CLIENT_ID, CLIENT_SECRET, REST_API_ENDPOINT, USER, PASSWORD);
    } catch (AssumptionViolatedException e) {
      LOG.warn("Service Now Batch Source tests are skipped. ");
      throw e;
    }
  }


  @Test
  public void testFetchTablesInfo() {
    ServiceNowColumn column1 = new ServiceNowColumn("sys_created_by", "string");
    ServiceNowColumn column2 = new ServiceNowColumn("sys_updated_by", "string");
    List<ServiceNowColumn> columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setColumns(columns);
    Assert.assertEquals(1, ServiceNowMultiInputFormat
      .fetchTablesInfo(serviceNowMultiSourceConfig)
      .size());
  }

  @Test
  public void testFetchTablesInfoEmptyWithTableNames() {
    Assert.assertFalse(ServiceNowMultiInputFormat.fetchTablesInfo(serviceNowMultiSourceConfig).isEmpty());
  }
}
