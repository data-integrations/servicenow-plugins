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

package io.cdap.plugin.servicenowmultisource.actions;

import io.cdap.e2e.utils.ElementHelper;
import io.cdap.e2e.utils.SeleniumHelper;
import io.cdap.plugin.servicenowmultisource.locators.ServiceNowMultiSourcePropertiesPage;
import io.cdap.plugin.utils.enums.TablesInTableMode;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ServiceNow Multi Source - Properties page - Actions.
 */
public class ServiceNowMultiSourcePropertiesPageActions {

  static {
    SeleniumHelper.getPropertiesLocators(ServiceNowMultiSourcePropertiesPage.class);
  }

  public static void fillReferenceName(String referenceName) {
    ElementHelper.sendKeys(ServiceNowMultiSourcePropertiesPage.referenceNameInput, referenceName);
  }

  public static void fillTableNamesInTableSpecificationSection(List<TablesInTableMode> tablesList) {
    for (int i = 0; i < tablesList.size() - 1; i++) {
      ElementHelper.clickOnElement(ServiceNowMultiSourcePropertiesPage.addRowButtonInTableNamesField.get(i));
    }

    for (int i = 0; i < tablesList.size(); i++) {
      ElementHelper.sendKeys(ServiceNowMultiSourcePropertiesPage.tableNamesInputs.get(i), tablesList.get(i).value);
    }
  }

  public static void configureServiceNowMultiSourcePlugin(List<String> tablesList) {
    String referenceName = "TestSN" + RandomStringUtils.randomAlphanumeric(7);
    List<TablesInTableMode> tables = new ArrayList<>();

    for (String table : tablesList) {
      tables.add(TablesInTableMode.valueOf(table));
    }

    fillReferenceName(referenceName);
    fillTableNamesInTableSpecificationSection(tables);
  }
}
