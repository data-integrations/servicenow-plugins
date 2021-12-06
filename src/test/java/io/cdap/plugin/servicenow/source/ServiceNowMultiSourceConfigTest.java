package io.cdap.plugin.servicenow.source;

import io.cdap.cdap.etl.api.validation.ValidationFailure;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ServiceNowMultiSourceConfig}.
 */
public class ServiceNowMultiSourceConfigTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() {
    assertEquals("Table Names",
      (new ServiceNowMultiSourceConfig("Reference Name", "Table Name Field", "42", "Client Secret",
        "https://dev115725.service-now.com", "User", "password", "42", "2021-12-30", "2021-12-31",
        "Table Names")).getTableNames());
  }

  @Test
  public void testValidate() throws Exception {
    ServiceNowMultiSourceConfig config = ServiceNowSourceConfigHelper.newConfigBuilder()
      .setReferenceName("test")
      .setRestApiEndpoint("https://dev115725.service-now.com")
      .setUser("User")
      .setPassword("password")
      .setClientId("42")
      .setClientSecret("Client Secret")
      .setTableNames("sys_user")
      .setValueType("Actual")
      .setStartDate("2021-12-30")
      .setEndDate("2021-12-31")
      .setTableNameField("tablename")
      .buildMultiSource();

    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    config.validate(mockFailureCollector);
    Assert.assertEquals(1, mockFailureCollector.getValidationFailures().size());
  }

  @Test
  public void testValidateTableNames() {
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig = new ServiceNowMultiSourceConfig("Reference Name",
      "Table Name Field", "42", "Client Secret", "https://dev115725.service-now.com/", "admin", "6qa8xrCJzWTV",
      "Actual",
      "2021-12-30", "2021-12-31", "Table Names");
    serviceNowMultiSourceConfig.validateTableNames(new MockFailureCollector("Stage Name"));
    assertEquals("42", serviceNowMultiSourceConfig.getClientId());
    assertEquals("Table Name Field", serviceNowMultiSourceConfig.tableNameField);
    assertEquals("admin", serviceNowMultiSourceConfig.getUser());
    assertEquals("Table Names", serviceNowMultiSourceConfig.getTableNames());
    assertEquals("2021-12-30", serviceNowMultiSourceConfig.getStartDate());
    assertEquals("https://dev115725.service-now.com/", serviceNowMultiSourceConfig.getRestApiEndpoint());
    assertEquals("6qa8xrCJzWTV", serviceNowMultiSourceConfig.getPassword());
    assertEquals("Client Secret", serviceNowMultiSourceConfig.getClientSecret());
    assertEquals("2021-12-31", serviceNowMultiSourceConfig.getEndDate());
  }

  @Test
  public void testValidateTableNamesWhenTableNamesAreEmpty() {
    ServiceNowMultiSourceConfig serviceNowMultiSourceConfig = new ServiceNowMultiSourceConfig("Reference Name",
      "Table Name Field", "42", "Client Secret", "https://dev115725.service-now.com/", "User", "password", "42",
      "2021-12-30", "2021-12-31", "");
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    serviceNowMultiSourceConfig.validateTableNames(mockFailureCollector);
    List<ValidationFailure> validationFailures = mockFailureCollector.getValidationFailures();
    assertEquals(1, validationFailures.size());
    ValidationFailure getResult = validationFailures.get(0);
    List<ValidationFailure.Cause> causes = getResult.getCauses();
    assertEquals(1, causes.size());
    assertEquals("Table names must be specified.", getResult.getMessage());
    assertEquals("Stage Name", getResult.getCorrectiveAction());
    assertEquals("Table names must be specified. Stage Name", getResult.getFullMessage());
    assertEquals("tableNames", causes.get(0).getAttributes().get("stageConfig"));
  }
}

