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

  public void testNonBusinessDurationAddition() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 10);
    calendar.set(Calendar.MINUTE, 30);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    Date twentySecondsLater = 
      businessCalendar.add(start, new Duration("20 seconds"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 10);
    calendar.set(Calendar.MINUTE, 30);
    calendar.set(Calendar.SECOND, 20);
    calendar.set(Calendar.MILLISECOND, 0);
    Date expected = calendar.getTime();

    assertEquals(expected, twentySecondsLater);
  }

  public void testNonBusinessDurationAdditionOverNonBusinessTime() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 11);
    calendar.set(Calendar.MINUTE, 55);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveBeforeTwelve = calendar.getTime();

    Date tenMinutesLaterThenFiveBeforeTwelve = 
      businessCalendar.add(fiveBeforeTwelve, new Duration("10 minutes"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, 5);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveAfterTwelve = calendar.getTime();

    assertEquals(fiveAfterTwelve, tenMinutesLaterThenFiveBeforeTwelve);
  }

  public void testNonBusinessFractionalDurationAddition() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    Date fiveWeeksLater = businessCalendar.add(start, new Duration("7.75 hours"));

    calendar.set(Calendar.HOUR_OF_DAY, 14);
    calendar.set(Calendar.MINUTE, 15);
    Date expected = calendar.getTime();

    assertEquals(expected, fiveWeeksLater);
  }

  public void testNonBusinessDayAddition() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2005, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.set(Calendar.DAY_OF_MONTH, 15);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 days")));
  }

  public void testNonBusinessWeekAddition() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.MARCH);
    calendar.set(Calendar.DAY_OF_MONTH, 11);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("5 weeks")));
  }

  public void testNonBusinessMonthAddition() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.MAY);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("3 months")));
  }

  public void testNonBusinessYearAddition() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2008, Calendar.FEBRUARY, 5, 6, 30, 45);
    calendar.set(Calendar.MILLISECOND, 125);
    Date start = calendar.getTime();

    calendar.set(Calendar.YEAR, 2009);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("1 year")));
  }

  public void testBusinessDurationAdditionInBusinessTime() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 11);
    calendar.set(Calendar.MINUTE, 55);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveBeforeTwelve = calendar.getTime();

    Date oneMinuteLaterThenFiveBeforeTwelve = 
      businessCalendar.add(fiveBeforeTwelve, new Duration("1 business minute"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 11);
    calendar.set(Calendar.MINUTE, 56);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fourBeforeTwelve = calendar.getTime();

    assertEquals(fourBeforeTwelve, oneMinuteLaterThenFiveBeforeTwelve);
  }

  public void testBusinessDurationAdditionOverLunchBreak() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 11);
    calendar.set(Calendar.MINUTE, 55);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveBeforeTwelve = calendar.getTime();

    Date tenBusinessMinutesAfterFiveBeforeTwelve = 
      businessCalendar.add(fiveBeforeTwelve, new Duration("10 business minutes"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, 35);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date twelveThirtyfive = calendar.getTime();

    assertEquals(twelveThirtyfive, tenBusinessMinutesAfterFiveBeforeTwelve);
  }

  public void testBusinessDurationAdditionOverDayBreak() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 16);
    calendar.set(Calendar.MINUTE, 55);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveBeforeFive = calendar.getTime();

    Date tenBusinessMinutesAfterFiveBeforeFive = 
      businessCalendar.add(fiveBeforeFive, new Duration("10 business minutes"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 9);
    calendar.set(Calendar.MINUTE, 5);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveAfterNineNextDay = calendar.getTime();

    assertEquals(fiveAfterNineNextDay, tenBusinessMinutesAfterFiveBeforeFive);
  }

  public void testBusinessDurationAdditionOverHolidayBreak() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.MAY);
    calendar.set(Calendar.DAY_OF_MONTH, 4);
    calendar.set(Calendar.HOUR_OF_DAY, 16);
    calendar.set(Calendar.MINUTE, 55);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveBeforeFive = calendar.getTime();

    Date tenBusinessMinutesAfterFiveBeforeFive = 
      businessCalendar.add(fiveBeforeFive, new Duration("10 business minutes"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.MAY);
    calendar.set(Calendar.DAY_OF_MONTH, 6);
    calendar.set(Calendar.HOUR_OF_DAY, 9);
    calendar.set(Calendar.MINUTE, 5);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveAfterNineTwoDaysLater = calendar.getTime();

    assertEquals(fiveAfterNineTwoDaysLater, tenBusinessMinutesAfterFiveBeforeFive);
  }

  public void testBusinessDurationAdditionOverWeekendBreak() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 16);
    calendar.set(Calendar.MINUTE, 55);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveBeforeFive = calendar.getTime();

    Date tenBusinessMinutesAfterFiveBeforeFive = 
      businessCalendar.add(fiveBeforeFive, new Duration("10 business minutes"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 11);
    calendar.set(Calendar.HOUR_OF_DAY, 9);
    calendar.set(Calendar.MINUTE, 5);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date fiveAfterNineAfterTheWeekend = calendar.getTime();

    assertEquals(fiveAfterNineAfterTheWeekend, tenBusinessMinutesAfterFiveBeforeFive);
  }

  public void testTwoBusinessHoursOverLunch() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 11);
    calendar.set(Calendar.MINUTE, 15);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date quarterPastTwelve = calendar.getTime(); 

    Date twoBusinessHoursAfterFiveBeforeFive = 
      businessCalendar.add(quarterPastTwelve, new Duration("2 business hours"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 7);
    calendar.set(Calendar.HOUR_OF_DAY, 13);
    calendar.set(Calendar.MINUTE, 45);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date rightAfterLunch = calendar.getTime();

    assertEquals(rightAfterLunch, twoBusinessHoursAfterFiveBeforeFive);
  }

  public void testBusinessDurationAdditionStartingOutsideBusinessHours() throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, 15);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date quarterPastTwelve = calendar.getTime();

    Date tenBusinessMinutesAfterQuarterPastTwelve = 
      businessCalendar.add(quarterPastTwelve, new Duration("30 business minutes"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 13);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date oneOClock = calendar.getTime();

    assertEquals(oneOClock, tenBusinessMinutesAfterQuarterPastTwelve);
  }

  public void testBusinessDurationAdditionStartingOutsideBusinessHoursAndOverWeekend()
      throws Exception {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, 15);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date quarterPastTwelve = calendar.getTime();

    Date fiveBusinessHoursAfterQuarterPastTwelve = 
      businessCalendar.add(quarterPastTwelve, new Duration("5 business hours"));
    
    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 11);
    calendar.set(Calendar.HOUR_OF_DAY, 9);
    calendar.set(Calendar.MINUTE, 30);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date halfPastNineAfterTheWeekend = calendar.getTime();

    assertEquals(halfPastNineAfterTheWeekend, fiveBusinessHoursAfterQuarterPastTwelve);
  }

  public void testBusinessFractionalDurationAddition() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.MINUTE, 45);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("7.75 business hours")));
  }

  public void testBusinessDayAddition() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.DAY_OF_MONTH, 21);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("10 business days")));
  }

  public void testBusinessWeekAddition() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.MARCH);
    calendar.set(Calendar.DAY_OF_MONTH, 14);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("5 business weeks")));
  }

  public void testBusinessMonthAddition() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.MONTH, Calendar.MAY);
    calendar.set(Calendar.DAY_OF_MONTH, 9);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("3 business months")));
  }

  public void testBusinessYearAddition() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(2005, Calendar.FEBRUARY, 7, 9, 30, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();

    calendar.set(Calendar.YEAR, 2006);
    calendar.set(Calendar.DAY_OF_MONTH, 6);
    Date expected = calendar.getTime();

    assertEquals(expected, businessCalendar.add(start, new Duration("1 business year")));
  }

  public void testNextDayStart() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 13);
    calendar.set(Calendar.MINUTE, 15);
    Date justAfterLunchOnAprilEight2005 = calendar.getTime();
    Date startOfNextDay = businessCalendar.findStartOfNextDay(justAfterLunchOnAprilEight2005);

    calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 9);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    assertEquals(calendar.getTime(), startOfNextDay);
  }

  public void testDayOfWeek() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 13);
    calendar.set(Calendar.MINUTE, 15);
    Date justAfterLunchOnAprilEight2005 = calendar.getTime();

    Day day = businessCalendar.findDay(justAfterLunchOnAprilEight2005);
    assertSame(businessCalendar.getWeekDays()[Calendar.FRIDAY], day);
  }

  public void testFindNextDayPart() {
    Calendar calendar = BusinessCalendar.getCalendar();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.APRIL);
    calendar.set(Calendar.DAY_OF_MONTH, 8);
    calendar.set(Calendar.HOUR_OF_DAY, 21);
    calendar.set(Calendar.MINUTE, 15);
    Date outsideBusinessHours = calendar.getTime();

    BusinessCalendar bc = new BusinessCalendar();
    bc.findNextDayPart(outsideBusinessHours);
  }
}
