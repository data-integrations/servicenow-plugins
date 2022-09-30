/*
 * Copyright © 2022 Cask Data, Inc.
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
package io.cdap.plugin.servicenow.sink;

public class ServiceNowSinkConfigHelper {
  public static final String TEST_REF_NAME = "TestRefName";
  public static final String TEST_CLIENT_ID = "test-client-id";
  public static final String TEST_CLIENT_SECRET = "test-client-secret";
  public static final String TEST_API_ENDPOINT = "TestApiEndpoint";
  public static final String TEST_USER = "TestUser";
  public static final String TEST_PASSWORD = "TestPassword";
  public static final String TEST_OPERATION = "TestOperation";

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
    private String operation = TEST_OPERATION;
    private String tableName = "tableName";

    public ConfigBuilder setReferenceName(String referenceName) {
      this.referenceName = referenceName;
      return this;
    }

    public ConfigBuilder setTableName(String tableName) {
      this.tableName = tableName;
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

    public ConfigBuilder setOperation(String operation) {
      this.operation = operation;
      return this;
    }

    public ServiceNowSinkConfig build() {
      return new ServiceNowSinkConfig(referenceName, clientId, clientSecret, restApiEndpoint, user, password, tableName,
                                      operation);
    }
  }
}
