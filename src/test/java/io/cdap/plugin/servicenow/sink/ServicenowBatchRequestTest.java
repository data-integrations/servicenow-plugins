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

import io.cdap.plugin.servicenow.sink.model.RestRequest;
import io.cdap.plugin.servicenow.sink.model.ServiceNowBatchRequest;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

public class ServicenowBatchRequestTest {

  @Test
  public void testId() {
    ServiceNowBatchRequest request = new ServiceNowBatchRequest();
    request.setBatchRequestId("id");
    Assert.assertEquals("id", request.getBatchRequestId());
  }

  @Test
  public void testRecords() {
    ServiceNowBatchRequest request = new ServiceNowBatchRequest();
    List<RestRequest> records = new ArrayList<>();
    RestRequest request1 = new RestRequest();
    request.setBatchRequestId("id");
    records.add(request1);
    request.setRestRequests(records);
    Assert.assertEquals(1, request.getRestRequests().size());
  }
}
