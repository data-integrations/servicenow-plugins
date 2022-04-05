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

package io.cdap.plugin.tests.hooks;

import com.google.cloud.bigquery.BigQueryException;
import io.cdap.e2e.utils.BigQueryClient;
import io.cdap.e2e.utils.PluginPropertyUtils;
import io.cdap.plugin.servicenow.source.ServiceNowSourceConfig;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.utils.enums.TablesInTableMode;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import stepsdesign.BeforeActions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Represents Test Setup and Clean up hooks.
 */
public class TestSetupHooks {
  public static ServiceNowSourceConfig config;
  public static String bqTargetDataset = StringUtils.EMPTY;
  public static String bqTargetTable = StringUtils.EMPTY;

  @Before(order = 1, value = "@SN_SOURCE_CONFIG")
  public static void initializeServiceNowSourceConfig() {
    BeforeActions.scenario.write("Initialize ServiceNowSourceConfig");
    config = new ServiceNowSourceConfig(
      "", "", "", "", "",
      PluginPropertyUtils.pluginProp("client.id"),
      PluginPropertyUtils.pluginProp("client.secret"),
      PluginPropertyUtils.pluginProp("rest.api.endpoint"),
      PluginPropertyUtils.pluginProp("pipeline.user.username"),
      PluginPropertyUtils.pluginProp("pipeline.user.password"),
      "", "", "");
  }

  @Before(order = 2, value = "@SN_PRODUCT_CATALOG_ITEM")
  public static void createRecordInProductCatalogItemTable() throws UnsupportedEncodingException {
    BeforeActions.scenario.write("Create new record in Product Catalog Item table");
    ServiceNowTableAPIClientImpl tableAPIClient = new ServiceNowTableAPIClientImpl(config);
    String uniqueId = "TestProductCatalogItem" + RandomStringUtils.randomAlphanumeric(10);
    String recordDetails = "{'name':'" + uniqueId + "','price':'2500'}";
    StringEntity entity = new StringEntity(recordDetails);
    tableAPIClient.createRecord(TablesInTableMode.PRODUCT_CATALOG_ITEM.value, entity);
  }

  @Before(order = 3, value = "@SN_RECEIVING_SLIP_LINE")
  public static void createRecordInReceivingSlipLineTable() throws UnsupportedEncodingException {
    BeforeActions.scenario.write("Create new record in Receiving Slip Line table");
    ServiceNowTableAPIClientImpl tableAPIClient = new ServiceNowTableAPIClientImpl(config);
    String uniqueId = "TestReceivingSlipLine" + RandomStringUtils.randomAlphanumeric(10);
    String recordDetails = "{'cost':'100','quantity':'5','number':'" + uniqueId + "'}";
    StringEntity entity = new StringEntity(recordDetails);
    tableAPIClient.createRecord(TablesInTableMode.RECEIVING_SLIP_LINE.value, entity);
  }

  @Before(order = 4, value = "@BQ_SINK")
  public static void setTempTargetBQDataset() {
    bqTargetDataset = "TestSN_dataset" + RandomStringUtils.randomAlphanumeric(10);
    BeforeActions.scenario.write("BigQuery Target dataset name: " + bqTargetDataset);
  }

  @Before(order = 5, value = "@BQ_SINK")
  public static void setTempTargetBQTable() {
    bqTargetTable = "TestSN_table" + RandomStringUtils.randomAlphanumeric(10);
    BeforeActions.scenario.write("BigQuery Target table name: " + bqTargetTable);
  }

  @After(order = 1, value = "@BQ_SINK_CLEANUP")
  public static void deleteTempTargetBQTable() throws IOException, InterruptedException {
    try {
      BigQueryClient.dropBqQuery(bqTargetDataset, bqTargetTable);
      BeforeActions.scenario.write("BigQuery Target table: " + bqTargetTable + " is deleted successfully");
      bqTargetTable = StringUtils.EMPTY;
    } catch (BigQueryException e) {
      if (e.getMessage().contains("Not found: Table")) {
        BeforeActions.scenario.write("BigQuery Target Table: " + bqTargetTable + " does not exist");
      } else {
        Assert.fail(e.getMessage());
      }
    }
  }
}
