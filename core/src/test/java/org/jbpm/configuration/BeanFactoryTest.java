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

public class BeanFactoryTest extends AbstractJbpmTestCase {

  protected ObjectFactoryImpl objectFactory;

  protected void parse(String xml) {
    objectFactory = ObjectFactoryParser.parseXmlString(xml);
  }

  public static class MyBean {
    String text;
    Integer number;
    boolean isSetterUsed;

    public MyBean() {
    }

    public MyBean(String text, Integer number) {
      this.text = text;
      this.number = number;
    }

    public void setNumber(Integer number) {
      this.number = number;
      isSetterUsed = true;
    }

    public void setText(String text) {
      this.text = text;
      isSetterUsed = true;
    }
  }

  public void testBeanDefaultConstructor() {
    parse("<beans>"
      + "  <bean name='mybean' class='" + MyBean.class.getName() + "' />"
      + "</beans>");

    MyBean myBean = (MyBean) objectFactory.createObject("mybean");
    assertNull(myBean.text);
  }

  public void testBeanConstructor() {
    parse("<beans>"
      + "  <bean name='mybean' class='" + MyBean.class.getName() + "'>"
      + "    <constructor>"
      + "      <parameter class='java.lang.String'>"
      + "        <string>hello</string>"
      + "      </parameter>"
      + "      <parameter class='java.lang.Integer'>"
      + "        <integer>6</integer>"
      + "      </parameter>"
      + "    </constructor>"
      + "  </bean>"
      + "</beans>");

    MyBean myBean = (MyBean) objectFactory.createObject("mybean");
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertFalse(myBean.isSetterUsed);
  }

  public static class MyBeanFactory {
    public MyBean createMyBean(String text, Integer number) {
      return new MyBean(text, number);
    }
  }

  public void testBeanFactory() {
    parse("<beans>"
      + "  <bean name='mybeanfactory' class='" + MyBeanFactory.class.getName() + "' />"
      + "  <bean name='mybean' >"
      + "    <constructor factory='mybeanfactory' method='createMyBean'>"
      + "      <parameter class='java.lang.String'>"
      + "        <string>hello</string>"
      + "      </parameter>"
      + "      <parameter class='java.lang.Integer'>"
      + "        <integer>6</integer>"
      + "      </parameter>"
      + "    </constructor>"
      + "  </bean>"
      + "</beans>");

    MyBean myBean = (MyBean) objectFactory.createObject("mybean");
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertFalse(myBean.isSetterUsed);
  }

  public static class MyBeanStaticFactory {
    public static MyBean createMyBean(String text, Integer number) {
      return new MyBean(text, number);
    }
  }

  public void testBeanFactoryStaticMethod() {
    parse("<beans>"
      + "  <bean name='mybean' >"
      + "    <constructor factory-class='" + MyBeanStaticFactory.class.getName()
      + "'     method='createMyBean'>"
      + "      <parameter class='java.lang.String'>"
      + "        <string>hello</string>"
      + "      </parameter>"
      + "      <parameter class='java.lang.Integer'>"
      + "        <integer>6</integer>"
      + "      </parameter>"
      + "    </constructor>"
      + "  </bean>"
      + "</beans>");

    MyBean myBean = (MyBean) objectFactory.createObject("mybean");
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertFalse(myBean.isSetterUsed);
  }

  public void testFieldInjection() {
    parse("<beans>"
      + "  <bean name='mybean' class='" + MyBean.class.getName() + "'>"
      + "    <field name='text'><string>hello</string></field>"
      + "    <field name='number'><integer>6</integer></field>"
      + "  </bean>"
      + "</beans>");

    MyBean myBean = (MyBean) objectFactory.createObject("mybean");
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertFalse(myBean.isSetterUsed);
  }

  public void testPropertyInjection() {
    parse("<beans>"
      + "  <bean name='mybean' class='" + MyBean.class.getName() + "'>"
      + "    <property name='text'><string>hello</string></property>"
      + "    <property name='number'><integer>6</integer></property>"
      + "  </bean>"
      + "</beans>");

    MyBean myBean = (MyBean) objectFactory.createObject("mybean");
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertTrue(myBean.isSetterUsed);
  }

