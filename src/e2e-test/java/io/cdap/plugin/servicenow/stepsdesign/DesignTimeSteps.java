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

import io.cdap.e2e.utils.CdfHelper;
import io.cdap.plugin.servicenow.actions.ServiceNowPropertiesPageActions;
import io.cdap.plugin.utils.enums.ApplicationInReportingMode;
import io.cdap.plugin.utils.enums.ServiceNowProperty;
import io.cdap.plugin.utils.enums.TablesInTableMode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

/**
 * ServiceNow batch source - Properties page - Steps.
 */
public class DesignTimeSteps implements CdfHelper {
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

  @When("fill Start Date (in format: yyyy-MM-dd): {string}")
  public void fillStartDate(String startDate) {
    ServiceNowPropertiesPageActions.fillStartDate(startDate);
  }

  @When("fill End Date (in format: yyyy-MM-dd): {string}")
  public void fillEndDate(String endDate) {
    ServiceNowPropertiesPageActions.fillEndDate(endDate);
  }

  @When("click on the Validate button")
  public void clickOnValidateButton() {
    ServiceNowPropertiesPageActions.clickOnValidateButton();
  }

  @Then("verify the Output Schema table for application: {string} in the Reporting mode")
  public void verifyOutputSchemaTableForApplication(String applicationName) {
  }

  @Then("verify No errors found success message")
  public void verifyNoErrorsFoundSuccessMessage() {
    ServiceNowPropertiesPageActions.verifyNoErrorsFoundSuccessMessage();
  }

  @Then("verify required fields missing validation message for below listed properties:")
  public void verifyRequiredFieldsMissingValidationMessage(DataTable table) {
    List<String> list = table.asList();

    for (String propertyName : list) {
      ServiceNowPropertiesPageActions
        .verifyRequiredFieldsMissingValidationMessage(ServiceNowProperty.valueOf(propertyName));
    }
  }

  @Then("verify invalid credentials validation message for below listed properties:")
  public void verifyInvalidCredentialsValidationMessage(DataTable table) {
    List<String> list = table.asList();

    for (String propertyName : list) {
      ServiceNowPropertiesPageActions
        .verifyInvalidCredentialsValidationMessage(ServiceNowProperty.valueOf(propertyName));
    }
  }

  @Then("verify validation message for invalid table name: {string}")
  public void verifyNoErrorsFoundSuccessMessage(String tableName) {
    ServiceNowPropertiesPageActions.verifyInvalidTableNameValidationMessage(TablesInTableMode.valueOf(tableName));
  }

  @Then("verify validation message for invalid format of Start Date")
  public void verifyValidationMessageOfInvalidStartDate() {
    ServiceNowPropertiesPageActions.verifyValidationMessageForInvalidFormatOfStartDate();
  }

  @Then("verify validation message for invalid format of End Date")
  public void verifyValidationMessageOfInvalidEndDate() {
    ServiceNowPropertiesPageActions.verifyValidationMessageForInvalidFormatOfEndDate();
  }
}
