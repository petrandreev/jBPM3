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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

/**
 * is a day on a business calendar.
 */
public class Day implements Serializable {

  private static final long serialVersionUID = 1L;

  final DayPart[] dayParts;
  private final BusinessCalendar businessCalendar;

  private static final String[] WEEK_DAY_KEYS = {
    null, "weekday.sunday", "weekday.monday", "weekday.tuesday", "weekday.wednesday",
    "weekday.thursday", "weekday.friday", "weekday.saturday"
  };

  public static Day[] parseWeekDays(Properties calendarProperties,
    BusinessCalendar businessCalendar) {
    DateFormat timeFormat = new SimpleDateFormat(calendarProperties.getProperty("hour.format"));

    Day[] weekDays = new Day[WEEK_DAY_KEYS.length];
    for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
      weekDays[i] = new Day(calendarProperties.getProperty(WEEK_DAY_KEYS[i]),
        timeFormat,
        businessCalendar);
    }
    return weekDays;
  }

  public Day(String dayPartsText, DateFormat dateFormat, BusinessCalendar businessCalendar) {
    this.businessCalendar = businessCalendar;

    String[] dayPartTexts = dayPartsText.split("[\\s&]+");
    if (dayPartTexts.length > 1 || dayPartTexts[0].length() > 0) {
      dayParts = new DayPart[dayPartTexts.length];
      for (int i = 0; i < dayParts.length; i++) {
        dayParts[i] = new DayPart(dayPartTexts[i], dateFormat, this, i);
      }
    }
    else {
      dayParts = new DayPart[0];
    }
  }

  /** @deprecated with no replacement */
  public void findNextDayPartStart(int dayPartIndex, Date date, Object[] result) {
    // if there is a day part that starts after the given date
    if (dayPartIndex < dayParts.length) {
      DayPart dayPart = dayParts[dayPartIndex];
      if (dayPart.isStartAfter(date)) {
        result[0] = dayPart.getStartTime(date);
        result[1] = dayPart;
      }
      else {
        findNextDayPartStart(dayPartIndex + 1, date, result);
      }
    }
    else {
      // descend recursively
      date = businessCalendar.findStartOfNextDay(date);
      Day nextDay = businessCalendar.findDay(date);
      nextDay.findNextDayPartStart(0, date, result);
    }
  }

  DayPart findNextDayPart(Date date) {
    return findNextDayPart(date, 0);
  }

  /**
   * Finds the business day part that starts on or after the given date and determines the time
   * that day part begins.
   * 
   * @param date on input, the base date; on output, the day part start date.
   * <strong>Beware!</strong> The method modifies the date.
   */
  DayPart findNextDayPart(Date date, int dayPartIndex) {
    // if there is a day part that starts after the given date
    if (dayPartIndex < dayParts.length) {
      DayPart dayPart = dayParts[dayPartIndex];
      if (dayPart.isStartAfter(date)) {
        date.setTime(dayPart.getStartTime(date).getTime());
        return dayPart;
      }
      else {
        return findNextDayPart(date, dayPartIndex + 1);
      }
    }
    else {
      // descend recursively
      date.setTime(businessCalendar.findStartOfNextDay(date).getTime());
      Day nextDay = businessCalendar.findDay(date);
      return nextDay.findNextDayPart(date);
    }
  }

  DayPart findPreviousDayPart(Date date) {
    return findPreviousDayPart(date, dayParts.length - 1);
  }

  /**
   * Finds the business day part that ends on or before the given date and determines the time
   * that day part ends.
   * 
   * @param date on input, the base date; on output, the day part end date.
   * <strong>Beware!</strong> The method modifies the date.
   */
  DayPart findPreviousDayPart(Date date, int dayPartIndex) {
    // if there is a day part that ends before the given date
    if (dayPartIndex >= 0) {
      DayPart dayPart = dayParts[dayPartIndex];
      if (dayPart.endsBefore(date)) {
        date.setTime(dayPart.getEndTime(date).getTime());
        return dayPart;
      }
      else {
        return findPreviousDayPart(date, dayPartIndex - 1);
      }
    }
    else {
      date.setTime(businessCalendar.findEndOfPreviousDay(date).getTime());
      Day previousDay = businessCalendar.findDay(date);
      return previousDay.findPreviousDayPart(date);
    }
  }
}
