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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * identifies a continuous set of days.
 */
public class Holiday implements Serializable {

  private static final long serialVersionUID = 1L;

  final Date startDate;
  final Date endDate;

  public static List parseHolidays(Properties calendarProperties,
    BusinessCalendar businessCalendar) {
    List holidays = new ArrayList();

    DateFormat dateFormat = new SimpleDateFormat(calendarProperties.getProperty("day.format"));
    for (Iterator iter = calendarProperties.keySet().iterator(); iter.hasNext();) {
      String key = (String) iter.next();
      if (key.startsWith("holiday")) {
        Holiday holiday = new Holiday(calendarProperties.getProperty(key), dateFormat,
          businessCalendar);
        holidays.add(holiday);
      }
    }

    return holidays;
  }

  public Holiday(String holidayText, DateFormat dateFormat, BusinessCalendar businessCalendar) {
    ParsePosition parsePosition = new ParsePosition(Duration.indexOfNonWhite(holidayText, 0));
    startDate = dateFormat.parse(holidayText, parsePosition);
    if (startDate == null) {
      throw new IllegalArgumentException("failed to parse holiday start date: " + holidayText);
    }

    Date end;
    int separatorIndex = Duration.indexOfNonWhite(holidayText, parsePosition.getIndex());
    if (separatorIndex != -1) {
      if (holidayText.charAt(separatorIndex) != '-') {
        throw new IllegalArgumentException("expected '-' in holiday date range: " + holidayText);
      }

      parsePosition.setIndex(Duration.indexOfNonWhite(holidayText, separatorIndex + 1));
      end = dateFormat.parse(holidayText, parsePosition);
      if (end == null) {
        throw new IllegalArgumentException("failed to parse holiday end date: " + holidayText);
      }
    }
    else {
      end = startDate;
    }
    // now set endDate to the end of the day, rather then the beginning
    // take the start of the next day as the end of the endDate
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(end);
    calendar.add(Calendar.DATE, 1);
    endDate = calendar.getTime();
  }

  public boolean includes(Date date) {
    return startDate.getTime() <= date.getTime() && date.getTime() < endDate.getTime();
  }
}
