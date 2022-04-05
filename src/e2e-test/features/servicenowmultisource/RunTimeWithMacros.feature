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
Feature: ServiceNow Multi Source - Run time scenarios (macro)

  @TS-SN-MULTI-RNTM-MACRO-01
  Scenario: Verify user should be able to preview the pipeline when the source plugin is configured with macros
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow Multi Source" from the plugins list as: "source"
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And Fill Reference Name
    And Click on the Macro button of Property: "tableNames" and set the value to: "tableNames"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
    Then Validate "ServiceNow Multi Source" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "BigQueryMultiTable" from the plugins list
    And Connect source as "ServiceNow" and sink as "BigQueryMultiTable" to establish connection
    And Navigate to the properties page of plugin: "BigQuery Multi Table"
    And Configure BigQuery sink plugin for Dataset and Table
    Then Validate "BigQuery Multi Table" plugin properties
    And Close the Plugin Properties page
    And Preview and run the pipeline
    And Enter runtime argument value "receiving_slip_line" for key "tableNames"
    And Enter runtime argument value "client.id" for key "clientId"
    And Enter runtime argument value "client.secret" for key "clientSecret"
    And Enter runtime argument value "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value "pipeline.user.username" for key "username"
    And Enter runtime argument value "pipeline.user.password" for key "password"
    Then Run the preview of pipeline with runtime arguments
    Then Verify the preview of pipeline is "success"

  @TS-SN-MULTI-RNTM-MACRO-02
  Scenario: Verify user should be able to run the pipeline when the source plugin is configured with macros
    When Open Datafusion Project to configure pipeline
    And Select data pipeline type as: "Data Pipeline - Batch"
    And Select plugin: "ServiceNow Multi Source" from the plugins list as: "source"
    And Navigate to the properties page of plugin: "ServiceNow Multi Source"
    And Fill Reference Name
    And Click on the Macro button of Property: "tableNames" and set the value to: "tableNames"
    And Click on the Macro button of Property: "clientId" and set the value to: "clientId"
    And Click on the Macro button of Property: "clientSecret" and set the value to: "clientSecret"
    And Click on the Macro button of Property: "restApiEndpoint" and set the value to: "restApiEndpoint"
    And Click on the Macro button of Property: "user" and set the value to: "username"
    And Click on the Macro button of Property: "password" and set the value to: "password"
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
    And Enter runtime argument value "receiving_slip_line" for key "tableNames"
    And Enter runtime argument value "client.id" for key "clientId"
    And Enter runtime argument value "client.secret" for key "clientSecret"
    And Enter runtime argument value "rest.api.endpoint" for key "restApiEndpoint"
    And Enter runtime argument value "pipeline.user.username" for key "username"
    And Enter runtime argument value "pipeline.user.password" for key "password"
    And Wait till pipeline is in running status with a timeout of 500 seconds
    And Verify the pipeline status is "Succeeded"
