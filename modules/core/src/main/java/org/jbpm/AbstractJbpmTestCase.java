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
package org.jbpm;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractJbpmTestCase extends TestCase
{
  private static Log log = LogFactory.getLog(AbstractJbpmTestCase.class);
  
  protected void setUp() throws Exception
  {
    log.debug("### START " + getLongName() + " ####################################################");
  }

  protected void tearDown() throws Exception
  {
    log.debug("### END " + getLongName() + " ######################################################");
  }
  
  protected String getShortName()
  {
    String shortName = getClass().getName();
    return shortName.substring(shortName.lastIndexOf('.') + 1) + "." + getName();
  }
  
  protected String getLongName()
  {
    return getClass().getName() + "." + getName();
  }
  
  protected void runTest() throws Throwable {
    try {
      super.runTest();
    } catch (AssertionFailedError e) {
      log.error("");
      log.error("ASSERTION FAILURE: "+e.getMessage());
      log.error("");
      throw e;
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
  }
}
