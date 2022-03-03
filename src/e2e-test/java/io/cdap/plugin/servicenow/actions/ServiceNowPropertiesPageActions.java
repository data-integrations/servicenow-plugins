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

package io.cdap.plugin.servicenow.actions;

import io.cdap.e2e.utils.AssertionHelper;
import io.cdap.e2e.utils.ElementHelper;
import io.cdap.e2e.utils.PluginPropertyUtils;
import io.cdap.e2e.utils.SeleniumHelper;
import io.cdap.e2e.utils.WaitHelper;
import io.cdap.plugin.servicenow.locators.ServiceNowPropertiesPage;
import io.cdap.plugin.utils.enums.ApplicationInReportingMode;
import io.cdap.plugin.utils.enums.PluginMode;
import io.cdap.plugin.utils.enums.ServiceNowProperty;
import io.cdap.plugin.utils.enums.TablesInTableMode;
import org.apache.commons.lang3.RandomStringUtils;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceNow batch source - Properties page - Actions.
 */
public class ServiceNowPropertiesPageActions {
  private static final Logger logger = LoggerFactory.getLogger(ServiceNowPropertiesPageActions.class);

  static {
    SeleniumHelper.getPropertiesLocators(ServiceNowPropertiesPage.class);
  }

  public static void fillReferenceName(String referenceName) {
    logger.info("Fill Reference name: " + referenceName);
    ServiceNowPropertiesPage.referenceNameInput.sendKeys(referenceName);
  }

  public static void selectMode(PluginMode mode) {
    logger.info("Select Mode: " + mode.value);
    ServiceNowPropertiesPage.modeDropdown.click();
    ServiceNowPropertiesPage.getDropdownOptionElement(mode.value).click();
  }

  public static void selectApplicationName(ApplicationInReportingMode applicationName) {
    logger.info("Select Application: " + applicationName.value);
    ServiceNowPropertiesPage.applicationNameDropdown.click();
    ServiceNowPropertiesPage.getDropdownOptionElement(applicationName.value).click();
  }

  public static void fillTableNameFieldPropertyInReportingMode(String tableName) {
    logger.info("Fill 'Table Name Field' property in Reporting mode with value: " + tableName);
    ServiceNowPropertiesPage.tableNameFieldInput.sendKeys(tableName);
  }

  public static void fillTableNamePropertyInTableMode(TablesInTableMode tableName) {
    logger.info("Fill 'Table Name' property in Table mode with value: " + tableName.value);
    ServiceNowPropertiesPage.tableNameInput.sendKeys(tableName.value);
  }

  public static void fillCredentialsSectionProperties(String clientId, String clientSecret, String restApiEndpoint,
                                                      String username, String password) {
    logger.info("Fill Credentials section properties");
    ServiceNowPropertiesPage.clientIdInput.sendKeys(clientId);
    ServiceNowPropertiesPage.clientSecretInput.sendKeys(clientSecret);
    ServiceNowPropertiesPage.restApiEndpointInput.sendKeys(restApiEndpoint);
    ServiceNowPropertiesPage.usernameInput.sendKeys(username);
    ServiceNowPropertiesPage.passwordInput.sendKeys(password);
  }

  public static void fillCredentialsSectionForPipelineUser() {
    ServiceNowPropertiesPageActions.fillCredentialsSectionProperties(
      PluginPropertyUtils.pluginProp("client.id"),
      PluginPropertyUtils.pluginProp("client.secret"),
      PluginPropertyUtils.pluginProp("rest.api.endpoint"),
      PluginPropertyUtils.pluginProp("pipeline.user.username"),
      PluginPropertyUtils.pluginProp("pipeline.user.password")
    );
  }

  public static void fillCredentialsSectionWithInvalidCredentials() {
    ServiceNowPropertiesPageActions.fillCredentialsSectionProperties(
      PluginPropertyUtils.pluginProp("invalid.client.id"),
      PluginPropertyUtils.pluginProp("invalid.client.secret"),
      PluginPropertyUtils.pluginProp("invalid.rest.api.endpoint"),
      PluginPropertyUtils.pluginProp("invalid.pipeline.user.username"),
      PluginPropertyUtils.pluginProp("invalid.pipeline.user.password")
    );
  }

