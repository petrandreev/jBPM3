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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.jbpm.AbstractJbpmTestCase;

public class HolidayTest extends AbstractJbpmTestCase {

  public void testHolidaySingleDayParsing() throws Exception {
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    Holiday holiday = new Holiday("21/07/2005", dateFormat, null);
    
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.JULY);
    calendar.set(Calendar.DAY_OF_MONTH, 21);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date expectedFromDay = calendar.getTime();

    assertEquals(expectedFromDay, holiday.startDate);
    
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.JULY);
    calendar.set(Calendar.DAY_OF_MONTH, 22);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date expectedToDay = calendar.getTime();

    assertEquals(expectedToDay, holiday.endDate);
  }

  public void testHolidayMulitDayParsing() throws Exception {
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    Holiday holiday = new Holiday("1/7/2005 - 31/8/2005", dateFormat, null);

    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.JULY);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date expectedFromDay = calendar.getTime();

    assertEquals(expectedFromDay, holiday.startDate);
    
    calendar.set(Calendar.YEAR, 2005);
    calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date expectedToDay = calendar.getTime();

    assertEquals(expectedToDay, holiday.endDate);
  }
}
