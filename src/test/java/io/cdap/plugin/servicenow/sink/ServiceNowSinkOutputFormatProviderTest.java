package io.cdap.plugin.servicenow.sink;

import io.cdap.cdap.api.data.batch.Output;
import io.cdap.plugin.servicenow.sink.output.ServiceNowOutputFormatProvider;
import org.apache.hadoop.conf.Configuration;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.junit.Assert;
import org.junit.Test;

public class ServiceNowSinkOutputFormatProviderTest {

  @Test
  public void testGetOutputFormatClassName() throws OAuthProblemException {
    Configuration configuration = new Configuration();
    ServiceNowOutputFormatProvider serviceNowOutputFormatProvider = new ServiceNowOutputFormatProvider(configuration);
    Assert.assertEquals("io.cdap.plugin.servicenow.sink.output.ServiceNowOutputFormat",
                        serviceNowOutputFormatProvider.getOutputFormatClassName());
  }

}
