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

package io.cdap.plugin.servicenow.source.util;

/**
 * ServiceNow constants.
 */
public interface ServiceNowConstants {

  /**
   * ServiceNow plugin name.
   */
  String PLUGIN_NAME = "ServiceNow";

  /**
   * Configuration property name used to specify the query mode.
   */
  String PROPERTY_QUERY_MODE = "queryMode";

  /**
   * Configuration property name used to specify the application.
   */
  String PROPERTY_APPLICATION_NAME = "applicationName";

  /**
   * Configuration property name used to specify table name.
   */
  String PROPERTY_TABLE_NAME = "tableName";

  /**
   * Configuration property name used to specify client id.
   */
  String PROPERTY_CLIENT_ID = "clientId";

  /**
   * Configuration property name used to specify client secret.
   */
  String PROPERTY_CLIENT_SECRET = "clientSecret";

  /**
   * Configuration property name used to specify REST API endpoint.
   */
  String PROPERTY_API_ENDPOINT = "restApiEndpoint";

  /**
   * Configuration property name used to specify user name.
   */
  String PROPERTY_USER = "user";

  /**
   * Configuration property name used to specify password.
   */
  String PROPERTY_PASSWORD = "password";

  /**
   * Configuration property name used to specify value type.
   */
  String PROPERTY_VALUE_TYPE = "valueType";

  /**
   * Configuration property name used to specify start date.
   */
  String PROPERTY_START_DATE = "startDate";

  /**
   * Configuration property name used to specify end date.
   */
  String PROPERTY_END_DATE = "endDate";

  /**
   * Table prefix to be used in case of Reporting mode.
   */
  String TABLE_PREFIX = "multisink.";

  /**
   * The date format.
   */
  String DATE_FORMAT = "yyyy-MM-dd";

  /**
   * The max limit for the page size.
   */
  int PAGE_SIZE = 5000;
}
