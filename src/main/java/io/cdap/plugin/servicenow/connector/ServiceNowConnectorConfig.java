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
package io.cdap.plugin.servicenow.connector;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.servicenow.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import io.cdap.plugin.servicenow.util.Util;
import org.apache.http.HttpHost;

import javax.annotation.Nullable;


/**
 * PluginConfig for ServiceNow Connector
 */
public class ServiceNowConnectorConfig extends PluginConfig {

  @Name(ServiceNowConstants.PROPERTY_CLIENT_ID)
  @Macro
  @Nullable
  @Description("The Client ID for ServiceNow Instance.")
  private final String clientId;

  @Name(ServiceNowConstants.PROPERTY_CLIENT_SECRET)
  @Macro
  @Nullable
  @Description("The Client Secret for ServiceNow Instance.")
  private final String clientSecret;

  @Name(ServiceNowConstants.PROPERTY_API_ENDPOINT)
  @Macro
  @Nullable
  @Description("The REST API Endpoint for ServiceNow Instance. For example, https://instance.service-now.com")
  private final String restApiEndpoint;

  @Name(ServiceNowConstants.PROPERTY_USER)
  @Macro
  @Nullable
  @Description("The user name for ServiceNow Instance.")
  private final String user;

  @Name(ServiceNowConstants.PROPERTY_PASSWORD)
  @Macro
  @Nullable
  @Description("The password for ServiceNow Instance.")
  private final String password;

  @Name(ServiceNowConstants.PROPERTY_PROXY_URL)
  @Macro
  @Nullable
  @Description("Proxy URL through which all the ServiceNow API calls are routed. " +
    "Must contain a protocol, address and port. For example, http://proxy.example.com:8080")
  private final String proxyUrl;

  @Name(ServiceNowConstants.PROPERTY_PROXY_USERNAME)
  @Macro
  @Nullable
  @Description("The username to authenticate with the proxy server, if the proxy requires authentication.")
  private final String proxyUsername;

  @Name(ServiceNowConstants.PROPERTY_PROXY_PASSWORD)
  @Macro
  @Nullable
  @Description("The password to authenticate with the proxy server, if the proxy requires authentication.")
  private final String proxyPassword;

  public ServiceNowConnectorConfig(String clientId, String clientSecret, String restApiEndpoint,
                                   String user, String password) {
    this(clientId, clientSecret, restApiEndpoint, user, password, null, null, null);
  }

  public ServiceNowConnectorConfig(String clientId, String clientSecret, String restApiEndpoint,
                                   String user, String password, String proxyUrl, String proxyUsername,
                                   String proxyPassword) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.restApiEndpoint = restApiEndpoint;
    this.user = user;
    this.password = password;
    this.proxyUrl = proxyUrl;
    this.proxyUsername = proxyUsername;
    this.proxyPassword = proxyPassword;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getRestApiEndpoint() {
    return restApiEndpoint;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  @Nullable
  public String getProxyUrl() {
    return proxyUrl;
  }

  @Nullable
  public String getProxyUsername() {
    return proxyUsername;
  }

  @Nullable
  public String getProxyPassword() {
    return proxyPassword;
  }

  /**
   * Returns true if the proxy properties do not contain macros and a proxy URL has been configured.
   */
  public boolean hasProxyConfigured() {
    return !containsMacro(ServiceNowConstants.PROPERTY_PROXY_URL) && !Util.isNullOrEmpty(proxyUrl);
  }


  /**
   * validates all the fields which are mandatory for the connection.
   */
  public void validateCredentialsFields(FailureCollector collector) {
    if (Util.isNullOrEmpty(getClientId())) {
      collector.addFailure("Client ID must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_ID);
    }

    if (Util.isNullOrEmpty(getClientSecret())) {
      collector.addFailure("Client Secret must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_SECRET);
    }

    if (Util.isNullOrEmpty(getRestApiEndpoint())) {
      collector.addFailure("API Endpoint must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_API_ENDPOINT);
    }

    if (Util.isNullOrEmpty(getUser())) {
      collector.addFailure("User name must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_USER);
    }

    if (Util.isNullOrEmpty(getPassword())) {
      collector.addFailure("Password must be specified.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_PASSWORD);
    }

    validateProxyFields(collector);
  }

  /**
   * Validates the optional proxy fields. The proxy URL must be a valid host with a protocol and port, and a proxy
   * username and password must be provided together.
   */
  public void validateProxyFields(FailureCollector collector) {
    if (!containsMacro(ServiceNowConstants.PROPERTY_PROXY_URL) && !Util.isNullOrEmpty(proxyUrl)) {
      try {
        HttpHost host = HttpHost.create(proxyUrl);
        if (host.getPort() == -1) {
          throw new IllegalArgumentException();
        }
      } catch (IllegalArgumentException e) {
        collector.addFailure("Proxy URL is not valid.",
                             "Ensure the Proxy URL contains a protocol, address and port. " +
                               "For example, http://proxy.example.com:8080.")
          .withConfigProperty(ServiceNowConstants.PROPERTY_PROXY_URL);
      }
    }

    if (containsMacro(ServiceNowConstants.PROPERTY_PROXY_USERNAME)
      || containsMacro(ServiceNowConstants.PROPERTY_PROXY_PASSWORD)) {
      return;
    }

    boolean hasUsername = !Util.isNullOrEmpty(proxyUsername);
    boolean hasPassword = !Util.isNullOrEmpty(proxyPassword);
    if (hasUsername && !hasPassword) {
      collector.addFailure("Proxy password must be specified when a proxy username is provided.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_PROXY_PASSWORD);
    } else if (!hasUsername && hasPassword) {
      collector.addFailure("Proxy username must be specified when a proxy password is provided.", null)
        .withConfigProperty(ServiceNowConstants.PROPERTY_PROXY_USERNAME);
    }
  }

  public void validateConnection(FailureCollector collector) {
    try {
      ServiceNowTableAPIClientImpl restApi = new ServiceNowTableAPIClientImpl(this, true);
      restApi.getAccessToken();
    } catch (Exception e) {
      collector.addFailure("Unable to connect to ServiceNow Instance.",
                           "Ensure properties like Client ID, Client Secret, API Endpoint, User Name, Password " +
                             "are correct.")
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_ID)
        .withConfigProperty(ServiceNowConstants.PROPERTY_CLIENT_SECRET)
        .withConfigProperty(ServiceNowConstants.PROPERTY_API_ENDPOINT)
        .withConfigProperty(ServiceNowConstants.PROPERTY_USER)
        .withConfigProperty(ServiceNowConstants.PROPERTY_PASSWORD)
        .withStacktrace(e.getStackTrace());
    }
  }


}
