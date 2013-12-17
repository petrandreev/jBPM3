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
package org.jbpm.instantiation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.dom4j.Element;
import org.jbpm.AbstractJbpmTestCase;

public class FieldInstantiatorTest extends AbstractJbpmTestCase {

  public FieldInstantiator fieldInstantiator = new FieldInstantiator();
  
  public static class ClassWithLotsOfFields {
    String s = null;
    Integer i = null;
    int ii = -1;
    Long l = null;
    long ll = -1;
    Float f = null;
    float ff = -1;
    Double d = null;
    double dd = -1;
    Boolean b = null;
    boolean bb = false;
    Character c = null;
    char cc = ' ';
    Short sh = null;
    short shsh = -1;
    Byte by = null;
    byte byby = -1;
  }
  
  public void testBasicTypes() {
    String configuration = 
      "<s>hello</s>" +
      "<i>1</i>" +
      "<ii>2</ii>" +
      "<l>3</l>" +
      "<ll>4</ll>" +
      "<f>5.5</f>" +
      "<ff>6.6</ff>" +
      "<d>7.7</d>" +
      "<dd>8.8</dd>" +
      "<b>TRUE</b>" +
      "<bb>true</bb>" +
      "<c>a</c>" +
      "<cc>b</cc>" +
      "<sh>9</sh>" +
      "<shsh>10</shsh>" +
      "<by>11</by>" +
      "<byby>12</byby>";
    
    ClassWithLotsOfFields c = (ClassWithLotsOfFields) fieldInstantiator.instantiate(ClassWithLotsOfFields.class, configuration);
    
    assertEquals( "hello", c.s );
    assertEquals( new Integer(1), c.i );
    assertEquals( 2, c.ii );
    assertEquals( new Long(3), c.l );
    assertEquals( 4, c.ll );
    assertEquals( new Float(5.5), c.f );
    assertEquals( (float)6.6, c.ff, 0 );
    assertEquals( new Double(7.7), c.d );
    assertEquals( 8.8, c.dd, 0 );
    assertEquals( Boolean.TRUE, c.b );
    assertEquals( true, c.bb );
    assertEquals( new Character('a'), c.c );
    assertEquals( 'b', c.cc );
    assertEquals( new Short((short) 9), c.sh );
    assertEquals( 10, c.shsh );
    assertEquals( new Byte((byte) 11), c.by );
    assertEquals( 12, c.byby );
  }

  public static class ClassWithStringConstructorType {
    RuntimeException e;
  }

  public void testStringConstructorType() {
    String configuration = "<e>i want yoghurt</e>";
    ClassWithStringConstructorType c = (ClassWithStringConstructorType) fieldInstantiator.instantiate(ClassWithStringConstructorType.class, configuration);
    assertEquals("i want yoghurt", c.e.getMessage());
  }

  public static class ClassWithDom4jField{
    Element structuredElement;
  }

  public void testStructuredElement() {
    String configuration = 
      "<structuredElement>" +
      " <surfboard length=\"270\" />" +
      " <mast length=\"475\" />" +
      " <boom length=\"160\" />" +
      " <sail size=\"5.7\" />" +
      "</structuredElement>";
    ClassWithDom4jField c = (ClassWithDom4jField) fieldInstantiator.instantiate(ClassWithDom4jField.class, configuration);
    assertEquals(4, c.structuredElement.elements().size());
    Element firstElement = (Element) c.structuredElement.elementIterator().next(); 
    assertEquals("surfboard", firstElement.getName());
    assertEquals("270", firstElement.attributeValue("length"));
  }

  public static class ClassWithOneField {
    String onlyMember = null;
  }

  public void testEmptyConfiguration() {
    ClassWithOneField c = (ClassWithOneField) fieldInstantiator.instantiate(ClassWithOneField.class, null);
    assertNull(c.onlyMember);
    c = (ClassWithOneField) fieldInstantiator.instantiate(ClassWithOneField.class, "");
    assertNull(c.onlyMember);
  }

  public void testNonMatchingConfiguration() {
    String configuration = "<unexistingMember>bullshit</unexistingMember>";
    ClassWithOneField c = (ClassWithOneField) fieldInstantiator.instantiate(ClassWithOneField.class, configuration);
    assertNull(c.onlyMember);
  }

  
  public static class Shape {
    String color;
  }
  
  public static class Square extends Shape  {
  }
  
