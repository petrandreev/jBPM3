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
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JbpmSchema;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

public class CustomVariableStringIdDbTest extends AbstractDbTestCase {

  private static final String CUSTOM_TABLE = "JBPM_TEST_CUSTOMSTRINGID";

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseResource(getJbpmTestConfig());
      DbPersistenceServiceFactory factory = (DbPersistenceServiceFactory) jbpmConfiguration.getServiceFactory(Services.SERVICENAME_PERSISTENCE);

      Configuration configuration = factory.getConfiguration();
      configuration.addClass(CustomStringClass.class);

      JbpmSchema jbpmSchema = new JbpmSchema(configuration);
      if (!jbpmSchema.tableExists(CUSTOM_TABLE)) {
        jbpmSchema.createTable(CUSTOM_TABLE);
      }
    }
    return jbpmConfiguration;
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testCustomVariableClassWithStringId() {
    // create and save the process definition
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addDefinition(new ContextDefinition());
    graphSession.saveProcessDefinition(processDefinition);
    CustomStringClass customStringObject = null;
    try {
      // create the process instance
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      ContextInstance contextInstance = processInstance.getContextInstance();
      // create the custom object
      customStringObject = new CustomStringClass("customname");
      contextInstance.setVariable("custom hibernate object", customStringObject);

      processInstance = saveAndReload(processInstance);
      contextInstance = processInstance.getContextInstance();

      // get the custom hibernatable object from the variables
      customStringObject = (CustomStringClass) contextInstance.getVariable("custom hibernate object");
      assertNotNull(customStringObject);
      assertEquals("customname", customStringObject.getName());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
      if (customStringObject != null)
        session.delete(customStringObject);
    }
  }
}
