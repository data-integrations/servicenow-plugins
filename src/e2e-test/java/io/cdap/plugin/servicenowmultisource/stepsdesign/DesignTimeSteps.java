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

package io.cdap.plugin.servicenowmultisource.stepsdesign;

import io.cdap.plugin.servicenowmultisource.actions.ServiceNowMultiSourcePropertiesPageActions;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;

import java.util.List;

/**
 * ServiceNow Multi Source - Design time steps.
 */
public class DesignTimeSteps {
  @When("configure ServiceNow Multi source plugin for below listed tables:")
  public void configureServiceNowSourceForApplication(DataTable table) {
    List<String> tablesList = table.asList();
    ServiceNowMultiSourcePropertiesPageActions.configureServiceNowMultiSourcePlugin(tablesList);
  }
}
