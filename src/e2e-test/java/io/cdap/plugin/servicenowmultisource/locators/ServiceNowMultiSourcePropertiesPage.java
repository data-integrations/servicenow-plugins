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

package io.cdap.plugin.servicenowmultisource.locators;

import io.cdap.e2e.utils.SeleniumDriver;
import io.cdap.plugin.utils.enums.ServiceNowProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import java.util.List;

/**
 * ServiceNow Multi Source - Properties page.
 */
public class ServiceNowMultiSourcePropertiesPage {
  // Properties tab
  @FindBy(how = How.XPATH, using = "//div[contains(@class, 'label-input-container')]//input")
  public static WebElement labelInput;

  // Basic section
  @FindBy(how = How.XPATH, using = "//input[@data-cy='referenceName']")
  public static WebElement referenceNameInput;

  // Table specification section
  @FindBy(how = How.XPATH, using = "//div[@data-cy='tableNames']//input")
  public static List<WebElement> tableNamesInputs;

  @FindBy(how = How.XPATH, using = "//div[@data-cy='tableNames']//button[@data-cy='add-row']")
  public static List<WebElement> addRowButtonInTableNamesField;

  @FindBy(how = How.XPATH, using = "//div[@data-cy='tableNames']//button[@data-cy='remove-row']")
  public static List<WebElement> removeRowButtonInTableNamesField;

  // Advanced section
  @FindBy(how = How.XPATH, using = "//input[@data-cy='tableNameField']")
  public static WebElement tableNameFieldInput;

  public static WebElement getPropertyInlineErrorMessage(ServiceNowProperty propertyName) {
    String xpath = "//div[@data-cy='" + propertyName.dataCyAttributeValue + "']" +
      "/following-sibling::div[contains(@class, 'propertyError')]";
    return SeleniumDriver.getDriver().findElement(By.xpath(xpath));
  }
}
