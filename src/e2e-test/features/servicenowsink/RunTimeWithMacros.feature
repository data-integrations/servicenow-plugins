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
Feature: ServiceNow Sink - Run time scenarios (macro)

  @TS-SN-RNTM-SINK-MACRO-01 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to preview a pipeline when plugin is configured for Insert operation with macros
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value from environment variable "client.id" for key "clientId"
    And Enter runtime argument value from environment variable "client.secret" for key "clientSecret"
    And Enter runtime argument value from environment variable "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value from environment variable "pipeline.user.username" for key "username"
    And Enter runtime argument value from environment variable "pipeline.user.password" for key "password"
    And Run the preview of pipeline with runtime arguments
    Then Verify the preview of pipeline is "success"
    And Open and capture pipeline preview logs

  @TS-SN-RNTM-SINK-MACRO-02  @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to deploy and run a pipeline when plugin is configured for Insert operation with macros
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value from environment variable "client.id" for key "clientId"
    And Enter runtime argument value from environment variable "client.secret" for key "clientSecret"
    And Enter runtime argument value from environment variable "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value from environment variable "pipeline.user.username" for key "username"
    And Enter runtime argument value from environment variable "pipeline.user.password" for key "password"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Verify the pipeline status is "Succeeded"
    And Open and capture logs
    Then Verify If new record created in ServiceNow application for table "receiving_slip_line" is correct


  @TS-SN-RNTM-SINK-MACRO-03 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SOURCE_UPDATE_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to preview a pipeline when plugin is configured for Update operation with macros
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value from environment variable "client.id" for key "clientId"
    And Enter runtime argument value from environment variable "client.secret" for key "clientSecret"
    And Enter runtime argument value from environment variable "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value from environment variable "pipeline.user.username" for key "username"
    And Enter runtime argument value from environment variable "pipeline.user.password" for key "password"
    And Run the preview of pipeline with runtime arguments
    And Verify the preview of pipeline is "success"
    Then Verify If an updated record in ServiceNow application for table "receiving_slip_line" is correct


  @TS-SN-RNTM-SINK-MACRO-04 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SOURCE_UPDATE_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to deploy and run a pipeline when plugin is configured for Update operation with macros
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value from environment variable "client.id" for key "clientId"
    And Enter runtime argument value from environment variable "client.secret" for key "clientSecret"
    And Enter runtime argument value from environment variable "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value from environment variable "pipeline.user.username" for key "username"
    And Enter runtime argument value from environment variable "pipeline.user.password" for key "password"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Verify the pipeline status is "Succeeded"
    Then Open and capture logs

  @TS-SN-RNTM-SINK-MACRO-06 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP
  Scenario: Verify pipeline failure message in logs when user provides invalid Credentials with Macros
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    Then Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value from environment variable "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value "invalid.client.id" for key "clientId"
    And Enter runtime argument value "invalid.client.secret" for key "clientSecret"
    And Enter runtime argument value "invalid.pipeline.user.username" for key "username"
    And Enter runtime argument value "invalid.pipeline.user.password" for key "password"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Verify the pipeline status is "Failed"
    Then Open Pipeline logs and verify Log entries having below listed Level and Message:
      | Level | Message                                   |
      | ERROR | invalid.credentials.logsmessage           |

  @TS-SN-RNTM-SINK-MACRO-01 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP @CONNECTION
  Scenario: Verify user should be able to preview a pipeline when plugin is configured for Insert operation with macros for connection manager
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Click on the Macro button of Property: "tableName" and set the value to: "tableName"
    And Click on the Macro button of Property: "connection" and set the value to: "Connection"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value "connectionMacros" for key "Connection"
    And Run the preview of pipeline with runtime arguments
    Then Verify the preview of pipeline is "success"
    And Open and capture pipeline preview logs
    And Close the pipeline logs
    And Close the preview
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Enter runtime argument value "receiving_slip_line" for key "tableName"
    And Enter runtime argument value "connectionMacros" for key "Connection"
    And Run the Pipeline in Runtime with runtime arguments
    And Wait till pipeline is in running state
    And Open and capture logs
    And Verify the pipeline status is "Succeeded"
    And Close the pipeline logs

