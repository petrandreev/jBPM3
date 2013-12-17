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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.util.ClassLoaderUtil;

/**
 * a calendar that knows about business hours.
 */
public class BusinessCalendar implements Serializable {

  private static final long serialVersionUID = 1L;

  private final Day[] weekDays;
  private final List holidays;

  private static final Map propertiesByResource = new HashMap();

  public static Properties getBusinessCalendarProperties() {
    String calendarResource = Configs.getString("resource.business.calendar");
    return getBusinessCalendarProperties(calendarResource);
  }

  private static Properties getBusinessCalendarProperties(String calendarResource) {
    synchronized (propertiesByResource) {
      Properties properties = (Properties) propertiesByResource.get(calendarResource);
      if (properties == null) {
        properties = ClassLoaderUtil.getProperties(calendarResource);
        propertiesByResource.put(calendarResource, properties);
      }
      return properties;
    }
  }

  public BusinessCalendar() {
    /*
     * loading properties at construction time used to be bad, as business calendars were kept
     * in static attributes of persistent classes, resulting in the default configuration being
     * unduly loaded; however, business calendars are no longer static.
     * see http://community.jboss.org/message/404365
     */
    this(getBusinessCalendarProperties());
  }

  public BusinessCalendar(String calendarResource) {
    this(getBusinessCalendarProperties(calendarResource));
  }

  public BusinessCalendar(Properties calendarProperties) {
    weekDays = Day.parseWeekDays(calendarProperties, this);
    holidays = Holiday.parseHolidays(calendarProperties, this);
  }

  public Day[] getWeekDays() {
    return weekDays;
  }

  public List getHolidays() {
    return holidays;
  }

  public Date add(Date date, Duration duration) {
    Date end = null;
    if (duration.isBusinessTime()) {
      DayPart dayPart = findDayPart(date);
      if (dayPart == null) {
        // outside business hours
        Day day = findDay(date);
        if (duration.isNegative()) {
          dayPart = day.findPreviousDayPart(date);
        }
        else {
          dayPart = day.findNextDayPart(date);
        }
      }
      end = dayPart.add(date, duration);
    }
    else {
      end = duration.addTo(date);
    }
    return end;
  }

  public Date findStartOfNextDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DATE, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    date = calendar.getTime();
    while (isHoliday(date)) {
      calendar.setTime(date);
      calendar.add(Calendar.DATE, 1);
      date = calendar.getTime();
    }
    return date;
  }

  Date findEndOfPreviousDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DATE, -1);
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    date = calendar.getTime();
    while (isHoliday(date)) {
      calendar.setTime(date);
      calendar.add(Calendar.DATE, -1);
      date = calendar.getTime();
    }
    return date;
  }

  public Day findDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return getWeekDays()[calendar.get(Calendar.DAY_OF_WEEK)];
  }

  public boolean isHoliday(Date date) {
    for (Iterator iter = getHolidays().iterator(); iter.hasNext();) {
      Holiday holiday = (Holiday) iter.next();
      if (holiday.includes(date)) return true;
    }
    return false;
  }

  private DayPart findDayPart(Date date) {
    if (!isHoliday(date)) {
      Day day = findDay(date);
      for (int i = 0; i < day.dayParts.length; i++) {
        DayPart dayPart = day.dayParts[i];
        if (dayPart.includes(date)) return dayPart;
      }
    }
    return null;
  }

  public DayPart findNextDayPart(Date date) {
    DayPart nextDayPart = findDayPart(date);
    if (nextDayPart == null) {
      date = findStartOfNextDay(date);
      Day day = findDay(date);
      nextDayPart = day.findNextDayPart(date);
    }
    return nextDayPart;
  }

  public boolean isInBusinessHours(Date date) {
    return findDayPart(date) != null;
  }

  /** @deprecated call {@link Calendar#getInstance()} directly */
  public static Calendar getCalendar() {
    return Calendar.getInstance();
  }
}
