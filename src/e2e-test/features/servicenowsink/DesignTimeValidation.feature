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
@SNSink
@Smoke
@Regression
Feature: ServiceNow Sink - Design time validation scenarios

  @TS-SN-DSGN-SINK-ERROR-01
  Scenario: Verify required fields missing validation messages
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow"
    And Click on the Validate button
    Then Verify mandatory property error for below listed properties:
      | referenceName   |
      | clientId        |
      | clientSecret    |
      | restApiEndpoint |
      | user            |
      | password        |

  @TS-SN-DSGN-SINK-ERROR-02
  Scenario: Verify invalid credentials validation messages
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And fill Credentials section with invalid credentials
    And Click on the Validate button
    Then Verify invalid credentials validation message for below listed properties:
      | clientId        |
      | clientSecret    |
      | restApiEndpoint |
      | user            |
      | password        |

  @TS-SN-DSGN-SINK-ERROR-03
  Scenario: Verify validation message for invalid table name
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow"
    And Enter input plugin property: "tableName" with value: "invalid.tablename"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Click on the Validate button
    Then Verify that the Plugin Property: "tableName" is displaying an in-line error message: "invalid.property.tablename"
