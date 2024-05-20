package io.cdap.plugin.servicenow.apiclient;

import javax.annotation.Nullable;

/**
 * Exception which contains a Http Status Code.
 */
public interface ExceptionWithHttpStatus {
    @Nullable Integer getHttpStatusCode();
}
