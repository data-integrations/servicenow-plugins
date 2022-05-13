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

import io.cdap.e2e.pages.locators.CdfPluginPropertiesLocators;
import io.cdap.e2e.utils.ElementHelper;
import io.cdap.e2e.utils.PluginPropertyUtils;
import io.cdap.e2e.utils.SeleniumHelper;
import io.cdap.plugin.servicenow.locators.ServiceNowPropertiesPage;
import io.cdap.plugin.utils.enums.ApplicationInReportingMode;
import io.cdap.plugin.utils.enums.PluginMode;
import io.cdap.plugin.utils.enums.TablesInTableMode;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * ServiceNow batch source - Properties page - Actions.
 */
public class ServiceNowPropertiesPageActions {

  static {
    SeleniumHelper.getPropertiesLocators(ServiceNowPropertiesPage.class);
  }

  public static void fillReferenceName(String referenceName) {
    ElementHelper.sendKeys(ServiceNowPropertiesPage.referenceNameInput, referenceName);
  }

  public static void selectMode(PluginMode mode) {
    ElementHelper.selectDropdownOption(
      ServiceNowPropertiesPage.modeDropdown, CdfPluginPropertiesLocators.locateDropdownListItem(mode.value));
  }

  public static void selectApplicationName(ApplicationInReportingMode applicationName) {
    ElementHelper.selectDropdownOption(
      ServiceNowPropertiesPage.applicationNameDropdown,
      CdfPluginPropertiesLocators.locateDropdownListItem(applicationName.value));
  }

  public static void fillTableNameFieldPropertyInReportingMode(String tableName) {
    ElementHelper.sendKeys(ServiceNowPropertiesPage.tableNameFieldInput, tableName);
  }

  public static void fillTableNamePropertyInTableMode(TablesInTableMode tableName) {
    ElementHelper.sendKeys(ServiceNowPropertiesPage.tableNameInput, tableName.value);
  }

  public static void fillCredentialsSectionProperties(String clientId, String clientSecret, String restApiEndpoint,
                                                      String username, String password) {
    ElementHelper.sendKeys(ServiceNowPropertiesPage.clientIdInput, clientId);
    ElementHelper.sendKeys(ServiceNowPropertiesPage.clientSecretInput, clientSecret);
    ElementHelper.sendKeys(ServiceNowPropertiesPage.restApiEndpointInput, restApiEndpoint);
    ElementHelper.sendKeys(ServiceNowPropertiesPage.usernameInput, username);
    ElementHelper.sendKeys(ServiceNowPropertiesPage.passwordInput, password);
  }

  public static void fillCredentialsSectionForPipelineUser() {
    ServiceNowPropertiesPageActions.fillCredentialsSectionProperties(
      System.getenv("CLIENT_ID"),
      System.getenv("CLIENT_SECRET"),
      System.getenv("REST_API_ENDPOINT"),
      System.getenv("SERVICENOW_USERNAME"),
      System.getenv("SERVICENOW_PASSWORD")
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
    ElementHelper.selectDropdownOption(
      ServiceNowPropertiesPage.typeOfValuesDropdown, CdfPluginPropertiesLocators.locateDropdownListItem(typeOfValue));
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
}
