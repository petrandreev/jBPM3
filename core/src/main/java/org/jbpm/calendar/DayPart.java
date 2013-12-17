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
import java.text.*;
import java.util.*;

/**
 * is part of a day that can for example be used to represent business hours.
 * 
 */
public class DayPart implements Serializable {

  private static final long serialVersionUID = 1L;

  final int startHour;
  final int startMinute;
  final int endHour;
  final int endMinute;

  private final Day day;
  private final int index;

  public DayPart(String dayPartText, DateFormat timeFormat, Day day, int index) {
    // parse start time
    ParsePosition parsePosition = new ParsePosition(Duration.indexOfNonWhite(dayPartText, 0));
    Date startTime = timeFormat.parse(dayPartText, parsePosition);
    if (startTime == null) {
      throw new IllegalArgumentException("failed to parse day part start time: " + dayPartText);
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startTime);
    startHour = calendar.get(Calendar.HOUR_OF_DAY);
    startMinute = calendar.get(Calendar.MINUTE);

    // check time separator
    int separatorIndex = Duration.indexOfNonWhite(dayPartText, parsePosition.getIndex());
    if (dayPartText.charAt(separatorIndex) != '-') {
      throw new IllegalArgumentException("missing '-' in day part: " + dayPartText);
    }

    // parse end time
    parsePosition.setIndex(separatorIndex + 1);
    Date endTime = timeFormat.parse(dayPartText, parsePosition);
    if (endTime == null) {
      throw new IllegalArgumentException("failed to parse day part end time: " + dayPartText);
    }
    calendar.setTime(endTime);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    endHour = hour == 0 ? 24 : hour;
    endMinute = calendar.get(Calendar.MINUTE);

    this.day = day;
    this.index = index;
  }

  public Date add(Date date, Duration duration) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);
    long durationMillis = duration.getMilliseconds();

    if (duration.isNegative()) {
      long dayPartMillis = (startHour - hour) * Duration.HOUR + (startMinute - minute)
        * Duration.MINUTE;
      if (durationMillis >= dayPartMillis) return duration.addTo(date);

      Duration remainder = new Duration(durationMillis - dayPartMillis);
      Date dayPartStartDate = new Date(date.getTime() + dayPartMillis);

      DayPart previousDayPart = day.findPreviousDayPart(dayPartStartDate, index - 1);
      Date previousDayPartEnd = dayPartStartDate;

      return previousDayPart.add(previousDayPartEnd, remainder);
    }
    else {
      long dayPartMillis = (endHour - hour) * Duration.HOUR + (endMinute - minute)
        * Duration.MINUTE;
      if (durationMillis <= dayPartMillis) return duration.addTo(date);

      Duration remainder = new Duration(durationMillis - dayPartMillis);
      Date dayPartEndDate = new Date(date.getTime() + dayPartMillis);

      DayPart nextDayPart = day.findNextDayPart(dayPartEndDate, index + 1);
      Date nextDayPartStart = dayPartEndDate;

      return nextDayPart.add(nextDayPartStart, remainder);
    }
  }

  public boolean isStartAfter(Date time) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(time);

    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    return startHour > hour
      || (startHour == hour && startMinute >= calendar.get(Calendar.MINUTE));
  }

  boolean endsBefore(Date time) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(time);

    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    return endHour < hour || (endHour == hour && endMinute <= calendar.get(Calendar.MINUTE));
  }

  public boolean includes(Date time) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(time);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);

    return (startHour < hour || (startHour == hour && startMinute <= minute))
      && (hour < endHour || (hour == endHour && minute <= endMinute));
  }

  public Date getStartTime(Date time) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(time);
    calendar.set(Calendar.HOUR_OF_DAY, startHour);
    calendar.set(Calendar.MINUTE, startMinute);
    return calendar.getTime();
  }

  Date getEndTime(Date time) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(time);
    calendar.set(Calendar.HOUR_OF_DAY, endHour);
    calendar.set(Calendar.MINUTE, endMinute);
    return calendar.getTime();
  }
}
