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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class.
 */
public class Util {
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
}
