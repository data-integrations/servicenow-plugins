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

package io.cdap.plugin.servicenow.util;

/**
 * ServiceNow constants.
 */
public interface ServiceNowConstants {

  /**
   * ServiceNow plugin name.
   */
  String PLUGIN_NAME = "ServiceNow";

  /**
   * ServiceNow Multi Source plugin name.
   */
  String PLUGIN_NAME_MULTI_SOURCE = "ServiceNowMultiSource";

  /**
   * Configuration property name used to specify the query mode.
   */
  String PROPERTY_QUERY_MODE = "queryMode";

  /**
   * Configuration property name used to specify the application.
   */
  String PROPERTY_APPLICATION_NAME = "applicationName";

  /**
   * Configuration property name used to specify the table name field.
   */
  String PROPERTY_TABLE_NAME_FIELD = "tableNameField";

  /**
   * Configuration property name used to specify table name.
   */
  String PROPERTY_TABLE_NAME = "tableName";

  /**
   * Configuration property name used to specify table names.
   */
  String PROPERTY_TABLE_NAMES = "tableNames";

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
   * Configuration property name used to specify the type of operation.
   */
  String PROPERTY_OPERATION = "operation";

  /**
   * Configuration property name used to specify the page size.
   */
  String PROPERTY_PAGE_SIZE = "pageSize";

  /**
   * Configuration property name used to get the schema.
   */
  String NAME_SCHEMA = "schema";

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
   * Configuration property name used to specify table name field.
   */
  String TABLE_NAME_FIELD_DEFAULT = "tablename";

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

  /**
   * The total count.
   */
  String HEADER_NAME_TOTAL_COUNT = "X-Total-Count";

  /**
   * The status.
   */
  String STATUS = "status";

  /**
   * The System Id.
   */
  String SYSTEM_ID = "sys_id";

  /**
   * The message.
   */
  String MESSAGE = "message";

  /**
   * The failure.
   */
  String FAILURE = "failure";

  /**
   * The error.
   */
  String ERROR = "error";

  /**
   * The result.
   */
  String RESULT = "result";

  /**
   * The error detail.
   */
  String ERROR_DETAIL = "detail";

  /**
   * The maximum execution time exceeded error.
   */
  String MAXIMUM_EXECUTION_TIME_EXCEEDED = "maximum execution time exceeded";

  /**
   * The ACL Exception Failed error.
   */
  String ACL_EXCEPTION = "ACL Exception Insert Failed due to security constraints";

  /**
   * The Error during insert operation.
   */
  String INSERT_ERROR = "Error during insert of";

  /**
   * The Error during update operation.
   */
  String UPDATE_ERROR = "Record doesn't exist or ACL restricts the record retrieval";

  /**
   * The wait time for API retry in milliseconds.
   */
  int WAIT_TIME = 120000;
  
  /**
   * The maximum number of retry attempts.
   */
  int MAX_NUMBER_OF_RETRY_ATTEMPTS = 5;

  /**
   * The API Url Template for INSERT operation .
   */
  String INSERT_TABLE_API_URL_TEMPLATE = "/api/now/table/%s";

  /**
   * The API Url Template for UPDATE operation
   */
  String UPDATE_TABLE_API_URL_TEMPLATE = "/api/now/table/%s/%s";

  /**
   * The INSERT operation
   */
  String INSERT_OPERATION = "insert";

  /**
   * The UPDATE operation
   */
  String UPDATE_OPERATION = "update";

  /**
   * The HTTP POST Request
   */
  String HTTP_POST = "POST";

  /**
   * The HTTP PUT Request
   */
  String HTTP_PUT = "PUT";

  /**
   * The ServiceNow System ID Field
   */
  String SYS_ID = "sys_id";

  /**
   * The response body
   */
  String BODY = "body";

  /**
   * The response id
   */
  String ID = "id";

  /**
   * The response status code
   */
  String STATUS_CODE = "status_code";

  /**
   * The serviced requests
   */
  String SERVICED_REQUESTS = "serviced_requests";

  /**
   * The unserviced requests
   */
  String UNSERVICED_REQUESTS = "unserviced_requests";

  /**
   * The initial wait time for HTTP POST API retry in milliseconds.
   */
  int BASE_DELAY = 5000;

  /**
   * No. of requests that will be sent to ServiceNow Batch API as a payload
   */
  int RECORDS_PER_BATCH = 50;

  /**
   * ServiceNow Table Rest API supports up to 2 digits only after the decimal for decimal types. Anything greater than
   * that gets rounded off.
   */
  int DEFAULT_SCALE = 2;

  /**
   * Default Precision supported by ServiceNow Rest API
   */
  int DEFAULT_PRECISION = 20;
}
