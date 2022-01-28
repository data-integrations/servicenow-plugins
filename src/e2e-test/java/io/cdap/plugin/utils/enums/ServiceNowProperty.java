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

package io.cdap.plugin.utils.enums;

import io.cdap.e2e.utils.PluginPropertyUtils;

/**
 * ServiceNow plugin properties - Enum.
 */
public enum ServiceNowProperty {
  REFERENCE_NAME("referenceName",
    PluginPropertyUtils.errorProp("required.property.referencename")),
  TABLE_NAMES("tableNames",
    PluginPropertyUtils.errorProp("required.property.tablenames")),
  CLIENT_ID("clientId",
    PluginPropertyUtils.errorProp("required.property.clientid")),
  CLIENT_SECRET("clientSecret",
    PluginPropertyUtils.errorProp("required.property.clientsecret")),
  REST_API_ENDPOINT("restApiEndpoint",
    PluginPropertyUtils.errorProp("required.property.restapiendpoint")),
  USERNAME("user",
    PluginPropertyUtils.errorProp("required.property.username")),
  PASSWORD("password",
    PluginPropertyUtils.errorProp("required.property.password")),
  START_DATE("startDate"),
  END_DATE("endDate");

  public final String dataCyAttributeValue;
  public String propertyMissingValidationMessage;

  ServiceNowProperty(String value) {
    this.dataCyAttributeValue = value;
  }

  ServiceNowProperty(String value, String propertyMissingValidationMessage) {
    this.dataCyAttributeValue = value;
    this.propertyMissingValidationMessage = propertyMissingValidationMessage;
  }
}
