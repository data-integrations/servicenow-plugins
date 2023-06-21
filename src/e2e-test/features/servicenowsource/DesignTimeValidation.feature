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
Feature: ServiceNow Source - Design time validation scenarios

  @TS-SN-DSGN-ERROR-01
  Scenario: Verify required fields missing validation messages
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Click on the Validate button
    Then Verify mandatory property error for below listed properties:
      | referenceName   |

  @TS-SN-DSGN-ERROR-02
  Scenario: Verify invalid credentials validation messages
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "HARDWARE_CATALOG" in the Table mode
    And fill Credentials section with invalid credentials
    And Click on the Validate button
    Then Verify invalid credentials validation message for below listed properties:
      | clientId        |
      | clientSecret    |
      | restApiEndpoint |
      | user            |
      | password        |

  @TS-SN-DSGN-ERROR-03
  Scenario: Verify validation message for invalid table name
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "INVALID_TABLE" in the Table mode
    And fill Credentials section for pipeline user
    And Click on the Validate button
    Then Verify that the Plugin Property: "tableName" is displaying an in-line error message: "invalid.property.tablename"

  @TS-SN-DSGN-ERROR-04
  Scenario: Verify validation message for Start date and End date in invalid format
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "HARDWARE_CATALOG" in the Table mode
    And fill Credentials section for pipeline user
    And Enter input plugin property: "startDate" with value: "2020-JAN-01"
    And Click on the Validate button
    Then Verify that the Plugin Property: "startDate" is displaying an in-line error message: "invalid.property.startdate"
    And Enter input plugin property: "startDate" with value: "start.date"
    And Enter input plugin property: "endDate" with value: "2021-DEC-31"
    And Click on the Validate button
    Then Verify that the Plugin Property: "endDate" is displaying an in-line error message: "invalid.property.enddate"

  @TS-SN-DSGN-05 @CONNECTION
  Scenario: Verify user should be able to get invalid credentials validation message when using invalid credentials in the connection manager functionality
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Click plugin property: "switch-useConnection"
    And Click on the Browse Connections button
    And Click on the Add Connection button
    And Click plugin property: "connector-ServiceNow"
    And Enter input plugin property: "name" with value: "connection.name"
    And fill Credentials section with invalid credentials
    Then Click on the Test Connection button
    Then Verify the invalid connection error message: "invalid.testconnection.logmessage" on the footer
