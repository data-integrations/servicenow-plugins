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

package io.cdap.plugin.utils.enums;

/**
 * Enum for Tables in Table mode.
 */
public enum TablesInTableMode {
  ASSET_COVERED("clm_m2m_contract_asset"),
  CONDITION("clm_condition_checker"),
  CONTRACT("ast_contract"),
  CONTRACT_HISTORY("clm_contract_history"),
  TERMS_AND_CONDITIONS("clm_terms_and_conditions"),
  USERS_COVERED("clm_m2m_contract_user"),
  HARDWARE_CATALOG("pc_hardware_cat_item"),
  SOFTWARE_CATALOG("pc_software_cat_item"),
  PRODUCT_CATALOG_ITEM("pc_product_cat_item"),
  VENDOR_CATALOG_ITEM("pc_vendor_cat_item"),
  PURCHASE_ORDER("proc_po"),
  PURCHASE_ORDER_LINE_ITEMS("proc_po_item"),
  INVALID_TABLE("blahblah");

  public final String value;

  TablesInTableMode(String value) {
    this.value = value;
  }
}
