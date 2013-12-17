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
package org.jbpm.context.exe;

import org.hibernate.cfg.Configuration;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JbpmSchema;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

public class StringIdVariableDbTest extends AbstractDbTestCase {

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      // disable logging service to prevent logs from referencing custom object
      jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/context/exe/jbpm.cfg.xml");

      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        DbPersistenceServiceFactory persistenceServiceFactory =
          (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
        Configuration configuration = persistenceServiceFactory.getConfiguration();
        configuration.addClass(CustomStringClass.class);

        JbpmSchema jbpmSchema = new JbpmSchema(configuration);
        jbpmSchema.createTable("JBPM_TEST_CUSTOMSTRINGID");
      }
      finally {
        jbpmContext.close();
      }
    }
    return jbpmConfiguration;
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testCustomVariableClassWithStringId() {
    // create and save process definition
    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    processDefinition.addDefinition(new ContextDefinition());
    deployProcessDefinition(processDefinition);

    // create process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    // create custom object
    CustomStringClass customStringObject = new CustomStringClass("my stuff");
    processInstance.getContextInstance().setVariable("custom", customStringObject);

    // save process instance
    processInstance = saveAndReload(processInstance);
    // get custom object from variables
    customStringObject = (CustomStringClass) processInstance.getContextInstance()
      .getVariable("custom");
    assertNotNull(customStringObject);
    assertEquals("my stuff", customStringObject.getName());

    // delete custom object
    processInstance.getContextInstance().deleteVariable("custom");
    session.delete(customStringObject);
  }
}
