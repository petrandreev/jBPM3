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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.util.ClassLoaderUtil;

/**
 * a calendar that knows about business hours.
 */
public class BusinessCalendar implements Serializable {
  
  private static final long serialVersionUID = 1L;
  static Properties businessCalendarProperties = null;
  
  private Day[] weekDays = null;
  private List holidays = null;

  public static synchronized Properties getBusinessCalendarProperties() {
    if (businessCalendarProperties==null) {
      String resource = JbpmConfiguration.Configs.getString("resource.business.calendar");
      businessCalendarProperties = ClassLoaderUtil.getProperties(resource);
    }
    return businessCalendarProperties;
  }

  public BusinessCalendar() {
    // don't load the properties during creation time!
    // see http://www.jboss.com/index.html?module=bb&op=viewtopic&p=4158259
    // this(getBusinessCalendarProperties());
  }
  
  public BusinessCalendar(Properties calendarProperties) {
    try {
      weekDays = Day.parseWeekDays(calendarProperties, this);
      holidays = Holiday.parseHolidays(calendarProperties, this);

    } catch (Exception e) {
      throw new JbpmException("couldn't create business calendar", e);
    }
  }
  
  public Day[] getWeekDays() {
    if (weekDays==null)
      // lazy load properties if not set during creation
      weekDays = Day.parseWeekDays(getBusinessCalendarProperties(), this);
    return weekDays;
  }

  public List getHolidays() {
    if (holidays==null)
      // lazy load properties if not set during creation
      holidays = Holiday.parseHolidays(getBusinessCalendarProperties(), this);
    return holidays;
  }

  public Date add(Date date, Duration duration) {
    Date end = null;
    if (duration.isBusinessTime()) {
      DayPart dayPart = findDayPart(date);
      if (dayPart == null) {
        Object[] result = new Object[2];
        findDay(date).findNextDayPartStart(0, date, result);
        date = (Date) result[0];
        dayPart = (DayPart) result[1];
      }
      end = dayPart.add(date, duration);
    } else {
      end = duration.addTo(date);
    }
    return end;
  }

  public Date findStartOfNextDay(Date date) {
    Calendar calendar = getCalendar();
    calendar.setTime(date);
    calendar.add(Calendar.DATE, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    date = calendar.getTime();
    while(isHoliday(date)) {
      calendar.setTime(date);
      calendar.add(Calendar.DATE, 1);
      date = calendar.getTime();
    }
    return date;
  }

  public Day findDay(Date date) {
    Calendar calendar = getCalendar();
    calendar.setTime(date);
    return getWeekDays()[calendar.get(Calendar.DAY_OF_WEEK)];
  }

  public boolean isHoliday(Date date) {
    Iterator iter = getHolidays().iterator();
    while (iter.hasNext()) {
      Holiday holiday = (Holiday) iter.next();
      if (holiday.includes(date)) {
        return true;
      }
    }
    return false;
  }

  DayPart findDayPart(Date date) {
    DayPart dayPart = null;
    if (! isHoliday(date)) {
      Day day = findDay(date);
      for (int i=0; ((i < day.dayParts.length)
                     && (dayPart==null)); i++) {
        DayPart candidate = day.dayParts[i];
        if (candidate.includes(date)) {
          dayPart = candidate;
        }
      }
    }
    return dayPart;
  }

  public DayPart findNextDayPart(Date date) { 
    DayPart nextDayPart = null; 
    while(nextDayPart==null) { 
      nextDayPart = findDayPart(date); 
      if (nextDayPart==null) { 
        date = findStartOfNextDay(date); 
        Object result[] = new Object[2]; 
        Day day = findDay(date); 
        day.findNextDayPartStart(0, date, result); 
        nextDayPart = (DayPart) result[1]; 
      } 
    } 
    return nextDayPart; 
  } 

  public boolean isInBusinessHours(Date date) { 
    return (findDayPart(date)!=null); 
  } 

  public static Calendar getCalendar() {
    return new GregorianCalendar();
  }
}
