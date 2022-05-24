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
Feature: ServiceNow Source - Run time scenarios (macro)

  @TS-SN-RNTM-MACRO-01 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify user should be able to preview a pipeline when ServiceNow plugin is configured with macros
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select mode as: "TABLE"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    Then Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQuery" to establish connection
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value "client.id" for key "clientId"
    And Enter runtime argument value "client.secret" for key "clientSecret"
    And Enter runtime argument value "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value "pipeline.user.username" for key "username"
    And Enter runtime argument value "pipeline.user.password" for key "password"
    Then Run the preview of pipeline with runtime arguments
    Then Verify the preview of pipeline is "success"

  @TS-SN-RNTM-MACRO-02 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify user should be able to run a pipeline when ServiceNow plugin is configured with macros
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select mode as: "TABLE"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    Then Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQuery" to establish connection
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value "client.id" for key "clientId"
    And Enter runtime argument value "client.secret" for key "clientSecret"
    And Enter runtime argument value "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value "pipeline.user.username" for key "username"
    And Enter runtime argument value "pipeline.user.password" for key "password"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Open and capture logs
    Then Verify the pipeline status is "Succeeded"

  @TS-SN-RNTM-MACRO-03 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify pipeline failure message in logs when user provides an invalid Table Name with Macros
    When Open Datafusion Project to configure pipeline
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select mode as: "TABLE"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And fill Credentials section for pipeline user
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryTable" from the plugins list
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Connect source as "ServiceNow" and sink as "BigQueryTable" to establish connection
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value "invalid.tablename" for key "tableName"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Verify the pipeline status is "Failed"
    Then Open Pipeline logs and verify Log entries having below listed Level and Message:
    | Level | Message                              |
    | ERROR | invalid.tablename.logsmessage        |

  @TS-SN-RNTM-MACRO-04 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify pipeline failure message in logs when user provides invalid Credentials with Macros
    When Open Datafusion Project to configure pipeline
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select mode as: "TABLE"
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryTable" from the plugins list
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Connect source as "ServiceNow" and sink as "BigQueryTable" to establish connection
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value "invalid.client.id" for key "clientId"
    And Enter runtime argument value "invalid.client.secret" for key "clientSecret"
    And Enter runtime argument value "invalid.rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value "invalid.pipeline.user.username" for key "username"
    And Enter runtime argument value "invalid.pipeline.user.password" for key "password"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Verify the pipeline status is "Failed"
    Then Open Pipeline logs and verify Log entries having below listed Level and Message:
      | Level | Message                                |
      | ERROR | invalid.credentials.logsmessage        |

  @TS-SN-RNTM-MACRO-05 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify pipeline failure message in logs when user provides invalid Advanced Properties with Macros
    When Open Datafusion Project to configure pipeline
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select mode as: "TABLE"
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And fill Credentials section for pipeline user
    And Click on the Macro button of Property: "startDate" and set the value to: "startDate"
    And Click on the Macro button of Property: "endDate" and set the value to: "endDate"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryTable" from the plugins list
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Connect source as "ServiceNow" and sink as "BigQueryTable" to establish connection
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value "invalid.start.date" for key "startDate"
    And Enter runtime argument value "invalid.end.date" for key "endDate"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Verify the pipeline status is "Failed"
    Then Open Pipeline logs and verify Log entries having below listed Level and Message:
      | Level | Message                                |
      | ERROR | invalid.filters.logsmessage            |

