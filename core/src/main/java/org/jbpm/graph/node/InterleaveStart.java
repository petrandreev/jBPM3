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

import java.util.ArrayList;
import java.util.Collection;

import org.dom4j.Element;
import org.jbpm.JbpmException;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.xml.JpdlXmlReader;

/**
 * is an unordered set of child nodeMap.  the path of execution will 
 * be given to each node exactly once.   the sequence of the child
 * nodeMap will be determined at runtime.  this implements the 
 * workflow pattern interleved parallel routing. 
 * 
 * If no script is supplied, the transition names will be sequenced
 * in arbitrary order.
 * If a script is provided, the variable transitionNames contains the 
 * available transition names. The returned value has to be one of 
 * those transitionNames.
 * Instead of supplying a script, its also possible to subclass this 
 * class and override the selectTransition method.
 */
public class InterleaveStart extends Node {
  
  private static final long serialVersionUID = 1L;

  String variableName = "interleave-transition-names";
  Interleaver interleaver = new DefaultInterleaver(); 
  
  public interface Interleaver {
    String selectNextTransition(Collection transitionNames);
  }
  
  public class DefaultInterleaver implements Interleaver {
    public String selectNextTransition(Collection transitionNames) {
      return (String) transitionNames.iterator().next();
    }
  }
  
  public InterleaveStart() {
  }

  public InterleaveStart(String name) {
    super(name);
  }

  public void read(Element element, JpdlXmlReader jpdlReader) {
    // TODO
    
    // just making sure that the context definition is present
    // because the interleave node needs the context instance at runtime
    ProcessDefinition processDefinition = jpdlReader.getProcessDefinition();
    if (processDefinition.getDefinition(ContextDefinition.class)==null) {
      processDefinition.addDefinition(new ContextDefinition());
    }
  }

  public void write(Element element) {
    // TODO
  }

  public void execute(ExecutionContext executionContext) {
    Token token = executionContext.getToken();
    Collection transitionNames = retrieveTransitionNames(token);
    // if this is the first time we enter
    if ( transitionNames == null ) {
      // collect all leaving transition names
      transitionNames = new ArrayList(getTransitionNames(token));
    }
    
    // select one of the remaining transition names
    String nextTransition = interleaver.selectNextTransition(transitionNames);
    // remove it from the remaining transitions
    transitionNames.remove(nextTransition);

    // store the transition names
    storeTransitionNames(transitionNames,token);

    // pass the token over the selected transition
    token.getNode().leave(executionContext, nextTransition);
  }

  protected Collection getTransitionNames(Token token) {
    Node node = token.getNode();
    return node.getLeavingTransitionsMap().keySet();
  }

  protected void storeTransitionNames(Collection transitionNames, Token token) {
    ContextInstance ci = token.getProcessInstance().getContextInstance();
    if (ci==null) throw new JbpmException("an interleave start node requires the availability of a context");
    ci.setVariable(variableName,transitionNames, token);
  }

  public Collection retrieveTransitionNames(Token token) {
    ContextInstance ci = token.getProcessInstance().getContextInstance();
    return (Collection) ci.getVariable(variableName, token);
  }

  public void removeTransitionNames(Token token) {
    ContextInstance ci = token.getProcessInstance().getContextInstance();
    ci.setVariable(variableName,null, token);
  }
  
  public Interleaver getInterleaver() {
    return interleaver;
  }
  public void setInterleaver(Interleaver interleaver) {
    this.interleaver = interleaver;
  }
}
