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
# the License.

@ServiceNow
@SNSource
@Smoke
@Regression
Feature: ServiceNow Source - Design time scenarios

  @TS-SN-DSGN-01
  Scenario Outline: Verify user should be able to validate the plugin in Reporting mode
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
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
    And Select data pipeline type as: "Data Pipeline - Batch"
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
      | CONDITION           | schema.table.condition           |

  @TS-SN-DSGN-03
  Scenario: Verify user should be able to get Output Schema table with Date filters
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "CONDITION" in the Table mode
    And fill Credentials section for pipeline user
    And Enter input plugin property: "startDate" with value: "start.date"
    And Enter input plugin property: "endDate" with value: "end.date"
    Then Validate "ServiceNow" plugin properties
    And Verify the Output Schema matches the Expected Schema: "schema.table.condition"
