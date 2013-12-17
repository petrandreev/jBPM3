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

import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jbpm.JbpmException;

public class XmlInstantiator implements Instantiator {

  private static final Class[] parameterTypes = new Class[] {Element.class};

  public Object instantiate(Class clazz, String configuration) {
    Object newInstance = null;
    try {
      // parse the bean configuration
      Element configurationElement = parseConfiguration(configuration);

      Constructor constructor = clazz.getDeclaredConstructor( parameterTypes );
      constructor.setAccessible(true);
      newInstance = constructor.newInstance( new Object[] { configurationElement } );
    } catch (Exception e) {
      log.error( "couldn't instantiate '" + clazz.getName() + "'", e );
      throw new JbpmException( e );
    }
    return newInstance;
  }
  
  protected Element parseConfiguration(String configuration) {
    Element element = null;
    try {
      element = DocumentHelper.parseText( "<action>"+configuration+"</action>" ).getRootElement();
    } catch (DocumentException e) {
      log.error( "couldn't parse bean configuration : " + configuration, e );
      throw new JbpmException(e);
    }
    return element;
  }

  private static final Log log = LogFactory.getLog(XmlInstantiator.class);
}
