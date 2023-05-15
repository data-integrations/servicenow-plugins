# Copyright © 2022 Cask Data, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License..

@ServiceNow
@SNSource
@Smoke
@Regression
Feature: ServiceNow Source - Design time scenarios

  @TS-SN-DSGN-01
  Scenario Outline: Verify user should be able to validate the plugin in Reporting mode
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for application: "<ApplicationName>" in the Reporting mode
    And fill Credentials section for pipeline user
    Then Validate "ServiceNow" plugin properties
    Examples:
      | ApplicationName     |
      | CONTRACT_MANAGEMENT |
      | PRODUCT_CATALOG     |
      | PROCUREMENT         |

  @TS-SN-DSGN-02
  Scenario Outline: Verify user should be able to get Output Schema table for Table mode
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "<TableName>" in the Table mode
    And fill Credentials section for pipeline user
    Then Validate "ServiceNow" plugin properties
    And Verify the Output Schema matches the Expected Schema: "<ExpectedSchema>"
    Examples:
      | TableName           | ExpectedSchema                   |
      | RECEIVING_SLIP_LINE | schema.table.receiving.slip.line |
      | ASSET_COVERED       | schema.table.asset.covered       |

  @TS-SN-DSGN-03
  Scenario: Verify user should be able to get Output Schema table with Date filters
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "RECEIVING_SLIP_LINE" in the Table mode
    And fill Credentials section for pipeline user
    And Enter input plugin property: "startDate" with value: "start.date"
    And Enter input plugin property: "endDate" with value: "end.date"
    Then Validate "ServiceNow" plugin properties
    And Verify the Output Schema matches the Expected Schema: "schema.table.receiving.slip.line"

  @TS-SN-DSGN-04 @CONNECTION
  Scenario: Verify user should be able to create the valid connection using connection manager functionality
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Click plugin property: "switch-useConnection"
    And Click on the Browse Connections button
    And Click on the Add Connection button
    And Click plugin property: "connector-ServiceNow"
    And Enter input plugin property: "name" with value: "connection.name"
    And fill Credentials section for pipeline user
    Then Click on the Test Connection button
    And Verify the test connection is successful
