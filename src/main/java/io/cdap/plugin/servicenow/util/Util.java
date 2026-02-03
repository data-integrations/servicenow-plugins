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

package io.cdap.plugin.servicenow.util;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class.
 */
public class Util {
  private static final Logger LOG = LoggerFactory.getLogger(Util.class);
  private static final String DATE_RANGE_TEMPLATE = "%sBETWEENjavascript:gs.dateGenerate('%s','start')" +
          "@javascript:gs.dateGenerate('%s','end')";
  private static final String FIELD_CREATED_ON = "sys_created_on";
  private static final String FIELD_UPDATED_ON = "sys_updated_on";

  /**
   * Utility function to check if incoming string is empty or not.
   *
   * @param string The value to be checked for emptyness
   * @return true if string is empty otherwise false
   */
  public static boolean isNullOrEmpty(String string) {
    return Strings.isNullOrEmpty(Strings.nullToEmpty(string).trim());
  }

  /**
   * Utility function to check if the date is in valid format.
   *
   * @param dateFormat The date format for which the value to be checked for validity
   * @param value The date value to be checked for validity
   * @return true if date is in valid format otherwise false
   */
  public static boolean isValidDateFormat(String dateFormat, String value) {
    final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    try {
      final Date date = sdf.parse(value);

      if (!value.equals(sdf.format(date))) {
        return false;
      }
    } catch (ParseException e) {
      return false;
    }
    return true;
  }

  /**
   * Generates a date range query for ServiceNow.
   *
   * @param startDate The start date
   * @param endDate The end date
   * @return The date range query
   */
  public static String generateDateRangeQuery(String startDate, String endDate) {
    if (Util.isNullOrEmpty(startDate) || Util.isNullOrEmpty(endDate)) {
        return "";
    }

    String dateRange = "";
    try {
        String createdOnDateRange = String.format(DATE_RANGE_TEMPLATE, FIELD_CREATED_ON, startDate, endDate);
        String updatedOnDateRange = String.format(DATE_RANGE_TEMPLATE, FIELD_UPDATED_ON, startDate, endDate);
        dateRange = String.format("%s^OR%s", createdOnDateRange, updatedOnDateRange);
      } catch (Exception e) {
          LOG.error("Error in generateDateRangeQuery, hence ignoring the date range", e);
    }

    return dateRange;
  }
}
