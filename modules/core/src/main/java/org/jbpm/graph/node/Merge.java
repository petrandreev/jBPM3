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

import java.util.Collection;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * TODO is the merge node useful ? 
 * i don't think the merge node is useful because every node has an 
 * implicit merge in front of it (= multiple transitions can arrive in 
 * the same node).  maybe we should just leave this in for the sake 
 * of workflow patterns ?
 */
public class Merge extends Node {
  
  private static final long serialVersionUID = 1L;
  
  boolean isSynchronized = false;
  
  public Merge() {
  }

  public Merge(String name) {
    super(name);
  }

  public void execute(ExecutionContext executionContext) {
    Token token = executionContext.getToken();
    Node mergeNode = token.getNode();
    
    // if this is a simple merge
    if ( ! isSynchronized ) {
      mergeNode.leave(executionContext);

    // this is a synchronizing multi merge
    } else {
      
      Collection<Token> concurrentTokens = token.getParent().getChildren().values();
      boolean reactivate = true;
      for (Token concurrentToken : concurrentTokens) {
        if (! mergeNode.equals(concurrentToken.getNode())) {
          reactivate = false;
          break;
        }        
      }
      
      if ( reactivate ) {
        for (Token concurrentToken : concurrentTokens) {
          mergeNode.leave(new ExecutionContext(concurrentToken));          
        }
      }
    }
  }
  
  public boolean isSynchronized() {
    return isSynchronized;
  }
  public void setSynchronized(boolean isSynchronized) {
    this.isSynchronized = isSynchronized;
  }
}