  public static void selectTypeOfValues(String typeOfValue) {
    logger.info("Select 'Type of values' dropdown option: " + typeOfValue);
    ServiceNowPropertiesPage.typeOfValuesDropdown.click();
    ServiceNowPropertiesPage.getDropdownOptionElement(typeOfValue).click();
  }

  public static void fillStartDate(String startDate) {
    logger.info("Fill Start Date: " + startDate);
    ElementHelper.clearElementValue(ServiceNowPropertiesPage.startDateInput);
    ServiceNowPropertiesPage.startDateInput.sendKeys(startDate);
  }

  public static void fillEndDate(String endDate) {
    logger.info("Fill End Date: " + endDate);
    ElementHelper.clearElementValue(ServiceNowPropertiesPage.endDateInput);
    ServiceNowPropertiesPage.endDateInput.sendKeys(endDate);
  }

  public static void configurePluginForReportingMode(ApplicationInReportingMode applicationName) {
    String referenceName = "TestSN" + RandomStringUtils.randomAlphanumeric(7);
    fillReferenceName(referenceName);
    selectMode(PluginMode.REPORTING);
    selectApplicationName(applicationName);
  }

  public static void configurePluginForTableMode(TablesInTableMode tableName) {
    String referenceName = "TestSN" + RandomStringUtils.randomAlphanumeric(7);
    fillReferenceName(referenceName);
    selectMode(PluginMode.TABLE);
    fillTableNamePropertyInTableMode(tableName);
  }

  public static void clickOnValidateButton() {
    logger.info("Click on the Validate button");
    ServiceNowPropertiesPage.validateButton.click();
    WaitHelper.waitForElementToBeDisplayed(ServiceNowPropertiesPage.loadingSpinnerOnValidateButton);
    WaitHelper.waitForElementToBeHidden(ServiceNowPropertiesPage.loadingSpinnerOnValidateButton);
  }

  public static void verifyNoErrorsFoundSuccessMessage() {
    AssertionHelper.verifyElementDisplayed(ServiceNowPropertiesPage.noErrorsFoundSuccessMessage);
  }

  public static void verifyPropertyInlineErrorMessage(ServiceNowProperty property,
                                                      String expectedErrorMessage) {
    WebElement element = ServiceNowPropertiesPage.getPropertyInlineErrorMessage(property);

    AssertionHelper.verifyElementDisplayed(element);
    AssertionHelper.verifyElementContainsText(element, expectedErrorMessage);
  }

  public static void verifyRequiredFieldsMissingValidationMessage(ServiceNowProperty propertyName) {
    verifyPropertyInlineErrorMessage(propertyName, propertyName.propertyMissingValidationMessage);
  }

  public static void verifyInvalidCredentialsValidationMessage(ServiceNowProperty propertyName) {
    verifyPropertyInlineErrorMessage(propertyName, PluginPropertyUtils.errorProp("invalid.property.credentials"));
  }

  public static void verifyErrorMessageOnHeader(String expectedErrorMessage) {
    AssertionHelper.verifyElementContainsText(ServiceNowPropertiesPage.errorMessageOnHeader, expectedErrorMessage);
  }

  public static void verifyInvalidTableNameValidationMessage(TablesInTableMode tableName) {
    String expectedMessage = PluginPropertyUtils.errorProp("invalid.property.tablename") +
      " " + tableName.value + " is invalid.";
    verifyErrorMessageOnHeader(expectedMessage);
  }

  public static void verifyValidationMessageForInvalidFormatOfStartDate() {
    verifyPropertyInlineErrorMessage(
      ServiceNowProperty.START_DATE,
      PluginPropertyUtils.errorProp("invalid.property.startdate"));
  }

  public static void verifyValidationMessageForInvalidFormatOfEndDate() {
    verifyPropertyInlineErrorMessage(
      ServiceNowProperty.END_DATE,
      PluginPropertyUtils.errorProp("invalid.property.enddate"));
  }
}
