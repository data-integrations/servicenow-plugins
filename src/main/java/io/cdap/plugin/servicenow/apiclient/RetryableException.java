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

package io.cdap.plugin.servicenow.apiclient;

import org.jetbrains.annotations.Nullable;

/** Custom Exception Class for handling retrying API calls */
public class RetryableException extends RuntimeException implements ExceptionWithHttpStatus {

  private static final long serialVersionUID = 1L;
  private final Integer httpStatusCode;

  public RetryableException() {
    super();
    this.httpStatusCode = null;
  }

  public RetryableException(String message, Integer httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  public RetryableException(String message) {
    super(message);
    this.httpStatusCode = null;
  }

  public RetryableException(String message, Throwable throwable) {
    super(message, throwable);
    this.httpStatusCode = null;
  }

  @Nullable
  @Override
  public Integer getHttpStatusCode() {
    return httpStatusCode;
  }
}
