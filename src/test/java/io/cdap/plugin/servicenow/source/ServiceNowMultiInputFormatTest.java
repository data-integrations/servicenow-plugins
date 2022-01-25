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

  @Before
  public void initializeTests() {
    try {
      Assume.assumeNotNull(CLIENT_ID, CLIENT_SECRET, REST_API_ENDPOINT, USER, PASSWORD);
    } catch (AssumptionViolatedException e) {
      LOG.warn("Service Now batch multi source tests are skipped. ");
      throw e;
    }
  }

  @Test
  public void testFetchTablesInfo() {
    ServiceNowMultiSourceConfig config =
      new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", CLIENT_ID, CLIENT_SECRET,
        REST_API_ENDPOINT, USER, PASSWORD, "Actual", "2021-12-30", "2021-12-31", "sys_user");
    ServiceNowColumn column1 = new ServiceNowColumn("sys_created_by", "string");
    ServiceNowColumn column2 = new ServiceNowColumn("sys_updated_by", "string");
    List<ServiceNowColumn> columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setColumns(columns);
    Assert.assertEquals(ServiceNowMultiInputFormat
      .fetchTablesInfo(config)
      .size(), 0);
  }

  @Test
  public void testFetchTablesInfoEmptyWithTableNames() {
    ServiceNowMultiSourceConfig config1 = new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42",
      "Client Secret",
      "https://dev115725.service-now.com", "User", "password", "Actual", "2021-12-30", "2021-12-31", ",");
    Assert.assertTrue(ServiceNowMultiInputFormat
      .fetchTablesInfo(config1)
      .isEmpty());

    ServiceNowMultiSourceConfig config2 = new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42",
      "Client " +
        "Secret",
      "https://dev115725.service-now.com", "User", "password", "Actual", "2021-12-30", "2021-12-31", "");

    Assert.assertTrue(ServiceNowMultiInputFormat
      .fetchTablesInfo(config2)
      .isEmpty());
  }
}
