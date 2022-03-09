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

package io.cdap.plugin.servicenow.stepsdesign;

import io.cdap.plugin.servicenow.actions.ServiceNowPropertiesPageActions;
import io.cdap.plugin.utils.enums.ApplicationInReportingMode;
import io.cdap.plugin.utils.enums.PluginMode;
import io.cdap.plugin.utils.enums.TablesInTableMode;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * ServiceNow batch source - Properties page - Steps.
 */
public class DesignTimeSteps {

  @When("Fill Reference Name")
  public void fillReferenceName() {
    String referenceName = "TestSN" + RandomStringUtils.randomAlphanumeric(7);
    ServiceNowPropertiesPageActions.fillReferenceName(referenceName);
  }

  @When("Select mode as: {string}")
  public void selectMode(String mode) {
    ServiceNowPropertiesPageActions.selectMode(PluginMode.valueOf(mode));
  }

  @When("configure ServiceNow source plugin for application: {string} in the Reporting mode")
  public void configureServiceNowSourceForApplication(String applicationName) {
    ServiceNowPropertiesPageActions
      .configurePluginForReportingMode(ApplicationInReportingMode.valueOf(applicationName));
  }

  @When("configure ServiceNow source plugin for table: {string} in the Table mode")
  public void configureServiceNowSourceForTable(String tableName) {
    ServiceNowPropertiesPageActions
      .configurePluginForTableMode(TablesInTableMode.valueOf(tableName));
  }

  @When("fill Credentials section for pipeline user")
  public void fillCredentialsSectionForPipelineUser() {
    ServiceNowPropertiesPageActions.fillCredentialsSectionForPipelineUser();
  }

  @When("fill Credentials section with invalid credentials")
  public void fillCredentialsSectionWithInvalidCredentials() {
    ServiceNowPropertiesPageActions.fillCredentialsSectionWithInvalidCredentials();
  }
}
