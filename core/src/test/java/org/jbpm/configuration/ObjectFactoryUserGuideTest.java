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
package org.jbpm.configuration;

import java.util.List;
import java.util.Map;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class ObjectFactoryUserGuideTest extends AbstractJbpmTestCase {
  
  protected ObjectFactoryImpl objectFactory = null;

  protected void parse(String xml) {
    objectFactory = ObjectFactoryParser.parseXmlString(xml);
  }


  public void testBean() {
    parse(
      "<beans>" +
      "  <bean name='task' class='org.jbpm.taskmgmt.exe.TaskInstance'/>" +
      "  <string name='greeting'>hello world</string>" +
      "  <int name='answer'>42</int>" +
      "  <boolean name='javaisold'>true</boolean>" +
      "  <float name='percentage'>10.2</float>" +
      "  <double name='salary'>100000000.32</double>" +
      "  <char name='java'>j</char>" +
      "  <null name='dusttodust'/>" +
      "</beans>"
    );
    
    assertEquals(TaskInstance.class, objectFactory.createObject("task").getClass());
    assertEquals("hello world", objectFactory.createObject("greeting"));
    assertEquals(new Integer(42), objectFactory.createObject("answer"));
    assertEquals(Boolean.TRUE, objectFactory.createObject("javaisold"));
    assertEquals(new Float(10.2), objectFactory.createObject("percentage"));
    assertEquals(new Double(100000000.32), objectFactory.createObject("salary"));
    assertEquals(new Character('j'), objectFactory.createObject("java"));
    assertNull(objectFactory.createObject("dusttodust"));
  }

  public void testList() {
    parse(
      "<beans>" +
      "  <list name='numbers'>" +
      "    <string>one</string>" +
      "    <string>two</string>" +
      "    <string>three</string>" +
      "  </list>" +
      "</beans>"
    );
    
    List list = (List) objectFactory.createObject("numbers");
    assertEquals("one", list.get(0));
    assertEquals("two", list.get(1));
    assertEquals("three", list.get(2));
  }
  
  public void testMap() {
    parse(
      "<beans>" +
      "  <map name='numbers'>" +
      "    <entry><key><int>1</int></key><value><string>one</string></value></entry>" +
      "    <entry><key><int>2</int></key><value><string>two</string></value></entry>" +
      "    <entry><key><int>3</int></key><value><string>three</string></value></entry>" +
      "  </map>" +
      "</beans>"
    );

    Map map = (Map) objectFactory.createObject("numbers");
    assertEquals("one", map.get(new Integer(1)));
    assertEquals("two", map.get(new Integer(2)));
    assertEquals("three", map.get(new Integer(3)));
  }
  
  public void testInjection() {
    parse(
      "<beans>" +
      "  <bean name='task' class='org.jbpm.taskmgmt.exe.TaskInstance' >" +
      "    <field name='name'><string>do dishes</string></field>" +
      "    <property name='actorId'><string>theotherguy</string></property>" +
      "  </bean>" +
      "</beans>"
    );

    TaskInstance taskInstance = (TaskInstance) objectFactory.createObject("task");
    assertEquals("do dishes", taskInstance.getName());
    assertEquals("theotherguy", taskInstance.getActorId());
  }
  
  public void testContruction() {
    parse(
      "<beans>" +
      "  <bean name='task' class='org.jbpm.taskmgmt.exe.TaskInstance' >" +
      "    <constructor>" +
      "      <parameter class='java.lang.String'>" +
      "        <string>do dishes</string>" +
      "      </parameter>" +
      "      <parameter class='java.lang.String'>" +
      "        <string>theotherguy</string>" +
      "      </parameter>" +
      "    </constructor>" +
      "  </bean>" +
      "</beans>"
    );

    TaskInstance taskInstance = (TaskInstance) objectFactory.createObject("task");
    assertEquals("do dishes", taskInstance.getName());
    assertEquals("theotherguy", taskInstance.getActorId());
  }
}