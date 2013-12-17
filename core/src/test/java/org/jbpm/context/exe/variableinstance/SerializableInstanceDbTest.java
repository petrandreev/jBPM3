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
package org.jbpm.context.exe.variableinstance;

import java.util.List;

import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.context.exe.MySerializableClass;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.db.DbPersistenceService;

public class SerializableInstanceDbTest extends AbstractDbTestCase {

  ProcessInstance processInstance;
  ContextInstance contextInstance;

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    processDefinition.addDefinition(new ContextDefinition());
    deployProcessDefinition(processDefinition);

    processInstance = new ProcessInstance(processDefinition);
    contextInstance = processInstance.getContextInstance();

  }

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      // disable logging service to prevent logs from referencing custom object
      jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/context/exe/jbpm.cfg.xml");
    }
    return jbpmConfiguration;
  } 
  
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testSerializableVariableKeepsId() {
    contextInstance.setVariable("a", new MySerializableClass(4));
    processInstance = saveAndReload(processInstance);

    int numByteArrayRows = getLatestDbId("ID_", "JBPM_BYTEARRAY");
    int numByteBlockRows = getLatestDbId("PROCESSFILE_", "JBPM_BYTEBLOCK");

    // check validation ids
    assertTrue(numByteArrayRows > 0);
    assertTrue(numByteBlockRows > 0);

    contextInstance = processInstance.getContextInstance();
    assertEquals(new MySerializableClass(4), contextInstance.getVariable("a"));

    // update serializeable variable with new value
    contextInstance.setVariable("a", new MySerializableClass(555));
    processInstance = saveAndReload(processInstance);

    int newByteArrayRows = getLatestDbId("ID_", "JBPM_BYTEARRAY");
    int newByteBlockRows = getLatestDbId("PROCESSFILE_", "JBPM_BYTEBLOCK");

    // check db ids again
    assertTrue(newByteArrayRows > 0);
    assertTrue(newByteBlockRows > 0);

    contextInstance = processInstance.getContextInstance();
    assertEquals(new MySerializableClass(555), contextInstance.getVariable("a"));

    // check if db ids are the same
    // 1. One ByteArray object for var
    // 2. One newValue for 1rst ByteArrayUpdateLog
    // 3. One newValue 
    // 4. and one oldValue for 2nd ByteArrayUpdateLog
    // TOTAL = 4
    // If a _NEW_ ByteArray object is made, then there would be 5 ByteArray objects and byte []'s 
    assertTrue("Too many ByteArray objects have been made [" + numByteArrayRows + "!=" + newByteArrayRows + "]", numByteArrayRows == newByteArrayRows );
    assertTrue("Too many byte [] objects have been persisted [" + numByteBlockRows + "!=" + newByteBlockRows + "]", numByteBlockRows == newByteBlockRows );
    
    // Check that null's don't cause a problem.
    contextInstance.setVariable("a", null);
    processInstance = saveAndReload(processInstance);
    assertEquals("Null variable not retrieved as null.", null, contextInstance.getVariable("a"));
    
    newByteArrayRows = getLatestDbId("ID_", "JBPM_BYTEARRAY");
    newByteBlockRows = getLatestDbId("PROCESSFILE_", "JBPM_BYTEBLOCK");

    // 4. One oldValue for 3rd ByteArrayUpdateLog (newValue is null)
    assertTrue("Too many ByteArray objects have been made [" + numByteArrayRows + "!=" + newByteArrayRows + "]", numByteArrayRows == newByteArrayRows );
    assertTrue("Too many byte [] objects have been persisted [" + numByteBlockRows + "! >=" + newByteBlockRows + "]", numByteBlockRows >= newByteBlockRows );
   
    // Refill var
    contextInstance.setVariable("a", new MySerializableClass(123));
    processInstance = saveAndReload(processInstance);
    
    newByteArrayRows = getLatestDbId("ID_", "JBPM_BYTEARRAY");
    newByteBlockRows = getLatestDbId("PROCESSFILE_", "JBPM_BYTEBLOCK");

    // Same as before..
    assertTrue("Too many ByteArray objects have been made [" + numByteArrayRows + "!=" + newByteArrayRows + "]", numByteArrayRows == newByteArrayRows );
    assertTrue("Too many byte [] objects have been persisted [" + numByteBlockRows + "!=" + newByteBlockRows + "]", numByteBlockRows == newByteBlockRows );
  }

  protected int getLatestDbId(String idName, String tableName) {
    int id = 0;
    DbPersistenceService persistenceService = (DbPersistenceService) jbpmContext.getServices()
      .getPersistenceService();
    if (persistenceService != null) {
      Session session = persistenceService.getSession();
      List res = session.createSQLQuery("select " + idName + " from " + tableName
        + " order by " + idName + " desc").list();
      if (res != null && res.size() > 0) {
        Object obj = res.get(0);
        log.debug(tableName + "::" + idName + "=" + obj);
        id = ((Number) obj).intValue();
      }
    }
    return id;

  }
}