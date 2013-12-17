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
package org.jbpm.examples.websale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class ShipItem implements ActionHandler {

  private static final long serialVersionUID = 1L;
  
  String swimlaneName;
  String msg;

  public void execute(ExecutionContext executionContext) throws Exception {
    TaskMgmtDefinition taskMgmtDefinition = executionContext.getProcessDefinition().getTaskMgmtDefinition();
    TaskMgmtInstance taskMgmtInstance = executionContext.getTaskMgmtInstance();
    ContextInstance contextInstance = executionContext.getContextInstance();
    
    Swimlane shipperSwimlane = taskMgmtDefinition.getSwimlane("shipper");
    SwimlaneInstance shipperSwimlaneInstance = taskMgmtInstance.getInitializedSwimlaneInstance(executionContext, shipperSwimlane);

    String actorId = shipperSwimlaneInstance.getActorId();
    
    String displayMsg = replace(msg, "${"+swimlaneName+"}", actorId); 
    displayMsg = replace(displayMsg, "${item}", (String)contextInstance.getVariable("item")); 
    displayMsg = replace(displayMsg, "${address}", (String)contextInstance.getVariable("address"));
    
    log.info("###############################################");
    log.info("### "+displayMsg);
    log.info("###############################################");
    
    executionContext.leaveNode();
  }

  static String replace(String msg, String pattern, String replacement) {
    String replaced = null;
    int pos = msg.indexOf(pattern);
    if (pos!=-1) {
      replaced = msg.substring(0,pos)
                 +replacement
                 +msg.substring(pos+pattern.length());
    }
    return replaced;
  }
  
  private static final Log log = LogFactory.getLog(ShipItem.class);
}
