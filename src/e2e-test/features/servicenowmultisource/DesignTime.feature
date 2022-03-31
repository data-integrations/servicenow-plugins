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
@SNMultiSource
@Smoke
@Regression
Feature: ServiceNow Multi Source - Design time scenarios

  @TS-SN-MULTI-DSGN-06
  Scenario: Verify user should be able to get Output Schema table for tables of the same application
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as 'Data Pipeline - Batch'
    And Select plugin: "ServiceNow Multi Source" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | HARDWARE_CATALOG | SOFTWARE_CATALOG | PRODUCT_CATALOG_ITEM | VENDOR_CATALOG_ITEM |
    And fill Credentials section for pipeline user
    And click on the Validate button
    Then verify No errors found success message

  @TS-SN-MULTI-DSGN-12
  Scenario: Verify user should be able to get Output Schema table for tables of different application
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as 'Data Pipeline - Batch'
    And Select plugin: "ServiceNow Multi Source" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | ASSET_COVERED | CONDITION | PRODUCT_CATALOG_ITEM | PURCHASE_ORDER | PURCHASE_ORDER_LINE_ITEMS |
    And fill Credentials section for pipeline user
    And click on the Validate button
    Then verify No errors found success message

  @TS-SN-MULTI-DSGN-10
  Scenario: Verify user should be able to get Output Schema table with Date filters
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as 'Data Pipeline - Batch'
    And Select plugin: "ServiceNow Multi Source" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | HARDWARE_CATALOG | SOFTWARE_CATALOG | PRODUCT_CATALOG_ITEM | VENDOR_CATALOG_ITEM |
    And fill Credentials section for pipeline user
    And fill Start Date in format: yyyy-MM-dd: "2013-01-01"
    And fill End Date in format: yyyy-MM-dd: "2021-12-31"
    And click on the Validate button
    Then verify No errors found success message
