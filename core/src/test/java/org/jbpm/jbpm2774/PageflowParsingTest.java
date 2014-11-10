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
package org.jbpm.jbpm2774;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * Pageflow parsing requires Internet connection.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-2774">JBPM-2774</a>
 * @author Alejandro Guizar
 */
public class PageflowParsingTest extends AbstractJbpmTestCase {

  public void testPageflow2_0() {
    ProcessDefinition.parseXmlResource("org/jbpm/jbpm2774/pageflow-2.0.xml");
  }

  public void testPageflow2_1() {
    ProcessDefinition.parseXmlResource("org/jbpm/jbpm2774/pageflow-2.1.xml");
  }

  public void testPageflow2_2() {
    ProcessDefinition.parseXmlResource("org/jbpm/jbpm2774/pageflow-2.2.xml");
  }
}
