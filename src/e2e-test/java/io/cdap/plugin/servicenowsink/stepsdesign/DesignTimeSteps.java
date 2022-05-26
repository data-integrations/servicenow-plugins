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

package io.cdap.plugin.servicenowsink.stepsdesign;

import io.cdap.e2e.pages.locators.CdfSchemaLocators;
import io.cdap.e2e.utils.AssertionHelper;
import io.cdap.e2e.utils.PluginPropertyUtils;
import io.cdap.plugin.servicenow.actions.ServiceNowPropertiesPageActions;
import io.cdap.plugin.servicenow.source.ServiceNowSourceConfig;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenowsink.actions.ServiceNowSinkPropertiesPageActions;
import io.cdap.plugin.tests.hooks.TestSetupHooks;
import io.cucumber.java.en.Then;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
/**
 * ServiceNow sink - Properties page - Steps.
 */

public class DesignTimeSteps {

  @Then("Verify If new record is created in ServiceNow application")
  public void verifyIfRecordExistsinServiceNowTable() throws OAuthProblemException, OAuthSystemException {
    ServiceNowSinkPropertiesPageActions.verifyIfRecordExistsInServiecNowReceivingSlipLineTable(
      TestSetupHooks.receivingSlipLineRecordUniqueNumber);
 }
}
