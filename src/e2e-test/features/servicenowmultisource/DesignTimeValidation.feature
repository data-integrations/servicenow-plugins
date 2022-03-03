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
Feature: ServiceNow Multi Source - Design time validation scenarios

  @TS-SN-MULTI-DSGN-02
  Scenario: Verify required fields missing validation messages
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as 'Data Pipeline - Batch'
    And Select plugin: "ServiceNow Multi Source" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And click on the Validate button
    Then verify required fields missing validation message for below listed properties:
      | REFERENCE_NAME    |
      | TABLE_NAMES       |
      | CLIENT_ID         |
      | CLIENT_SECRET     |
      | REST_API_ENDPOINT |
      | USERNAME          |
      | PASSWORD          |

  @TS-SN-MULTI-DSGN-03
  Scenario: Verify validation message for invalid table name
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as 'Data Pipeline - Batch'
    And Select plugin: "ServiceNow Multi Source" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | INVALID_TABLE |
    And fill Credentials section for pipeline user
    And click on the Validate button
    Then verify validation message for invalid table names: "INVALID_TABLE"

  @TS-SN-MULTI-DSGN-13
  Scenario: Verify validation message for Start date and End date in invalid format
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as 'Data Pipeline - Batch'
    And Select plugin: "ServiceNow Multi Source" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | HARDWARE_CATALOG | SOFTWARE_CATALOG | PRODUCT_CATALOG_ITEM | VENDOR_CATALOG_ITEM |
    And fill Credentials section for pipeline user
    And fill Start Date in format: yyyy-MM-dd: "2013-JAN-01"
    And click on the Validate button
    Then verify validation message for invalid format of Start Date
    And fill Start Date in format: yyyy-MM-dd: "2013-01-01"
    And fill End Date in format: yyyy-MM-dd: "2021-DEC-31"
    And click on the Validate button
    Then verify validation message for invalid format of End Date