  public void testInheritedFieldInjection() {
    String configuration = 
      "<color>red</color>";
    Square square = (Square) fieldInstantiator.instantiate(Square.class, configuration);
    assertNotNull(square);
    assertEquals("red", square.color);
  }

  public static class ListAction {
    List numbers = null;
    LinkedList linkedNumbers = null;
  }

  public void testListInjection() {
    String configuration = 
      "<numbers>" +
      "  <element>one</element>" +
      "  <element>two</element>" +
      "  <element>three</element>" +
      "</numbers>";
    
    ListAction listAction = (ListAction) fieldInstantiator.instantiate(ListAction.class, configuration);
    
    List expectedNumbers = new ArrayList();
    expectedNumbers.add("one");
    expectedNumbers.add("two");
    expectedNumbers.add("three");
    
    assertEquals(expectedNumbers, listAction.numbers);
  }

  public void testLinkedListInjection() {
    String configuration = 
      "<linkedNumbers>" +
      "  <element>one</element>" +
      "  <element>two</element>" +
      "  <element>three</element>" +
      "</linkedNumbers>";
    
    ListAction listAction = (ListAction) fieldInstantiator.instantiate(ListAction.class, configuration);
    
    List expectedNumbers = new ArrayList();
    expectedNumbers.add("one");
    expectedNumbers.add("two");
    expectedNumbers.add("three");
    
    assertEquals(expectedNumbers, listAction.linkedNumbers);
  }

  public void testListTypedInjection() {
    String configuration = 
      "<numbers element-type='java.lang.Integer'> " +
      "  <element>1</element>" +
      "  <element>2</element>" +
      "  <element>3</element>" +
      "</numbers>";
    
    ListAction listAction = (ListAction) fieldInstantiator.instantiate(ListAction.class, configuration);
    
    List expectedNumbers = new ArrayList();
    expectedNumbers.add(new Integer(1));
    expectedNumbers.add(new Integer(2));
    expectedNumbers.add(new Integer(3));
    
    assertEquals(expectedNumbers, listAction.numbers);
  }

  public static class MapAction {
    Map numbers = null;
    Hashtable numbersTable = null;
    SortedMap sortedNumbers = null;
  }

  public void testMapInjection() {
    String configuration = 
      "<numbers>" +
      "  <entry><key>one</key><value>1</value></entry>" +
      "  <entry><key>two</key><value>2</value></entry>" +
      "  <entry><key>three</key><value>3</value></entry>" +
      "</numbers>";
    
    MapAction mapAction = (MapAction) fieldInstantiator.instantiate(MapAction.class, configuration);
    
    Map expectedNumbers = new HashMap();
    expectedNumbers.put("one", "1");
    expectedNumbers.put("two", "2");
    expectedNumbers.put("three", "3");
    
    assertEquals(expectedNumbers, mapAction.numbers);
  }

  public void testHashtableInjection() {
    String configuration = 
      "<numbersTable>" +
      "  <entry><key>one</key><value>1</value></entry>" +
      "  <entry><key>two</key><value>2</value></entry>" +
      "  <entry><key>three</key><value>3</value></entry>" +
      "</numbersTable>";
    
    MapAction mapAction = (MapAction) fieldInstantiator.instantiate(MapAction.class, configuration);
    
    Map expectedNumbers = new HashMap();
    expectedNumbers.put("one", "1");
    expectedNumbers.put("two", "2");
    expectedNumbers.put("three", "3");
    
    assertEquals(expectedNumbers, mapAction.numbersTable);
  }

  public void testSortedMapInjection() {
    String configuration = 
      "<sortedNumbers>" +
      "  <entry><key>one</key><value>1</value></entry>" +
      "  <entry><key>two</key><value>2</value></entry>" +
      "  <entry><key>three</key><value>3</value></entry>" +
      "</sortedNumbers>";
    
    MapAction mapAction = (MapAction) fieldInstantiator.instantiate(MapAction.class, configuration);
    
    Iterator i = mapAction.sortedNumbers.entrySet().iterator();
    Map.Entry entry = (Map.Entry)i.next();
    assertEquals("one", entry.getKey());
    assertEquals("1", entry.getValue());
    entry = (Map.Entry)i.next();
    assertEquals("three", entry.getKey());
    assertEquals("3", entry.getValue());
    entry = (Map.Entry)i.next();
    assertEquals("two", entry.getKey());
    assertEquals("2", entry.getValue());
  }

