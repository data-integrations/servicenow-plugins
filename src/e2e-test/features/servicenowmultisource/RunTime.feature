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

  @TS-SN-MULTI-RNTM-01 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify user should be able to preview the pipeline
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow Multi Source" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | HARDWARE_CATALOG | SOFTWARE_CATALOG | PRODUCT_CATALOG_ITEM | RECEIVING_SLIP_LINE |
    And fill Credentials section for pipeline user
    Then Validate "ServiceNow Multi Source" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryMultiTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQueryMultiTable" to establish connection
    And Navigate to the properties page of plugin: "BigQuery Multi Table"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery Multi Table" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Wait till pipeline preview is in running state with a timeout of 500 seconds
    Then Verify the preview of pipeline is "success"

  @TS-SN-MULTI-RNTM-02 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SINK
  Scenario: Verify user should be able to run the pipeline
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow Multi Source" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And configure ServiceNow Multi source plugin for below listed tables:
      | HARDWARE_CATALOG | SOFTWARE_CATALOG | PRODUCT_CATALOG_ITEM | RECEIVING_SLIP_LINE |
    And fill Credentials section for pipeline user
    Then Validate "ServiceNow Multi Source" plugin properties
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
