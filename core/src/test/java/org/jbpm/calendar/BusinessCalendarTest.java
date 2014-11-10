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

import java.util.Calendar;
import java.util.Date;

import org.jbpm.AbstractJbpmTestCase;

public class BusinessCalendarTest extends AbstractJbpmTestCase {

  static BusinessCalendar businessCalendar = new BusinessCalendar();

  public void testNonBusinessSecondAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 10, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.SECOND, 20);
    Date expected = calendar.getTime();

    Date actual = businessCalendar.add(start, new Duration("20 seconds"));
    assertEquals(expected, actual);
  }

  public void testNonBusinessMinuteAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 11, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, 10);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 minutes")));
  }

  public void testNonBusinessFractionalDurationAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.HOUR, 7);
    calendar.add(Calendar.MINUTE, 45);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("7.75 hours")));
  }

  public void testNonBusinessDayAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.DAY_OF_MONTH, 10);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 days")));
  }

  public void testNonBusinessWeekAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.WEEK_OF_YEAR, 5);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("5 weeks")));
  }

  public void testNonBusinessMonthAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.MONTH, 3);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("3 months")));
  }

  public void testNonBusinessYearAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.YEAR, 1);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("1 year")));
  }

  public void testBusinessDurationAdditionOverBusinessTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 11, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, 1);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("1 business minute")));
  }

  public void testBusinessDurationAdditionOverLunchBreak() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 11, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, 10);
    // lunch break spans 30 minutes
    calendar.add(Calendar.MINUTE, 30);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 business minutes")));
  }

  public void testBusinessDurationAdditionOverDayBreak() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 16, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, 10);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, 16);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 business minutes")));
  }

  public void testBusinessDurationAdditionOverHoliday() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2010, Calendar.NOVEMBER, 10, 16, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, 10);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, 16);
    // holiday
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 business minutes")));
  }

  public void testBusinessDurationAdditionOverWeekend() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 8, 16, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, 10);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, 16);
    // weekend
    calendar.add(Calendar.DAY_OF_MONTH, 2);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 business minutes")));
  }

  public void testTwoBusinessHoursOverLunch() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 11, 15, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.HOUR, 2);
    // lunch break spans 30 minutes
    calendar.add(Calendar.MINUTE, 30);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("2 business hours")));
  }

  public void testBusinessDurationAdditionOutsideBusinessHours() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 8, 12, 15, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, 30);
    // lunch break ends at 12:30
    calendar.add(Calendar.MINUTE, 15);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("30 business minutes")));
  }

  public void testBusinessDurationAdditionOutsideBusinessHoursOverWeekend() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 8, 12, 15, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.HOUR, 5);
    // lunch break ends at 12:30
    calendar.add(Calendar.MINUTE, 15);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, 16);
    // weekend
    calendar.add(Calendar.DAY_OF_MONTH, 2);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("5 business hours")));
  }

  public void testBusinessFractionalDurationAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.HOUR, 7);
    calendar.add(Calendar.MINUTE, 45);
    // lunch break spans 30 minutes
    calendar.add(Calendar.MINUTE, 30);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, 16);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("7.75 business hours")));
  }

  public void testBusinessDayAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.DAY_OF_MONTH, 21);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 business days")));
  }

  public void testBusinessWeekAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.MARCH);
    calendar.set(Calendar.DAY_OF_MONTH, 14);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("5 business weeks")));
  }

  public void testBusinessMonthAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2010, Calendar.FEBRUARY, 8, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.MAY);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("3 business months")));
  }

  public void testBusinessYearAddition() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2009, Calendar.DECEMBER, 28, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.YEAR, 2010);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("1 business year")));
  }
}