  public void testBeanRef() {
    parse("<beans>"
      + "  <bean name='original' class='" + MyBean.class.getName() + "' singleton='true'>"
      + "    <property name='text'><string>hello</string></property>"
      + "  </bean>"
      + "  <ref name='referencer' bean='original' />"
      + "</beans>");

    MyBean original = (MyBean) objectFactory.createObject("original");
    MyBean referencer = (MyBean) objectFactory.createObject("referencer");
    assertSame(original, referencer);

    referencer = (MyBean) objectFactory.createObject("referencer");
    original = (MyBean) objectFactory.createObject("original");
    assertSame(original, referencer);
  }

  public static class DependingBean {
    DependingBean anotherBean;
  }

  public void testCircularReference() {
    parse("<beans>"
      + "  <bean name='first' class='" + DependingBean.class.getName() + "'>"
      + "    <field name='anotherBean'>"
      + "      <ref bean='second' />"
      + "    </field>"
      + "  </bean>"
      + "  <bean name='second' class='" + DependingBean.class.getName() + "'>"
      + "    <field name='anotherBean'>"
      + "      <ref bean='third' />"
      + "    </field>"
      + "  </bean>"
      + "  <bean name='third' class='" + DependingBean.class.getName() + "'>"
      + "    <field name='anotherBean'>"
      + "      <ref bean='first' />"
      + "    </field>"
      + "  </bean>"
      + "</beans>");

    try {
      objectFactory.createObject("first");
      fail("expected exception");
    }
    catch (ConfigurationException e) {
      assertTrue(e.getMessage().indexOf("first") != -1);
    }

    try {
      objectFactory.createObject("second");
      fail("expected exception");
    }
    catch (ConfigurationException e) {
      assertTrue(e.getMessage().indexOf("second") != -1);
    }

    try {
      objectFactory.createObject("third");
      fail("expected exception");
    }
    catch (ConfigurationException e) {
      assertTrue(e.getMessage().indexOf("third") != -1);
    }
  }

  public static class A {
  }

  public void testListWithBeanRef() {
    parse("<beans>"
      + "  <list name='mylist'>"
      + "    <bean name='a' class='" + A.class.getName() + "' />"
      + "    <ref bean='a' />"
      + "  </list>"
      + "</beans>");

    List list = (List) objectFactory.createObject("mylist");
    assertEquals(2, list.size());
    assertSame(list.get(0), list.get(1));
  }

  public void testMapWithBeanRef() {
    parse("<beans>"
      + "  <map name='mymap'>"
      +	"    <entry>"
      +	"      <key><string value='1st'/></key>"
      + "      <value><bean name='a' class='" + A.class.getName() + "'/></value>"
      + "    </entry>"
      + "    <entry>"
      + "      <key><string value='2nd'/></key>"
      + "      <value><ref bean='a'/></value>"
      + "    </entry>"
      + "  </map>"
      + "</beans>");

    Map map = (Map) objectFactory.createObject("mymap");
    assertEquals(2, map.size());
    assertSame(map.get("1st"), map.get("2nd"));
  }

  public static class Shape {
    String color;
    int lineSize = -1;

    public void setLineSize(int lineSize) {
      this.lineSize = lineSize;
    }
  }

  public static class Square extends Shape {
  }

  public void testInheritedFieldInjection() {
    parse("<beans>"
      + "  <bean name='s' class='" + Square.class.getName() + "'>"
      + "    <field name='color'><string value='red' /></field>"
      + "  </bean>"
      + "</beans>");

    Square square = (Square) objectFactory.createObject("s");
    assertNotNull(square);
    assertEquals("red", square.color);
  }

  public void testInheritedSetterInjection() {
    parse("<beans>"
      + "  <bean name='s' class='" + Square.class.getName() + "'>"
      + "    <property name='lineSize'><int value='5' /></property>"
      + "  </bean>"
      + "</beans>");

    Square square = (Square) objectFactory.createObject("s");
    assertNotNull(square);
    assertEquals(5, square.lineSize);
  }

}
