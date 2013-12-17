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
package org.jbpm.mock;

import java.util.Iterator;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.persistence.db.DbPersistenceService;

/**
 * @author Alejandro Guizar
 */
public class EsbActionHandler implements ActionHandler {

  private String esbCategoryName;
  private String esbServiceName;
  private Element bpmToEsbVars;
  private Element esbToBpmVars;
  private String exceptionTransition;

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(EsbActionHandler.class);

  public void execute(ExecutionContext executionContext) {
    log.debug("invoking " + esbCategoryName + "::" + esbServiceName);
    try {
      for (Iterator i = bpmToEsbVars.elementIterator(); i.hasNext();) {
        Element bpmToEsbVar = (Element) i.next();
        String var = bpmToEsbVar.attributeValue("bpm");
        Object value = executionContext.getVariable(var);
        log.debug("read " + value + " from variable " + var);
      }
      Random random = new Random();
      for (Iterator i = esbToBpmVars.elementIterator(); i.hasNext();) {
        Element esbToBpmVar = (Element) i.next();
        String var = esbToBpmVar.attributeValue("bpm");
        byte[] value = new byte[random.nextInt(2048)];
        random.nextBytes(value);
        executionContext.setVariable(var, value);
        log.debug("wrote " + value.length + " bytes to variable " + var);
      }
      executionContext.leaveNode();
    }
    catch (RuntimeException e) {
      if (DbPersistenceService.isPersistenceException(e)) throw e;
      log.debug("possibly recoverable exception in esb action", e);
      executionContext.leaveNode(exceptionTransition);
    }
  }

}
