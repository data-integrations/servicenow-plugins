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

package io.cdap.plugin.servicenow.sink.model;

/**
 * Model class for Schema Response from Schema API
 */
public class SchemaResponse {
  private String label;
  private String exampleValue;
  private String internalType;
  private String name;

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(String exampleValue) {
    this.exampleValue = exampleValue;
  }

  public String getInternalType() {
    return internalType;
  }

  public void setInternalType(String internalType) {
    this.internalType = internalType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
