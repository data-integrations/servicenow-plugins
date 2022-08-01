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
Feature: ServiceNow Source - Run time scenarios

  @TS-SN-RNTM-1 @SN_SOURCE_CONFIG @SN_PRODUCT_CATALOG_ITEM @BQ_SINK
  Scenario: Verify user should be able to preview the pipeline where ServiceNow source is configured for Reporting mode
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for application: "PRODUCT_CATALOG" in the Reporting mode
    And fill Credentials section for pipeline user
    And Enter input plugin property: "startDate" with value: "start.date"
    And Enter input plugin property: "endDate" with value: "end.date"
    Then Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryMultiTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQueryMultiTable" to establish connection
    And Navigate to the properties page of plugin: "BigQuery Multi Table"
    And Configure BigQuery Multi Table sink plugin for Dataset
    Then Validate "BigQuery Multi Table" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Wait till pipeline preview is in running state with a timeout of 1000 seconds
    Then Verify the preview of pipeline is "success"
    And Click on the Preview Data link on the Sink plugin node: "BigQuery Multi Table"
    And Verify sink plugin's Preview Data for Input Records table and the Input Schema matches the Output Schema of Source plugin

  @TS-SN-RNTM-2 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify user should be able to preview the pipeline where ServiceNow source is configured for Table mode
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "RECEIVING_SLIP_LINE" in the Table mode
    And fill Credentials section for pipeline user
    And Enter input plugin property: "startDate" with value: "start.date"
    And Enter input plugin property: "endDate" with value: "end.date"
    Then Validate "ServiceNow" plugin properties
    And Capture the generated Output Schema
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQuery" to establish connection
    And Navigate to the properties page of plugin: "BigQuery"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Wait till pipeline preview is in running state
    Then Verify the preview of pipeline is "success"
    And Click on the Preview Data link on the Sink plugin node: "BigQueryTable"
    And Verify sink plugin's Preview Data for Input Records table and the Input Schema matches the Output Schema of Source plugin

  @TS-SN-RNTM-3 @SN_SOURCE_CONFIG @SN_PRODUCT_CATALOG_ITEM @BQ_SINK
  Scenario: Verify user should be able to deploy and run the pipeline where ServiceNow source is configured for Reporting mode
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for application: "PRODUCT_CATALOG" in the Reporting mode
    And fill Credentials section for pipeline user
    And Enter input plugin property: "startDate" with value: "start.date"
    And Enter input plugin property: "endDate" with value: "end.date"
    Then Validate "ServiceNow" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryMultiTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQueryMultiTable" to establish connection
    And Navigate to the properties page of plugin: "BigQuery Multi Table"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery Multi Table" plugin properties
    And Close the Plugin Properties page
    And Save and Deploy Pipeline
    And Run the Pipeline in Runtime
    And Wait till pipeline is in running status with a timeout of 500 seconds
    And Verify the pipeline status is "Succeeded"
    And Verify count of no of records transferred to the target BigQuery Table

  @TS-SN-RNTM-4 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify user should be able to deploy and run the pipeline where ServiceNow source is configured for Table mode
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow"
    And configure ServiceNow source plugin for table: "RECEIVING_SLIP_LINE" in the Table mode
    And fill Credentials section for pipeline user
    And Enter input plugin property: "startDate" with value: "start.date"
    And Enter input plugin property: "endDate" with value: "end.date"
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
    And Wait till pipeline is in running state
    And Verify the pipeline status is "Succeeded"
    And Verify count of no of records transferred to the target BigQuery Table
