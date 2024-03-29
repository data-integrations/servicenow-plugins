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

package io.cdap.plugin.servicenow.source;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.cdap.plugin.servicenow.sink.ServiceNowSinkConfig;
import io.cdap.plugin.servicenow.util.ServiceNowTableInfo;
import org.apache.hadoop.conf.Configuration;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Allows to specify and access connection configuration properties of {@link Configuration}.
 */
public class ServiceNowJobConfiguration {
  private static final String PLUGIN_CONF_FIELD = "servicenow.plugin.conf";
  private static final String INFO_FIELD = "servicenow.table.info";

  private static final Type PLUGIN_CONF_TYPE = new TypeToken<ServiceNowSourceConfig>() {
  }.getType();
  private static final Type MULTI_SOURCE_PLUGIN_CONF_TYPE = new TypeToken<ServiceNowMultiSourceConfig>() {
  }.getType();
  private static final Type PLUGIN_SINK_CONF_TYPE = new TypeToken<ServiceNowSinkConfig>() {
  }.getType();
  private static final Type INFO_TYPE = new TypeToken<List<ServiceNowTableInfo>>() {
  }.getType();

  private static final Gson GSON = new Gson();

  private Configuration conf;

  public ServiceNowJobConfiguration(Configuration job) {
    this.conf = job;
  }

  public Configuration getConf() {
    return this.conf;
  }

  public void setPluginConfiguration(ServiceNowSourceConfig conf) {
    set(PLUGIN_CONF_FIELD, GSON.toJson(conf));
  }

  public void setMultiSourcePluginConfiguration(ServiceNowMultiSourceConfig conf) {
    set(PLUGIN_CONF_FIELD, GSON.toJson(conf));
  }
  public void setSinkPluginConfiguration(ServiceNowSinkConfig conf) {
    set(PLUGIN_CONF_FIELD, GSON.toJson(conf));
  }

  public ServiceNowSourceConfig getPluginConf() {
    return GSON.fromJson(getConf().get(PLUGIN_CONF_FIELD), PLUGIN_CONF_TYPE);
  }

  public ServiceNowMultiSourceConfig getMultiSourcePluginConf() {
    return GSON.fromJson(getConf().get(PLUGIN_CONF_FIELD), MULTI_SOURCE_PLUGIN_CONF_TYPE);
  }

  public ServiceNowSinkConfig getSinkPluginConf() {
    return GSON.fromJson(getConf().get(PLUGIN_CONF_FIELD), PLUGIN_SINK_CONF_TYPE);
  }

  public List<ServiceNowTableInfo> getTableInfos() {
    return GSON.fromJson(getConf().get(INFO_FIELD), INFO_TYPE);
  }

  public void setTableInfos(List<ServiceNowTableInfo> infoList) {
    set(INFO_FIELD, GSON.toJson(infoList));
  }

  private void set(String key, String value) {
    getConf().set(key, value);
  }
}
