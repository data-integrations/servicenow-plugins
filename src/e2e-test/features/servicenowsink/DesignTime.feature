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
Feature: ServiceNow Sink - Design time scenarios

  @TS-SN-DSGN-SINK-01 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE
  Scenario: Verify user should be able to validate when plugin is configured for table Receiving Slip with Input operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-02 @BQ_SOURCE_TEST_RECEIVING_SLIP_LINE
  Scenario: Verify user should be able to validate the plugin when we Insert data of one table into another using Sink Plugin
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "pc_vendor_cat_item"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Click on the Validate button
    Then Verify error in Input Schema for non creatable fields

  @TS-SN-DSGN-SINK-03 @BQ_SOURCE_AGENT_ASSIST_RECOMMENDATION
  Scenario: Verify user should be able to validate when plugin is configured for table Agent Assist recommendation with Input operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "agent_assist_recommendation"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-04 @BQ_SOURCE_VENDOR_CATALOG_ITEM
  Scenario: Verify user should be able to validate when plugin is configured for table Vendor Catalog Item with Input operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "vendor_catalog_item"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-05 @BQ_SOURCE_SERVICE_OFFERING
  Scenario: Verify user should be able to validate when plugin is configured for table Service Offering with Input operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "service_offering"
    And Select radio button plugin property: "operation" with value: "INSERT"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-06 @SN_SOURCE_CONFIG @SN_RECEIVING_SLIP_LINE @BQ_SOURCE_UPDATE_RECEIVING_SLIP_LINE
  Scenario: Verify user should be able to validate when plugin is configured for table Receiving Slip with Update operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "receiving_slip_line"
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-07 @SN_UPDATE_AGENT_ASSIST_RECOMMENDATION @SN_SOURCE_CONFIG @BQ_SOURCE_UPDATE_AGENT_ASSIST_RECOMMENDATION
  Scenario: Verify user should be able to validate when plugin is configured for table Agent Assist recommendation with Update operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "agent_assist_recommendation"
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-08 @SN_UPDATE_VENDOR_CATALOG_ITEM @SN_SOURCE_CONFIG @BQ_SOURCE_UPDATE_VENDOR_CATALOG_ITEM
  Scenario: Verify user should be able to validate when plugin is configured for table Vendor Catalog Item with Update operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "vendor_catalog_item"
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-9 @SN_SOURCE_CONFIG @SN_UPDATE_SERVICE_OFFERING @BQ_SOURCE_UPDATE_SERVICE_OFFERING
  Scenario: Verify user should be able to validate when plugin is configured for table Service Offering with Update operation
    When Open Datafusion Project to configure pipeline
    And Select plugin: "BigQuery" from the plugins list as: "Source"
    And Navigate to the properties page of plugin: "BigQuery"
    And Fill Reference Name
    And Configure BigQuery source plugin for Dataset and Table
    And Validate "BigQuery" plugin properties
    And Close the Plugin Properties page
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Connect plugins: "BigQuery" and "ServiceNow" to establish connection
    And Navigate to the properties page of plugin: "ServiceNow"
    And Fill Reference Name
    And fill Credentials section for pipeline user
    And Enter input plugin property: "tableName" with value: "service_offering"
    And Select radio button plugin property: "operation" with value: "UPDATE"
    And Validate "ServiceNow" plugin properties

  @TS-SN-DSGN-SINK-10 @CONNECTION
  Scenario: Verify user should be able to create the valid connection using connection manager functionality
    When Open Datafusion Project to configure pipeline
    And Select Sink plugin: "ServiceNow" from the plugins list
    And Navigate to the properties page of plugin: "ServiceNow"
    And Click plugin property: "switch-useConnection"
    And Click on the Browse Connections button
    And Click on the Add Connection button
    And Select ServiceNow Connection
    And Enter input plugin property: "name" with value: "connection.name"
    And fill Credentials section for pipeline user
    Then Click on the Test Connection button
    And Verify the test connection is successful
