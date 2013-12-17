/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.calendar;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * interprets textual descriptions of a duration.
 * <p>
 * Syntax: &lt;quantity&gt; [business] &lt;unit&gt; <br />
 * Where
 * <ul>
 * <li>&lt;quantity&gt; is a piece of text that is parsable with
 * <code>NumberFormat.getNumberInstance().parse(quantity)</code>.</li>
 * <li>&lt;unit&gt; is one of {second, seconds, minute, minutes, hour, hours, day, days, week,
 * weeks, month, months, year, years}.</li>
 * <li>And adding the optional indication <code>business</code> means that only business hours
 * should be taken into account for this duration.</li>
 * </ul>
 * </p>
 */
public class Duration implements Serializable {

  private static final long serialVersionUID = 2L;

  static final long SECOND = 1000;
  static final long MINUTE = 60 * SECOND;
  static final long HOUR = 60 * MINUTE;
  static final long DAY = 24 * HOUR;
  static final long WEEK = 7 * DAY;

  static final long BUSINESS_DAY;
  static final long BUSINESS_WEEK;
  static final long BUSINESS_MONTH;
  static final long BUSINESS_YEAR;

  static {
    Properties businessCalendarProperties = BusinessCalendar.getBusinessCalendarProperties();
    String businessDayText = businessCalendarProperties.getProperty("business.day.expressed.in.hours");
    String businessWeekText = businessCalendarProperties.getProperty("business.week.expressed.in.hours");
    String businessMonthText = businessCalendarProperties.getProperty("business.month.expressed.in.business.days");
    String businessYearText = businessCalendarProperties.getProperty("business.year.expressed.in.business.days");

    try {
      NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
      BUSINESS_DAY = multiply(format.parse(businessDayText), HOUR);
      BUSINESS_WEEK = multiply(format.parse(businessWeekText), HOUR);
      BUSINESS_MONTH = multiply(format.parse(businessMonthText), BUSINESS_DAY);
      BUSINESS_YEAR = multiply(format.parse(businessYearText), BUSINESS_DAY);
    }
    catch (ParseException e) {
      throw new NumberFormatException(e.getMessage());
    }
  }

  private static long multiply(Number n1, long n2) {
    return n1 instanceof Long ? n1.longValue() * n2 : (long) (n1.doubleValue() * n2);
  }

  static Map calendarFields = new HashMap();

  static {
    Integer millisecondField = new Integer(Calendar.MILLISECOND);
    calendarFields.put("millisecond", millisecondField);
    calendarFields.put("milliseconds", millisecondField);

    Integer secondField = new Integer(Calendar.SECOND);
    calendarFields.put("second", secondField);
    calendarFields.put("seconds", secondField);

    Integer minuteField = new Integer(Calendar.MINUTE);
    calendarFields.put("minute", minuteField);
    calendarFields.put("minutes", minuteField);

    Integer hourField = new Integer(Calendar.HOUR);
    calendarFields.put("hour", hourField);
    calendarFields.put("hours", hourField);

    Integer dayField = new Integer(Calendar.DAY_OF_MONTH);
    calendarFields.put("day", dayField);
    calendarFields.put("days", dayField);

    Integer weekField = new Integer(Calendar.WEEK_OF_MONTH);
    calendarFields.put("week", weekField);
    calendarFields.put("weeks", weekField);

    Integer monthField = new Integer(Calendar.MONTH);
    calendarFields.put("month", monthField);
    calendarFields.put("months", monthField);

    Integer yearField = new Integer(Calendar.YEAR);
    calendarFields.put("year", yearField);
    calendarFields.put("years", yearField);
  }

  static Map businessAmounts = new HashMap();

  static {
    Long secondAmount = new Long(SECOND);
    businessAmounts.put("business second", secondAmount);
    businessAmounts.put("business seconds", secondAmount);

    Long minuteAmount = new Long(MINUTE);
    businessAmounts.put("business minute", minuteAmount);
    businessAmounts.put("business minutes", minuteAmount);

    Long hourAmount = new Long(HOUR);
    businessAmounts.put("business hour", hourAmount);
    businessAmounts.put("business hours", hourAmount);

    Long dayAmount = new Long(BUSINESS_DAY);
    businessAmounts.put("business day", dayAmount);
    businessAmounts.put("business days", dayAmount);

    Long weekAmount = new Long(BUSINESS_WEEK);
    businessAmounts.put("business week", weekAmount);
    businessAmounts.put("business weeks", weekAmount);

    Long monthAmount = new Long(BUSINESS_MONTH);
    businessAmounts.put("business month", monthAmount);
    businessAmounts.put("business months", monthAmount);

    Long yearAmount = new Long(BUSINESS_YEAR);
    businessAmounts.put("business year", yearAmount);
    businessAmounts.put("business years", yearAmount);
  }

