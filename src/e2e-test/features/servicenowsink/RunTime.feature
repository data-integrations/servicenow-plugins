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
Feature: ServiceNow Sink - Run time scenarios

  @TS-SN-RNTM-SINK-01 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE
  Scenario: Verify user should be able to preview the pipeline when plugin is configured for Insert operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    Then Verify the preview of pipeline is "success"
    And Click on the Preview Data link on the Sink plugin node: "ServiceNow"
    And Verify sink plugin's Preview Data for Input Records table and the Input Schema matches the Output Schema of Source plugin

  @TS-SN-RNTM-SINK-02 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to deploy and run the pipeline when plugin is configured for table Receiving Slip with Input operation
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
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running state
    And Open and capture logs
    Then Verify the pipeline status is "Succeeded"
    Then Verify If new record created in ServiceNow application for table "receiving_slip_line" is correct

  @TS-SN-RNTM-SINK-03 @BQ_SOURCE_AGENT_ASSIST_RECOMMENDATION @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to deploy and run the pipeline when plugin is configured for table Agent Assist recommendation with Input operation
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
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "agent_assist_recommendation"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running state
    And Open and capture logs
    And Verify the pipeline status is "Succeeded"
    Then Verify If new record created in ServiceNow application for table "agent_assist_recommendation" is correct

  @TS-SN-RNTM-SINK-04 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SOURCE_UPDATE_RECEIVING_SLIP_LINE @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to deploy and run the pipeline when plugin is configured for table Receiving Slip with Update operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running state
    And Open and capture logs
    And Close the pipeline logs
    Then Verify If an updated record in ServiceNow application for table "receiving_slip_line" is correct

  @TS-SN-RNTM-SINK-05 @SN_UPDATE_AGENT_ASSIST_RECOMMENDATION @SN_SOURCE_CONFIG @BQ_SOURCE_UPDATE_AGENT_ASSIST_RECOMMENDATION @BQ_SINK_CLEANUP
  Scenario: Verify user should be able to deploy and run the pipeline when plugin is configured for table Agent Assist Recommendation with Update operation
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
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "agent_assist_recommendation"
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running state
    And Open and capture logs
    And Verify the pipeline status is "Succeeded"
    Then Verify If an updated record in ServiceNow application for table "agent_assist_recommendation" is correct

  @TS-SN-RNTM-SINK-06 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE @CONNECTION
  Scenario: Verify user should be able to deploy and run the pipeline using connection manager functionality
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Replace input plugin property: "project" with value: "projectId"
    And Enter input plugin property: "datasetProject" with value: "datasetprojectId"
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Click plugin property: "switch-useConnection"
    And Click on the Browse Connections button
    And Click on the Add Connection button
    And Click plugin property: "connector-ServiceNow"
    And Enter input plugin property: "name" with value: "connection.name"
    And fill Credentials section for pipeline user
    Then Click on the Test Connection button
    And Verify the test connection is successful
    Then Click on the Create button
    And Use new connection
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Enter input plugin property: "referenceName" with value: "test"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running state
    And Open and capture logs
    Then Verify the pipeline status is "Succeeded"
    Then Verify If new record created in ServiceNow application for table "receiving_slip_line" is correct
