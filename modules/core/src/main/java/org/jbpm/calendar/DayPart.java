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
import java.text.DateFormat;
import java.util.*;

/**
 * is part of a day that can for example be used to represent business hours. 
 *
 */
public class DayPart implements Serializable {

  private static final long serialVersionUID = 1L;
  
  int fromHour = -1;
  int fromMinute = -1;
  int toHour = -1;
  int toMinute = -1;
  Day day = null;
  int index = -1;

  public DayPart(String dayPartText, DateFormat dateFormat, Day day, int index) {
    this.day = day;
    this.index = index;
    
    int separatorIndex = dayPartText.indexOf('-');
    if (separatorIndex==-1) throw new IllegalArgumentException("improper format of daypart '"+dayPartText+"'");
    String fromText = dayPartText.substring(0, separatorIndex).trim().toLowerCase(); 
    String toText = dayPartText.substring(separatorIndex+1).trim().toLowerCase();
    
    try {
      Date from = dateFormat.parse(fromText);
      Date to = dateFormat.parse(toText);
      
      Calendar calendar = BusinessCalendar.getCalendar();
      calendar.setTime(from);
      fromHour = calendar.get(Calendar.HOUR_OF_DAY);
      fromMinute = calendar.get(Calendar.MINUTE);

      calendar.setTime(to);
      toHour = calendar.get(Calendar.HOUR_OF_DAY);
      if (toHour==0) {
        toHour=24;
      }
      toMinute = calendar.get(Calendar.MINUTE);

    } catch (ParseException e) {
      throw new IllegalArgumentException("improper format of daypart '" + dayPartText + "'");
    }
  }

  public Date add(Date date, Duration duration) {
    Date end = null;
    
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.setTime(date);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);

    long millisInThisDayPart = (toHour - hour) * Duration.HOUR + (toMinute - minute) * Duration.MINUTE;
    long durationMillis = duration.getMilliseconds();
    
    if (durationMillis <= millisInThisDayPart) {
      end = duration.addTo(date);
    } else {
      Duration remainder = new Duration(durationMillis - millisInThisDayPart);
      Date dayPartEndDate = new Date(date.getTime() + millisInThisDayPart);
      
      Object[] result = new Object[2];
      day.findNextDayPartStart(index+1, dayPartEndDate, result);
      Date nextDayPartStart = (Date) result[0];
      DayPart nextDayPart = (DayPart) result[1];
      
      end = nextDayPart.add(nextDayPartStart, remainder);
    }
    
    return end;
  }
  
  public boolean isStartAfter(Date date) {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.setTime(date);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);
    
    return ( (hour<fromHour)
             || ( (hour==fromHour)
                  && (minute<=fromMinute) 
                ) 
           );
  }


  public boolean includes(Date date) {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.setTime(date);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);
    
    return ( ( (fromHour<hour)
               || ( (fromHour==hour)
                   && (fromMinute<=minute) 
                 )
             ) &&
             ( (hour<toHour)
               || ( (hour==toHour)
                    && (minute<=toMinute) 
                  )
             )
           );
  }

  public Date getStartTime(Date date) {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, fromHour);
    calendar.set(Calendar.MINUTE, fromMinute);
    return calendar.getTime();
  }
}
