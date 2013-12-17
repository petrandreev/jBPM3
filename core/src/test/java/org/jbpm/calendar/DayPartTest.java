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

import org.jbpm.AbstractJbpmTestCase;

public class DayPartTest extends AbstractJbpmTestCase {

  public void testDayPartParsing() {
    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
    DayPart dayPart = new DayPart("9:00-12:15", dateFormat, null, 0);
    assertEquals(9, dayPart.startHour);
    assertEquals(0, dayPart.startMinute);
    assertEquals(12, dayPart.endHour);
    assertEquals(15, dayPart.endMinute);
  }

  public void testDayPartWithSpacesParsing() {
    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
    DayPart dayPart = new DayPart(" 9:00 - 12:15 ", dateFormat, null, 0);
    assertEquals(9, dayPart.startHour);
    assertEquals(0, dayPart.startMinute);
    assertEquals(12, dayPart.endHour);
    assertEquals(15, dayPart.endMinute);
  }

  public void testDayPartAmPmParsing() {
    DateFormat dateFormat = new SimpleDateFormat("hh'h'mma");
    DayPart dayPart = new DayPart("9h00am-12h15pm", dateFormat, null, 0);
    assertEquals(9, dayPart.startHour);
    assertEquals(0, dayPart.startMinute);
    assertEquals(12, dayPart.endHour);
    assertEquals(15, dayPart.endMinute);
  }
  
}
