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
package org.jbpm.jbpm2630;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmConfigurationTestHelper;
import org.jbpm.JbpmContext;

/**
 * {@link JbpmConfiguration#popJbpmConfiguration} does not implement a pop
 * operation.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2630">JBPM-2630</a>
 * @author Alejandro Guizar
 */
public class JBPM2630Test extends AbstractJbpmTestCase {

  JbpmConfiguration baseConfiguration;
  JbpmConfiguration alternateConfiguration;

  protected void setUp() throws Exception {
    super.setUp();
    baseConfiguration = JbpmConfiguration.parseResource("jbpm.cfg.xml");
    alternateConfiguration = JbpmConfiguration.parseResource("org/jbpm/jbpm2630/jbpm.nolog.cfg.xml");
  }

  protected void tearDown() throws Exception {
    alternateConfiguration.close();
    baseConfiguration.close();
    super.tearDown();
  }

  public void testBlockStructuredContextClose() {
    JbpmContext baseContext1 = baseConfiguration.createJbpmContext();
    try {
      JbpmContext alternateContext = alternateConfiguration.createJbpmContext();
      try {
        JbpmContext baseContext2 = baseConfiguration.createJbpmContext();
        try {
          assertSame(baseConfiguration,
            JbpmConfigurationTestHelper.getCurrentJbpmConfiguration());
        }
        finally {
          baseContext2.close();
        }
        assertSame(alternateConfiguration,
          JbpmConfigurationTestHelper.getCurrentJbpmConfiguration());
      }
      finally {
        alternateContext.close();
      }
      assertSame(baseConfiguration, JbpmConfigurationTestHelper.getCurrentJbpmConfiguration());
    }
    finally {
      baseContext1.close();
    }
  }

  public void testUnstructuredContextClose() {
    JbpmContext baseContext = baseConfiguration.createJbpmContext();
    assertSame(baseConfiguration,
      JbpmConfigurationTestHelper.getCurrentJbpmConfiguration());

    JbpmContext alternateContext = alternateConfiguration.createJbpmContext();
    assertSame(alternateConfiguration,
      JbpmConfigurationTestHelper.getCurrentJbpmConfiguration());

    // this will emit a block-structured warning, but still succeed
    baseContext.close();
    alternateContext.close();

    assertNull(JbpmConfigurationTestHelper.getCurrentJbpmConfiguration());
  }
}
