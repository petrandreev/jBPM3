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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.jbpm.JbpmException;

/**
 * identifies a continuous set of days.
 */
public class Holiday implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  Date fromDay = null;
  Date toDay = null;
  BusinessCalendar businessCalendar = null;

  public static List parseHolidays(Properties calendarProperties, BusinessCalendar businessCalendar) {
    List holidays = new ArrayList();
    
    DateFormat dateFormat = new SimpleDateFormat(calendarProperties.getProperty("day.format"));
    Iterator iter = calendarProperties.keySet().iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      if (key.startsWith("holiday")) {
        Holiday holiday = new Holiday(calendarProperties.getProperty(key), dateFormat, businessCalendar);
        holidays.add(holiday);
      }
    }

    return holidays;
  }

  public Holiday(String holidayText, DateFormat dateFormat, BusinessCalendar businessCalendar) {
    this.businessCalendar = businessCalendar;
    try {
      int separatorIndex = holidayText.indexOf('-');
      if (separatorIndex==-1) {
        fromDay = dateFormat.parse(holidayText.trim());
        toDay = fromDay;
      } else {
        String fromText = holidayText.substring(0, separatorIndex).trim();
        String toText = holidayText.substring(separatorIndex+1).trim();
        fromDay = dateFormat.parse(fromText);
        toDay = dateFormat.parse(toText);
      }
      // now we are going to set the toDay to the end of the day, rather then the beginning.
      // we take the start of the next day as the end of the toDay.
      Calendar calendar = BusinessCalendar.getCalendar();
      calendar.setTime(toDay);
      calendar.add(Calendar.DATE, 1);
      toDay = calendar.getTime();
      
    } catch (ParseException e) {
      throw new JbpmException("couldn't parse holiday '"+holidayText+"'", e);
    }
  }

  public boolean includes(Date date) {
    return ( (fromDay.getTime()<=date.getTime())
             && (date.getTime()<toDay.getTime())
           );
  }
}
