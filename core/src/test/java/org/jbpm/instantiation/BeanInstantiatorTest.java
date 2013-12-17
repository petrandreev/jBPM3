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

import org.dom4j.Element;
import org.jbpm.AbstractJbpmTestCase;

public class BeanInstantiatorTest extends AbstractJbpmTestCase {

  public BeanInstantiator beanInstantiator = new BeanInstantiator();
  
  // most of the stuff is been tested in FieldInstantiatorTest
  // here, only  the method stuff needs to be tested
  public static class Mix {
    String s = null;
    Element structuredElement = null;
    RuntimeException stringConstructor = null;
    String memberWithoutSetter = null;
    
    private int callCounter = 0;
    
    private void setS(String s) {
      this.s = s;
      callCounter++;
    }
    
    protected void warningSurpresser() {
      setS(null);
    }
    
    protected void setStructuredElement(Element structuredElement) {
      this.structuredElement = structuredElement;
      callCounter++;
    }
    
    public void setStringConstructor(RuntimeException stringConstructor) {
      this.stringConstructor = stringConstructor;
      callCounter++;
    }
  }
  
  public void testBasicTypes() {
    String configuration = 
      "<s>hello</s>" +
      "<stringConstructor>" +
      "  i want yoghurt" +
      "</stringConstructor>" +
      "<structuredElement>" +
      " <surfboard length=\"270\" />" +
      " <mast length=\"475\" />" +
      " <boom length=\"160\" />" +
      " <sail size=\"5.7\" />" +
      "</structuredElement>" +
      "<memberWithoutSetter>hello</memberWithoutSetter>";
    
    Mix mix = (Mix) beanInstantiator.instantiate(Mix.class, configuration);
    
    assertEquals( "hello", mix.s);

    assertEquals("i want yoghurt", mix.stringConstructor.getMessage());

    assertEquals(4, mix.structuredElement.elements().size());
    Element firstElement = (Element) mix.structuredElement.elementIterator().next(); 
    assertEquals("surfboard", firstElement.getName());
    assertEquals("270", firstElement.attributeValue("length"));
    
    assertNull(mix.memberWithoutSetter);
  }

  public static class Shape {
    int lineSize = -1;
    public void setLineSize(int lineSize) {
      this.lineSize = lineSize;
    }
  }
  
  public static class Square extends Shape  {
  }
  
  public void testInheritedSetterInjection() {
    String configuration = 
      "<lineSize>5</lineSize>";
    Square square = (Square) beanInstantiator.instantiate(Square.class, configuration);
    assertNotNull(square);
    assertEquals(5, square.lineSize);
  }
}
