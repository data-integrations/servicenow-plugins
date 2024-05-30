package io.cdap.plugin.servicenow.restapi;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import io.cdap.plugin.servicenow.util.ServiceNowConstants;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ExperimentTest {

  public Boolean isRetryable = true;
  public String errorMessage = "Error Handling request";
  public int cnt = 0;

  @Test
  public void mainTest() throws TestException {
    Callable<TestResponse> func =
        () -> {
          return getResponse(errorMessage);
        };
    Retryer<TestResponse> retryer = getRetryer();
    try {
      TestResponse response = retryer.call(func);
      // Execution is successful
      if (response.exception != null) {
        // Execution successful but returned non retryable error
        throw new TestException(response.exception);
      }
    } catch (RetryException e) {
      // Execution successful but returned retryable error
      Attempt<?> attempt = e.getLastFailedAttempt();
      if (attempt.hasResult()) {
        TestResponse response = (TestResponse) attempt.getResult();
        throw response.exception;
      } else if (attempt.hasException()) {
        throw new TestException(attempt.getExceptionCause());
      }
    } catch (ExecutionException e) {
      // Execution failed with error
      System.out.println("Huh");
      throw new RuntimeException(e);
    }
  }

  public TestResponse getResponse(String message) throws InterruptedException {
    System.out.println("Running request");
    cnt++;
    //Thread.sleep(5*1000);
    if (cnt == 5) {
      throw new RuntimeException("runtime sssss");
    }
    return new TestResponse(isRetryable, new TestException(message));
  }

  public Retryer<TestResponse> getRetryer() {
    Retryer<TestResponse> retryer =
        RetryerBuilder.<TestResponse>newBuilder()
            .retryIfResult(
                new Predicate<TestResponse>() {
                  @Override
                  public boolean apply(@Nullable TestResponse testResponse) {
                    return testResponse.exception != null && testResponse.isRetryable;
                  }
                })
            .withWaitStrategy(
                WaitStrategies.exponentialWait(
                    ServiceNowConstants.WAIT_TIME, TimeUnit.MILLISECONDS))
            .withStopStrategy(
                StopStrategies.stopAfterAttempt(ServiceNowConstants.MAX_NUMBER_OF_RETRY_ATTEMPTS))
            .build();
    return retryer;
  }

  public class TestResponse {
    public Boolean isRetryable;
    public TestException exception;

    public TestResponse(Boolean isRetryable, TestException exception) {
      this.isRetryable = isRetryable;
      this.exception = exception;
    }
  }

  public class TestException extends Exception {
    public TestException(String message) {
      super(message);
    }

    public TestException(Throwable t) {
      super(t);
    }
  }
}
