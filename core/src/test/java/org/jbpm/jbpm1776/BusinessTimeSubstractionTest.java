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
package org.jbpm.jbpm1776;

import java.util.Calendar;
import java.util.Date;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;

/**
 * Business time subtraction delivers unexpected results.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-1776">JBPM-1776</a>
 * @author Alejandro Guizar
 */
public class BusinessTimeSubstractionTest extends AbstractJbpmTestCase {

  static BusinessCalendar businessCalendar = new BusinessCalendar();

  public void testNonBusinessSecondSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 10, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.SECOND, -20);
    Date expected = calendar.getTime();

    Date actual = businessCalendar.add(start, new Duration("-20 seconds"));
    assertEquals(expected, actual);
  }

  public void testNonBusinessMinuteSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 11, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, -10);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-10 minutes")));
  }

  public void testNonBusinessFractionalDurationSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.HOUR, -7);
    calendar.add(Calendar.MINUTE, -45);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-7.75 hours")));
  }

  public void testNonBusinessDaySubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.DAY_OF_MONTH, -10);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-10 days")));
  }

  public void testNonBusinessWeekSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.WEEK_OF_YEAR, -5);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-5 weeks")));
  }

  public void testNonBusinessMonthSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.MONTH, -3);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-3 months")));
  }

  public void testNonBusinessYearSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.add(Calendar.YEAR, -1);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-1 year")));
  }

  public void testBusinessDurationSubtractionOverBusinessTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 11, 55, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, -1);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-1 business minute")));
  }

  public void testBusinessDurationSubtractionOverLunchBreak() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 12, 35, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, -10);
    // lunch break spans 30 minutes
    calendar.add(Calendar.MINUTE, -30);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-10 business minutes")));
  }

  public void testBusinessDurationSubtractionOverDayBreak() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 8, 9, 5, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, -10);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, -16);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-10 business minutes")));
  }

  public void testBusinessDurationSubtractionOverHoliday() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2010, Calendar.NOVEMBER, 12, 9, 5, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, -10);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, -16);
    // holiday
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-10 business minutes")));
  }

  public void testBusinessDurationSubtractionOverWeekend() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 11, 9, 5, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, -10);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, -16);
    // weekend
    calendar.add(Calendar.DAY_OF_MONTH, -2);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-10 business minutes")));
  }

  public void testTwoBusinessHoursOverLunch() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 7, 13, 45, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.HOUR, -2);
    // lunch break spans 30 minutes
    calendar.add(Calendar.MINUTE, -30);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-2 business hours")));
  }

  public void testBusinessDurationSubtractionOutsideBusinessHours() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 8, 12, 15, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.MINUTE, -30);
    // lunch break ends at 12:30
    calendar.add(Calendar.MINUTE, -15);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-30 business minutes")));
  }

  public void testBusinessDurationSubtractionOutsideBusinessHoursOverWeekend() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.APRIL, 11, 12, 15, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.add(Calendar.HOUR, -5);
    // lunch break ends at 12:30
    calendar.add(Calendar.MINUTE, -15);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, -16);
    // weekend
    calendar.add(Calendar.DAY_OF_MONTH, -2);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-5 business hours")));
  }

  public void testBusinessDaySubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.FEBRUARY, 21, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.DAY_OF_MONTH, 7);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-10 business days")));
  }

  public void testBusinessWeekSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2005, Calendar.MARCH, 14, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.FEBRUARY);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-5 business weeks")));
  }

  public void testBusinessMonthSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2010, Calendar.MAY, 4, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.FEBRUARY);
    calendar.set(Calendar.DAY_OF_MONTH, 3);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-3 business months")));
  }

  public void testBusinessYearSubtraction() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2010, Calendar.DECEMBER, 28, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.YEAR, 2009);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("-1 business year")));
  }
}
