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

package io.cdap.plugin.servicenow.source;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceNowMultiSourceTest {

  protected static final String CLIENT_ID = System.getProperty("servicenow.test.clientId");
  protected static final String CLIENT_SECRET = System.getProperty("servicenow.test.clientSecret");
  protected static final String REST_API_ENDPOINT = System.getProperty("servicenow.test.restApiEndpoint");
  protected static final String USER = System.getProperty("servicenow.test.user");
  protected static final String PASSWORD = System.getProperty("servicenow.test.password");
  private static final Logger LOG = LoggerFactory.getLogger(ServiceNowMultiInputFormatTest.class);

  ServiceNowMultiSource serviceNowMultiSource;
  ServiceNowMultiSourceConfig serviceNowMultiSourceConfig;

  @Before
  public void setup() {
    serviceNowMultiSourceConfig = new ServiceNowMultiSourceConfig("Reference Name", "tableName", "Query Mode",
      CLIENT_ID, CLIENT_SECRET, REST_API_ENDPOINT, USER, PASSWORD, "Actual", "2012-12-31", "2021-12-31"
    );
    serviceNowMultiSource = new ServiceNowMultiSource(serviceNowMultiSourceConfig);
  }

  @Test
  public void test() {
    //TODO
  }
}
