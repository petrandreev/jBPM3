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
package org.jbpm.graph.node;
import org.jbpm.graph.def.*;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.exe.*;

public class MilestoneNode extends Node {
  
  private static final long serialVersionUID = 1L;
  
  String tokenPath = ".";
  
  public MilestoneNode() {
  }

  public MilestoneNode(String name) {
    super(name);
  }

  public void execute(ExecutionContext executionContext) {
    Token token = executionContext.getToken();
    // get the token on which the milestone should be verified
    Token milestoneToken = token.findToken( tokenPath );
    if ( isMilestoneReached( name, milestoneToken ) ) {
      
      // continue to pass the token over the default transition
      token.getNode().leave(executionContext);

    } else {
      addMilestoneListener(name,milestoneToken);
    }
  }
  
  public boolean isMilestoneReached(String milestoneName, Token token) {
    MilestoneInstance mi = MilestoneInstance.getMilestoneInstance(milestoneName, token);
    return (mi != null ? mi.isReached() : false);
  }

  public void addMilestoneListener(String milestoneName, Token token) {
    MilestoneInstance mi = MilestoneInstance.getMilestoneInstance(milestoneName, token);
    mi.addListener(token);
  }


  public String getTokenPath() {
    return tokenPath;
  }
  public void setTokenPath(String relativeTokenPath) {
    this.tokenPath = relativeTokenPath;
  }
}
