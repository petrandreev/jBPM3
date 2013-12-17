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

import org.jbpm.AbstractJbpmTestCase;


public class BeanFactoryTest extends AbstractJbpmTestCase {

  protected ObjectFactoryImpl objectFactory = null;

  protected void parse(String xml) {
    objectFactory = ObjectFactoryParser.parseXmlString(xml);
  }

  public static class MyBean {
    String text = null;
    Integer number = null;
    boolean isSetterUsed = false;
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
    parse(
      "<beans>" +
      "  <bean class='org.jbpm.configuration.BeanFactoryTest$MyBean' />" +
      "</beans>"
    );
    
    MyBean myBean = (MyBean) objectFactory.createObject(0);
    assertNull(myBean.text);
  }

  public void testBeanConstructor() {
    parse(
      "<beans>" +
      "  <bean class='org.jbpm.configuration.BeanFactoryTest$MyBean'>" +
      "    <constructor>" +
      "      <parameter class='java.lang.String'>" +
      "        <string>hello</string>" +
      "      </parameter>" +
      "      <parameter class='java.lang.Integer'>" +
      "        <integer>6</integer>" +
      "      </parameter>" +
      "    </constructor>" +
      "  </bean>" +
      "</beans>"
    );
    
    MyBean myBean = (MyBean) objectFactory.createObject(0);
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
    parse(
      "<beans>" +
      "  <bean name='mybeanfactory' class='org.jbpm.configuration.BeanFactoryTest$MyBeanFactory' />" +
      "  <bean name='mybean' >" +
      "    <constructor factory='mybeanfactory' method='createMyBean'>" +
      "      <parameter class='java.lang.String'>" +
      "        <string>hello</string>" +
      "      </parameter>" +
      "      <parameter class='java.lang.Integer'>" +
      "        <integer>6</integer>" +
      "      </parameter>" +
      "    </constructor>" +
      "  </bean>" +
      "</beans>"
    );
    
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
    parse(
      "<beans>" +
      "  <bean name='mybean' >" +
      "    <constructor factory-class='org.jbpm.configuration.BeanFactoryTest$MyBeanStaticFactory' method='createMyBean'>" +
      "      <parameter class='java.lang.String'>" +
      "        <string>hello</string>" +
      "      </parameter>" +
      "      <parameter class='java.lang.Integer'>" +
      "        <integer>6</integer>" +
      "      </parameter>" +
      "    </constructor>" +
      "  </bean>" +
      "</beans>"
    );
    
    MyBean myBean = (MyBean) objectFactory.createObject("mybean");
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertFalse(myBean.isSetterUsed);
  }

  public void testFieldInjection() {
    parse(
      "<beans>" +
      "  <bean class='org.jbpm.configuration.BeanFactoryTest$MyBean'>" +
      "    <field name='text'><string>hello</string></field>" +
      "    <field name='number'><integer>6</integer></field>" +
      "  </bean>" +
      "</beans>"
    );
    
    MyBean myBean = (MyBean) objectFactory.createObject(0);
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertFalse(myBean.isSetterUsed);
  }

  public void testPropertyInjection() {
    parse(
      "<beans>" +
      "  <bean class='org.jbpm.configuration.BeanFactoryTest$MyBean'>" +
      "    <property name='text'><string>hello</string></property>" +
      "    <property name='number'><integer>6</integer></property>" +
      "  </bean>" +
      "</beans>"
    );
    
    MyBean myBean = (MyBean) objectFactory.createObject(0);
    assertEquals("hello", myBean.text);
    assertEquals(new Integer(6), myBean.number);
    assertTrue(myBean.isSetterUsed);
  }

  public void testBeanRef() {
    parse(
      "<beans>" +
      "  <bean name='original' class='org.jbpm.configuration.BeanFactoryTest$MyBean'>" +
      "    <property name='text'><string>hello</string></property>" +
      "  </bean>" +
      "  <ref name='referencer' bean='original' />" +
      "</beans>"
    );
    
    objectFactory.clearRegistry();
    MyBean original = (MyBean) objectFactory.getObject("original");
    MyBean referencer = (MyBean) objectFactory.getObject("referencer");
    assertSame(original, referencer);

    objectFactory.clearRegistry();
    referencer = (MyBean) objectFactory.getObject("referencer");
    original = (MyBean) objectFactory.getObject("original");
    assertSame(original, referencer);
  }

  public static class DependingBean {
    DependingBean anotherBean;
  }
  public void testCircularReference() {
    parse(
      "<beans>" +
      "  <bean name='first' class='org.jbpm.configuration.BeanFactoryTest$DependingBean'>" +
      "    <field name='anotherBean'>" +
      "      <ref bean='second' />" +
      "    </field>" +
      "  </bean>" +
      "  <bean name='second' class='org.jbpm.configuration.BeanFactoryTest$DependingBean'>" +
      "    <field name='anotherBean'>" +
      "      <ref bean='third' />" +
      "    </field>" +
      "  </bean>" +
      "  <bean name='third' class='org.jbpm.configuration.BeanFactoryTest$DependingBean'>" +
      "    <field name='anotherBean'>" +
      "      <ref bean='first' />" +
      "    </field>" +
      "  </bean>" +
      "</beans>"
    );
    
    objectFactory.clearRegistry();

    try {
      objectFactory.getObject("first");
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().startsWith("circular object dependency"));
    }
    
    objectFactory.clearRegistry();

    try {
      objectFactory.getObject("second");
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().startsWith("circular object dependency"));
    }
    
    objectFactory.clearRegistry();

    try {
      objectFactory.getObject("third");
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().startsWith("circular object dependency"));
    }
  }

  
  public static class A {}
  
  public void testListWithBeanRef() {
    parse(
      "<beans>" +
      "  <list>" +
      "    <bean name='a' class='org.jbpm.configuration.BeanFactoryTest$A' />" +
      "    <ref bean='a' />" +
      "  </list>" +
      "</beans>"
    );
    
    List list = (List) objectFactory.createObject(0);
    assertNotNull(list);
    assertEquals(2, list.size());
  }

  public static class Shape {
    String color;
    int lineSize = -1;
    public void setLineSize(int lineSize) {
      this.lineSize = lineSize;
    }
  }
  
  public static class Square extends Shape  {
  }
  
  public void testInheritedFieldInjection() {
    parse(
      "<beans>" +
      "  <bean name='s' class='"+Square.class.getName()+"'>" +
      "    <field name='color'><string value='red' /></field>" +
      "  </bean>" +
      "</beans>"
    );
    
    Square square = (Square) objectFactory.createObject("s");
    assertNotNull(square);
    assertEquals("red", square.color);
  }

  public void testInheritedSetterInjection() {
    parse(
      "<beans>" +
      "  <bean name='s' class='"+Square.class.getName()+"'>" +
      "    <property name='lineSize'><int value='5' /></property>" +
      "  </bean>" +
      "</beans>"
    );
    
    Square square = (Square) objectFactory.createObject("s");
    assertNotNull(square);
    assertEquals(5, square.lineSize);
  }

}
