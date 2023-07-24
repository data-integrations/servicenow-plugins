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
Feature: ServiceNow Multi Source - Run time scenarios

  @TS-SN-MULTI-RNTM-01 @BQ_SINK @Required
  Scenario: Verify user should be able to preview the pipeline
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow Multi Source" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | HARDWARE_CATALOG | RECEIVING_SLIP_LINE |
    And fill Credentials section for pipeline user
    Then Validate "ServiceNow Multi Source" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryMultiTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQueryMultiTable" to establish connection
    And Navigate to the properties page of plugin: "BigQuery Multi Table"
    And Configure BigQuery Multi Table sink plugin for Dataset
    Then Validate "BigQuery Multi Table" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Wait till pipeline preview is in running state with a timeout of 500 seconds
    Then Verify the preview of pipeline is "success"

  @TS-SN-MULTI-RNTM-02 @BQ_SINK
  Scenario: Verify user should be able to run the pipeline
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow Multi Source" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | RECEIVING_SLIP_LINE |
    And fill Credentials section for pipeline user
    Then Validate "ServiceNow Multi Source" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryMultiTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQueryMultiTable" to establish connection
    And Navigate to the properties page of plugin: "BigQuery Multi Table"
    And Replace input plugin property: "project" with value: "projectId"
    And Enter input plugin property: "datasetProject" with value: "datasetprojectId"
    And Configure BigQuery Multi Table sink plugin for Dataset
    Then Validate "BigQuery Multi Table" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running status with a timeout of 500 seconds
    And Open and capture logs
    And Verify the pipeline status is "Succeeded"

  @TS-SN-MULTI-RNTM-03 @CONNECTION @BQ_SINK
  Scenario: Verify user should be able to deploy and run the pipeline using connection manager functionality
    When Open Datafusion Project to configure pipeline
    And Select plugin: "ServiceNow Multi Source" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
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
    And configure ServiceNow Multi source plugin for below listed tables:
      | RECEIVING_SLIP_LINE |
    Then Validate "ServiceNow Multi Source" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryMultiTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQueryMultiTable" to establish connection
    And Navigate to the properties page of plugin: "BigQuery Multi Table"
    And Replace input plugin property: "project" with value: "projectId"
    And Enter input plugin property: "datasetProject" with value: "datasetprojectId"
    And Configure BigQuery Multi Table sink plugin for Dataset
    Then Validate "BigQuery Multi Table" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running status with a timeout of 500 seconds
    And Open and capture logs
    And Verify the pipeline status is "Succeeded"
