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

package io.cdap.plugin.servicenow.locators;

import io.cdap.e2e.utils.SeleniumDriver;
import io.cdap.plugin.utils.enums.ServiceNowProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

/**
 * ServiceNow batch source - Properties page - Locators.
 */
public class ServiceNowPropertiesPage {

  @FindBy(how = How.XPATH, using = "//div[contains(@class, 'label-input-container')]//input")
  public static WebElement labelInput;

  // Basic section
  @FindBy(how = How.XPATH, using = "//input[@data-cy='referenceName']")
  public static WebElement referenceNameInput;

  @FindBy(how = How.XPATH, using = "//div[@data-cy='select-queryMode']")
  public static WebElement modeDropdown;

  // Reporting Mode section
  @FindBy(how = How.XPATH, using = "//div[@data-cy='select-applicationName']")
  public static WebElement applicationNameDropdown;

  @FindBy(how = How.XPATH, using = "//input[@data-cy='tableNameField']")
  public static WebElement tableNameFieldInput;

  // Table Mode section
  @FindBy(how = How.XPATH, using = "//input[@data-cy='tableName']")
  public static WebElement tableNameInput;

  // Credentials section
  @FindBy(how = How.XPATH, using = "//input[@data-cy='clientId']")
  public static WebElement clientIdInput;

  @FindBy(how = How.XPATH, using = "//input[@data-cy='clientSecret']")
  public static WebElement clientSecretInput;

  @FindBy(how = How.XPATH, using = "//input[@data-cy='restApiEndpoint']")
  public static WebElement restApiEndpointInput;

  @FindBy(how = How.XPATH, using = "//input[@data-cy='user']")
  public static WebElement usernameInput;

  @FindBy(how = How.XPATH, using = "//input[@data-cy='password']")
  public static WebElement passwordInput;

  // Advanced section
  @FindBy(how = How.XPATH, using = "//div[@data-cy='select-valueType']")
  public static WebElement typeOfValuesDropdown;

  @FindBy(how = How.XPATH, using = "//input[@data-cy='startDate']")
  public static WebElement startDateInput;

  @FindBy(how = How.XPATH, using = "//input[@data-cy='endDate']")
  public static WebElement endDateInput;

  @FindBy(how = How.XPATH, using = "//button[contains(@class, 'validate-btn')]")
  public static WebElement validateButton;

  @FindBy(how = How.XPATH, using = "//button[contains(@class, 'validate-btn')]//span[contains(@class, 'fa-spin')]")
  public static WebElement loadingSpinnerOnValidateButton;

  @FindBy(how = How.XPATH, using = "//span[contains(@class, 'text-success')]" +
    "[normalize-space(text())= 'No errors found.']")
  public static WebElement noErrorsFoundSuccessMessage;
  @FindBy(how = How.XPATH, using = "//h2[contains(text(), 'Errors')]" +
    "/following-sibling::div[contains(@class, 'text-danger')]//li")
  public static WebElement errorMessageOnHeader;

  public static WebElement getDropdownOptionElement(String option) {
    String xpath = "//li[@role='option'][normalize-space(text()) = '" + option + "']";
    return SeleniumDriver.getDriver().findElement(By.xpath(xpath));
  }

  public static WebElement getPropertyInlineErrorMessage(ServiceNowProperty propertyName) {
    String xpath = "//div[@data-cy='" + propertyName.dataCyAttributeValue + "']" +
      "/following-sibling::div[contains(@class, 'propertyError')]";
    return SeleniumDriver.getDriver().findElement(By.xpath(xpath));
  }
}
