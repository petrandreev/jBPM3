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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.AbstractJbpmTestCase;

public class ObjectFactoryTest extends AbstractJbpmTestCase {
  
  protected ObjectFactoryImpl objectFactory = null;

  protected void parse(String xml) {
    objectFactory = ObjectFactoryParser.parseXmlString(xml);
  }

  public void testNamedBean() {
    parse(
      "<beans>" +
      "  <bean name='mybean' class='java.util.HashMap' />" +
      "</beans>"
    );
    
    HashMap m = (HashMap) objectFactory.createObject("mybean");
    assertNotNull(m);
  }

  public void testNonSingletonBean() {
    parse(
      "<beans>" +
      "  <bean name='mybean' class='java.util.HashMap' />" +
      "</beans>"
    );
    
    HashMap m1 = (HashMap) objectFactory.getObject("mybean");
    HashMap m2 = (HashMap) objectFactory.getObject("mybean");
    assertSame(m1, m2);
  }

  public void testNonSingletonNewBean() {
    parse(
      "<beans>" +
      "  <bean name='mybean' class='java.util.HashMap' />" +
      "</beans>"
    );
    
    HashMap m1 = (HashMap) objectFactory.createObject("mybean");
    HashMap m2 = (HashMap) objectFactory.createObject("mybean");
    assertNotSame(m1, m2);
  }

  public void testSingletonBean() {
    parse(
      "<beans>" +
      "  <bean singleton='true' name='mybean' class='java.util.HashMap' />" +
      "</beans>"
    );
    
    HashMap m1 = (HashMap) objectFactory.createObject("mybean");
    HashMap m2 = (HashMap) objectFactory.createObject("mybean");
    assertSame(m1, m2);
  }

  public void testNamedMap() {
    parse(
      "<beans>" +
      "  <map name='mymap' />" +
      "</beans>"
    );
    
    Map m = (Map) objectFactory.createObject("mymap");
    assertNotNull(m);
  }

  public void testNonSingletonMap() {
    parse(
      "<beans>" +
      "  <map name='mymap' />" +
      "</beans>"
    );
    
    Map m1 = (Map) objectFactory.getObject("mymap");
    Map m2 = (Map) objectFactory.getObject("mymap");
    assertSame(m1, m2);
  }

  public void testNonSingletonNewMap() {
    parse(
      "<beans>" +
      "  <map name='mymap' />" +
      "</beans>"
    );
    
    Map m1 = (Map) objectFactory.createObject("mymap");
    Map m2 = (Map) objectFactory.createObject("mymap");
    assertNotSame(m1, m2);
  }

  public void testSingletonMap() {
    parse(
      "<beans>" +
      "  <map singleton='true' name='mymap' />" +
      "</beans>"
    );
    
    Map m1 = (Map) objectFactory.createObject("mymap");
    Map m2 = (Map) objectFactory.createObject("mymap");
    assertSame(m1, m2);
  }

  public void testNamedList() {
    parse(
      "<beans>" +
      "  <list name='mymap' />" +
      "</beans>"
    );
    
    List l = (List) objectFactory.createObject("mymap");
    assertNotNull(l);
  }

  public void testNonSingletonList() {
    parse(
      "<beans>" +
      "  <list name='mylist' />" +
      "</beans>"
    );
    
    List l1 = (List) objectFactory.getObject("mylist");
    List l2 = (List) objectFactory.getObject("mylist");
    assertSame(l1, l2);
  }

  public void testNonSingletonNewList() {
    parse(
      "<beans>" +
      "  <list name='mylist' />" +
      "</beans>"
    );
    
    List l1 = (List) objectFactory.createObject("mylist");
    List l2 = (List) objectFactory.createObject("mylist");
    assertNotSame(l1, l2);
  }

  public void testSingletonList() {
    parse(
      "<beans>" +
      "  <list singleton='true' name='mylist' />" +
      "</beans>"
    );
    
    List l1 = (List) objectFactory.createObject("mylist");
    List l2 = (List) objectFactory.createObject("mylist");
    assertSame(l1, l2);
  }

  public void testNamedNull() {
    parse(
      "<beans>" +
      "  <null name='mynull'/>" +
      "</beans>"
    );
    
    assertNull(objectFactory.createObject("mynull"));
  }

  public void testNamedString() {
    parse(
      "<beans>" +
      "  <string name='level'>four</string>" +
      "</beans>"
    );
    
    assertEquals("four", objectFactory.createObject("level"));
  }

  public void testNamedInt() {
    parse(
      "<beans>" +
      "  <int name='level'>5</int>" +
      "</beans>"
    );
    
    assertEquals(new Integer(5), objectFactory.createObject("level"));
  }

  public void testNamedInteger() {
    parse(
      "<beans>" +
      "  <integer name='level'>6</integer>" +
      "</beans>"
    );
    
    assertEquals(new Integer(6), objectFactory.createObject("level"));
  }

  public void testNamedLong() {
    parse(
      "<beans>" +
      "  <long name='level'>7</long>" +
      "</beans>"
    );
    
    assertEquals(new Long(7), objectFactory.createObject("level"));
  }

  public void testNamedFloat() {
    parse(
      "<beans>" +
      "  <float name='level'>7.7</float>" +
      "</beans>"
    );
    
    assertEquals(new Float(7.7), objectFactory.createObject("level"));
  }

  public void testNamedDouble() {
    parse(
      "<beans>" +
      "  <double name='level'>8.8</double>" +
      "</beans>"
    );
    
    assertEquals(new Double(8.8), objectFactory.createObject("level"));
  }

  public void testNamedChar() {
    parse(
      "<beans>" +
      "  <char name='level'>a</char>" +
      "</beans>"
    );
    
    assertEquals(new Character('a'), objectFactory.createObject("level"));
  }
  
  public void testNamedBoolean() {
    parse(
      "<beans>" +
      "  <boolean name='affirmative'>true</boolean>" +
      "  <boolean name='negative'>false</boolean>" +
      "</beans>"
    );
    
    assertEquals(Boolean.TRUE, objectFactory.createObject("affirmative"));
    assertEquals(Boolean.FALSE, objectFactory.createObject("negative"));
  }

  public void testUndefinedName() {
    parse(
      "<beans>" +
      "  <string name='first'>1</string>" +
      "  <string name='second'>2</string>" +
      "</beans>"
    );
    
    try {
      objectFactory.createObject("unexisting-object-name");
      fail("expected exception");
    } catch (ConfigurationException e) {
      // OK
    }
  }
}
