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

package io.cdap.plugin.servicenowsink.actions;

import io.cdap.e2e.utils.AssertionHelper;
import io.cdap.e2e.utils.PluginPropertyUtils;
import io.cdap.plugin.servicenow.source.ServiceNowSourceConfig;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.utils.enums.TablesInTableMode;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceNow sink - Properties page - Actions.
 */

public class ServiceNowSinkPropertiesPageActions {
  public static ServiceNowSourceConfig config;

  private static final Logger logger = LoggerFactory.getLogger(ServiceNowSinkPropertiesPageActions.class);



  public static void verifyIfRecordExistsInServiecNowReceivingSlipLineTable(String uniqueNumber)
    throws OAuthProblemException, OAuthSystemException {
    String query = "number=" + uniqueNumber;
    boolean responseStatusCodeIsSuccess;

    config = new ServiceNowSourceConfig(
      "", "", "", "", "",
      System.getenv("SERVICENOW_CLIENT_ID"),
      System.getenv("SERVICENOW_CLIENT_SECRET"),
      System.getenv("SERVICENOW_REST_API_ENDPOINT"),
      System.getenv("SERVICENOW_USERNAME"),
      System.getenv("SERVICENOW_PASSWORD"),
      "", "", "");

    ServiceNowTableAPIClientImpl tableAPIClient = new ServiceNowTableAPIClientImpl(config);
    responseStatusCodeIsSuccess = tableAPIClient.verifyIfRecordInServiceNowTableExists(
      TablesInTableMode.RECEIVING_SLIP_LINE.value, query);

    logger.info("Verifying that the Response Status Code is true: " + responseStatusCodeIsSuccess);
    Assert.assertTrue(responseStatusCodeIsSuccess);

  }
}