  private int field;
  private long amount;
  private boolean isBusinessTime;

  Duration() {
  }

  public Duration(long milliseconds) {
    amount = milliseconds;
    field = Calendar.MILLISECOND;
  }

  public Duration(Duration duration) {
    field = duration.field;
    amount = duration.amount;
    isBusinessTime = duration.isBusinessTime;
  }

  /**
   * creates a duration from a textual description. syntax: {number} space {unit} where number
   * is parsable to a java.lang.Number and unit is one of
   * <ul>
   * <li>second</li>
   * <li>seconds</li>
   * <li>minute</li>
   * <li>minutes</li>
   * <li>hour</li>
   * <li>hours</li>
   * <li>day</li>
   * <li>days</li>
   * <li>week</li>
   * <li>weeks</li>
   * <li>month</li>
   * <li>months</li>
   * <li>year</li>
   * <li>years</li>
   * </ul>
   */
  public Duration(String duration) {
    int index = indexOfNonWhite(duration, 0);
    char lead = duration.charAt(index);
    if (lead == '+' || lead == '-') ++index;

    // parse quantity
    NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
    index = indexOfNonWhite(duration, index);
    ParsePosition position = new ParsePosition(index);
    Number quantity = format.parse(duration, position);
    if (quantity == null) {
      throw new IllegalArgumentException("failed to parse amount: " + duration);
    }

    String unitText = duration.substring(position.getIndex()).trim();
    if (unitText.startsWith("business")) {
      // parse unit
      Long unit = (Long) businessAmounts.get(unitText);
      if (unit == null) {
        throw new IllegalArgumentException("no such time unit: " + unitText);
      }

      field = Calendar.MILLISECOND;
      amount = multiply(quantity, unit.longValue());
      isBusinessTime = true;
    }
    else {
      // parse unit
      Integer unit = (Integer) calendarFields.get(unitText);
      if (unit == null) {
        throw new IllegalArgumentException("no such time unit: " + unitText);
      }

      // is quantity exactly representable as int?
      if (quantity instanceof Long && isInteger(quantity.longValue())) {
        field = unit.intValue();
        amount = quantity.longValue();
      }
      else {
        field = Calendar.MILLISECOND;

        switch (unit.intValue()) {
        case Calendar.SECOND:
          amount = (long) (quantity.doubleValue() * SECOND);
          break;
        case Calendar.MINUTE:
          amount = (long) (quantity.doubleValue() * MINUTE);
          break;
        case Calendar.HOUR:
          amount = (long) (quantity.doubleValue() * HOUR);
          break;
        case Calendar.DAY_OF_MONTH:
          amount = (long) (quantity.doubleValue() * DAY);
          break;
        case Calendar.WEEK_OF_MONTH:
          amount = (long) (quantity.doubleValue() * WEEK);
          break;
        default:
          throw new IllegalArgumentException("fractional amount not supported for time unit: "
            + unitText);
        }
      }
    }

    if (lead == '-') amount = -amount;
  }

  boolean isNegative() {
    return amount < 0;
  }

  static int indexOfNonWhite(String str, int fromIndex) {
    int off = fromIndex;
    for (int len = str.length(); off < len; off++) {
      if (str.charAt(off) != ' ') return off;
    }
    return -1;
  }

  private static boolean isInteger(long number) {
    return number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE;
  }

  public Date addTo(Date date) {
    if (field == Calendar.MILLISECOND) return new Date(date.getTime() + amount);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(field, (int) amount);
    return calendar.getTime();
  }

  public long getMilliseconds() {
    switch (field) {
    case Calendar.MILLISECOND:
      return amount;
    case Calendar.SECOND:
      return amount * SECOND;
    case Calendar.MINUTE:
      return amount * MINUTE;
    case Calendar.HOUR:
      return amount * HOUR;
    case Calendar.DAY_OF_MONTH:
      return amount * DAY;
    case Calendar.WEEK_OF_MONTH:
      return amount * WEEK;
    default:
      throw new IllegalStateException("calendar field '" + field
        + "' does not have a fixed duration");
    }
  }

  public boolean isBusinessTime() {
    return isBusinessTime;
  }
}
