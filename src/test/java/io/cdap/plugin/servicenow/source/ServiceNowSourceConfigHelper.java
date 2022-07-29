/*
 * Copyright © 2020 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin.servicenow.source;

/**
 * Utility class that provides handy methods to construct ServiceNow Source Config for testing
 */
public class ServiceNowSourceConfigHelper {

  public static final String TEST_REF_NAME = "TestRefName";
  public static final String TEST_CLIENT_ID = "test-client-id";
  public static final String TEST_CLIENT_SECRET = "test-client-secret";
  public static final String TEST_API_ENDPOINT = "TestApiEndpoint";
  public static final String TEST_USER = "TestUser";
  public static final String TEST_PASSWORD = "TestPassword";

  public static ConfigBuilder newConfigBuilder() {
    return new ConfigBuilder();
  }

  public static class ConfigBuilder {
    private String referenceName = TEST_REF_NAME;
    private String clientId = TEST_CLIENT_ID;
    private String clientSecret = TEST_CLIENT_SECRET;
    private String restApiEndpoint = TEST_API_ENDPOINT;
    private String user = TEST_USER;
    private String password = TEST_PASSWORD;
    private String queryMode = "Table";
    private String applicationName = "";
    /*
    The name of the field that holds the table name. Must not be the name of any table column that will be read.
    Defaults to `tablename`. Note, the Table name field value will be ignored if the Mode is set to `Table`.
     */
    private String tableNameField = "tablename";
    /*
    The name of the ServiceNow table from which data to be fetched. Note, the Table name value will be ignored if the
    Mode is set to `Reporting`.
     */
    private String tableName = "tablename";
    /*
    The name of the ServiceNow table(s) from which data to be fetched.
     */
    private String tableNames = "tablesnames";
    private String valueType = "Actual";
    private String startDate = "";
    private String endDate = "";

    public ConfigBuilder setReferenceName(String referenceName) {
      this.referenceName = referenceName;
      return this;
    }

    public ConfigBuilder setQueryMode(String queryMode) {
      this.queryMode = queryMode;
      return this;
    }

    public ConfigBuilder setApplicationName(String applicationName) {
      this.applicationName = applicationName;
      return this;
    }

    public ConfigBuilder setTableNameField(String tableNameField) {
      this.tableNameField = tableNameField;
      return this;
    }

    public ConfigBuilder setTableName(String tableName) {
      this.tableName = tableName;
      return this;
    }

    public ConfigBuilder setTableNames(String tableNames) {
      this.tableNames = tableNames;
      return this;
    }

    public ConfigBuilder setClientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public ConfigBuilder setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    public ConfigBuilder setRestApiEndpoint(String restApiEndpoint) {
      this.restApiEndpoint = restApiEndpoint;
      return this;
    }

    public ConfigBuilder setUser(String user) {
      this.user = user;
      return this;
    }

    public ConfigBuilder setPassword(String password) {
      this.password = password;
      return this;
    }

    public ConfigBuilder setValueType(String valueType) {
      this.valueType = valueType;
      return this;
    }

    public ConfigBuilder setStartDate(String startDate) {
      this.startDate = startDate;
      return this;
    }

    public ConfigBuilder setEndDate(String endDate) {
      this.endDate = endDate;
      return this;
    }

    public ServiceNowSourceConfig build() {
      return new ServiceNowSourceConfig(referenceName, queryMode, applicationName, tableNameField, tableName,
        clientId, clientSecret, restApiEndpoint, user, password, valueType, startDate, endDate);
    }

    public ServiceNowMultiSourceConfig buildMultiSource() {
      return new ServiceNowMultiSourceConfig(referenceName, clientId, clientSecret, restApiEndpoint, user, password,
        tableNameField, valueType, startDate, endDate, tableNames);
    }


  }

}