  public void testMapTypedInjection() {
    String configuration = 
      "<numbers key-type='java.lang.Integer' value-type='java.lang.Long'>" +
      "  <entry><key>1</key><value>1</value></entry>" +
      "  <entry><key>2</key><value>2</value></entry>" +
      "  <entry><key>3</key><value>3</value></entry>" +
      "</numbers>";
    
    MapAction mapAction = (MapAction) fieldInstantiator.instantiate(MapAction.class, configuration);
    
    Map expectedNumbers = new HashMap();
    expectedNumbers.put(new Integer(1), new Long(1));
    expectedNumbers.put(new Integer(2), new Long(2));
    expectedNumbers.put(new Integer(3), new Long(3));
    
    assertEquals(expectedNumbers, mapAction.numbers);
  }

  public static class SetAction {
    Set numbers;
    LinkedHashSet linkedNumbers;
    SortedSet sortedNumbers;
  }

  public void testSetInjection() {
    String configuration = 
      "<numbers>" +
      "  <element>one</element>" +
      "  <element>two</element>" +
      "  <element>three</element>" +
      "</numbers>";
    
    SetAction setAction = (SetAction) fieldInstantiator.instantiate(SetAction.class, configuration);
    
    Set expectedNumbers = new HashSet();
    expectedNumbers.add("one");
    expectedNumbers.add("two");
    expectedNumbers.add("three");
    
    assertEquals(expectedNumbers, setAction.numbers);
  }

  public void testLinkedHashSetInjection() {
    String configuration = 
      "<linkedNumbers>" +
      "  <element>one</element>" +
      "  <element>two</element>" +
      "  <element>three</element>" +
      "</linkedNumbers>";
    
    SetAction setAction = (SetAction) fieldInstantiator.instantiate(SetAction.class, configuration);
    
    Set expectedNumbers = new HashSet();
    expectedNumbers.add("one");
    expectedNumbers.add("two");
    expectedNumbers.add("three");
    
    assertEquals(expectedNumbers, setAction.linkedNumbers);
  }

  public void testSortedSetInjection() {
    String configuration = 
      "<sortedNumbers>" +
      "  <element>one</element>" +
      "  <element>two</element>" +
      "  <element>three</element>" +
      "</sortedNumbers>";
    
    SetAction setAction = (SetAction) fieldInstantiator.instantiate(SetAction.class, configuration);
    
    Iterator i = setAction.sortedNumbers.iterator();
    String element = (String) i.next();
    assertEquals("one", element);
    element = (String) i.next();
    assertEquals("three", element);
    element = (String) i.next();
    assertEquals("two", element);
  }

  public void testSetTypedInjection() {
    String configuration = 
      "<numbers element-type='java.lang.Integer'> " +
      "  <element>1</element>" +
      "  <element>2</element>" +
      "  <element>3</element>" +
      "</numbers>";
    
    SetAction setAction = (SetAction) fieldInstantiator.instantiate(SetAction.class, configuration);
    
    Set expectedNumbers = new HashSet();
    expectedNumbers.add(new Integer(1));
    expectedNumbers.add(new Integer(2));
    expectedNumbers.add(new Integer(3));
    
    assertEquals(expectedNumbers, setAction.numbers);
  }

  public static class CollectionAction {
    Collection numbers;
  }

  public void testCollectionInjection() {
    String configuration = 
      "<numbers>" +
      "  <element>one</element>" +
      "  <element>two</element>" +
      "  <element>three</element>" +
      "</numbers>";
    
    CollectionAction collectionAction = (CollectionAction) fieldInstantiator.instantiate(CollectionAction.class, configuration);
    
    Collection expectedNumbers = new ArrayList();
    expectedNumbers.add("one");
    expectedNumbers.add("two");
    expectedNumbers.add("three");
    
    assertEquals(expectedNumbers, collectionAction.numbers);
  }

  public void testCollectionTypedInjection() {
    String configuration = 
      "<numbers element-type='java.lang.Integer'> " +
      "  <element>1</element>" +
      "  <element>2</element>" +
      "  <element>3</element>" +
      "</numbers>";
    
    CollectionAction collectionAction = (CollectionAction) fieldInstantiator.instantiate(CollectionAction.class, configuration);
    
    Collection expectedNumbers = new ArrayList();
    expectedNumbers.add(new Integer(1));
    expectedNumbers.add(new Integer(2));
    expectedNumbers.add(new Integer(3));
    
    assertEquals(expectedNumbers, collectionAction.numbers);
  }
}
