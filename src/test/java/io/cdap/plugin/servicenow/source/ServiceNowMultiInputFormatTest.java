package io.cdap.plugin.servicenow.source;

import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableAPIClientImpl;
import io.cdap.plugin.servicenow.source.apiclient.ServiceNowTableDataResponse;
import io.cdap.plugin.servicenow.source.util.ServiceNowColumn;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ServiceNowMultiInputFormatTest {

  @Mock
  private ServiceNowTableAPIClientImpl serviceNowTableAPIClient;

  @InjectMocks
  ServiceNowMultiInputFormat serviceNowMultiInputFormat;

  @Before
  public void createMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testFetchTablesInfo() {
    ServiceNowMultiSourceConfig config = new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42",
      "Client Secret",
      "https://dev115725.service-now.com", "admin", "6qa8xrCJzWTV", "Actual", "2021-12-30", "2021-12-31", "sys_user");
    ServiceNowColumn column1 = new ServiceNowColumn("sys_created_by", "string");
    ServiceNowColumn column2 = new ServiceNowColumn("sys_updated_by", "string");
    List<ServiceNowColumn> columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    ServiceNowTableDataResponse response = new ServiceNowTableDataResponse();
    response.setColumns(columns);
    //when(serviceNowTableAPIClient.fetchTableSchema("sys_user", "2021-12-30","2021-12-31", Boolean.TRUE)).thenReturn
    // (response);
    assertEquals(ServiceNowMultiInputFormat
      .fetchTablesInfo(config)
      .size(), 0);
  }

  @Test
  public void testFetchTablesInfoEmptyWithTablenames() {
    ServiceNowMultiSourceConfig config1 = new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42",
      "Client Secret",
      "https://dev115725.service-now.com", "User", "password", "Actual", "2021-12-30", "2021-12-31", ",");
    assertTrue(ServiceNowMultiInputFormat
      .fetchTablesInfo(config1)
      .isEmpty());

    ServiceNowMultiSourceConfig config2 = new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42",
      "Client " +
        "Secret",
      "https://dev115725.service-now.com", "User", "password", "Actual", "2021-12-30", "2021-12-31", "");

    assertTrue(ServiceNowMultiInputFormat
      .fetchTablesInfo(config2)
      .isEmpty());
  }
}

