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
package org.jbpm.graph.def;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipInputStream;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.ProcessFactory;
import org.jbpm.graph.node.StartState;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.par.ProcessArchive;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.util.ClassLoaderUtil;
import org.xml.sax.InputSource;

public class ProcessDefinition extends GraphElement implements NodeCollection {

  private static final long serialVersionUID = 1L;
  
  protected int version = -1;
  protected boolean isTerminationImplicit = false;
  protected Node startState = null;
  protected List<Node> nodes = null;
  transient Map<String, Node> nodesMap = null;
  protected Map<String, Action> actions = null;
  protected Map<String, ModuleDefinition> definitions = null;

  // event types //////////////////////////////////////////////////////////////

  public static final String[] supportedEventTypes = new String[]{
    Event.EVENTTYPE_PROCESS_START,
    Event.EVENTTYPE_PROCESS_END,
    Event.EVENTTYPE_NODE_ENTER,
    Event.EVENTTYPE_NODE_LEAVE,
    Event.EVENTTYPE_TASK_CREATE,
    Event.EVENTTYPE_TASK_ASSIGN,
    Event.EVENTTYPE_TASK_START,
    Event.EVENTTYPE_TASK_END,
    Event.EVENTTYPE_TRANSITION,
    Event.EVENTTYPE_BEFORE_SIGNAL,
    Event.EVENTTYPE_AFTER_SIGNAL,
    Event.EVENTTYPE_SUPERSTATE_ENTER,
    Event.EVENTTYPE_SUPERSTATE_LEAVE,
    Event.EVENTTYPE_SUBPROCESS_CREATED,
    Event.EVENTTYPE_SUBPROCESS_END,
    Event.EVENTTYPE_TIMER
  };
  public String[] getSupportedEventTypes() {
    return supportedEventTypes;
  }

  // constructors /////////////////////////////////////////////////////////////

  public ProcessDefinition() {
    this.processDefinition = this;
  }

  public static ProcessDefinition createNewProcessDefinition() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    
    // now add all the default modules that are configured in the file jbpm.default.modules
    String resource = JbpmConfiguration.Configs.getString("resource.default.modules");
    Properties defaultModulesProperties = ClassLoaderUtil.getProperties(resource);
    for (Object key : defaultModulesProperties.keySet()) {
      String moduleClassName = (String) key;
      try {
        ModuleDefinition moduleDefinition = (ModuleDefinition) ClassLoaderUtil.classForName(moduleClassName).newInstance();
        processDefinition.addDefinition(moduleDefinition);
        
      } catch (Exception e) {
        throw new JbpmException("couldn't instantiate default module '"+moduleClassName+"'", e);
      }      
    }
    return processDefinition;
  }

  public ProcessDefinition(String name) {
    this.processDefinition = this;
    this.name = name;
  }

  public ProcessDefinition(String[] nodes, String[] transitions) {
    this.processDefinition = this;
    ProcessFactory.addNodesAndTransitions(this, nodes, transitions);
  }

  public ProcessInstance createProcessInstance() {
    return new ProcessInstance(this);
  }

  public ProcessInstance createProcessInstance(Map<String, Object> variables) {
    return new ProcessInstance(this, variables, null);
  }

  public ProcessInstance createProcessInstance(Map<String, Object> variables, String businessKey) {
    return new ProcessInstance(this, variables, businessKey);
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    if (! this.equals(processDefinition)) {
      throw new JbpmException("can't set the process-definition-property of a process defition to something else then a self-reference");
    }
  }

  // parsing //////////////////////////////////////////////////////////////////
  
  /**
   * parse a process definition from an xml string.
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseXmlString(String xml) {
    StringReader stringReader = new StringReader(xml);
    JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(stringReader));
    return jpdlReader.readProcessDefinition();
  }

  /**
   * parse a process definition from an xml resource file.
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseXmlResource(String xmlResource) {
    URL resourceURL = ClassLoaderUtil.getClassLoader().getResource(xmlResource);
    if (resourceURL == null) {
      throw new JpdlException("resource not found: " + xmlResource);
    }
    JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(resourceURL.toString())); 
    return jpdlReader.readProcessDefinition();
  }

  /**
   * parse a process definition from an xml input stream.
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseXmlInputStream(InputStream inputStream) {
    JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(inputStream)); 
    return jpdlReader.readProcessDefinition();
  }

  /**
   * parse a process definition from an xml reader.
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseXmlReader(Reader reader) {
    JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(reader));
    return jpdlReader.readProcessDefinition();
  }

  /**
   * parse a process definition from a process archive zip-stream.
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseParZipInputStream(ZipInputStream zipInputStream) throws IOException {
    return new ProcessArchive(zipInputStream).parseProcessDefinition();
  }

  /**
   * parse a process definition from a process archive resource.
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseParResource(String parResource) throws IOException {
    return parseParZipInputStream(new ZipInputStream(ClassLoaderUtil.getStream(parResource)));
  }

  // nodes ////////////////////////////////////////////////////////////////////

  // javadoc description in NodeCollection
  public List<Node> getNodes() {
    return nodes;
  }

  // javadoc description in NodeCollection
  public Map<String, Node> getNodesMap() {
    if (nodesMap==null) {
      nodesMap = new HashMap<String, Node>();
      if (nodes!=null) {
        for (Node node : nodes) {
          nodesMap.put(node.getName(),node);
        }
      }
    }
    return nodesMap;
  }

  // javadoc description in NodeCollection
  public Node getNode(String name) {
    if (nodes==null) return null;
    return getNodesMap().get(name);
  }

  // javadoc description in NodeCollection
  public boolean hasNode(String name) {
    if (nodes==null) return false;
    return getNodesMap().containsKey(name);
  }

  // javadoc description in NodeCollection
  public Node addNode(Node node) {
    if (node == null) throw new IllegalArgumentException("can't add a null node to a processdefinition");
    if (nodes == null) nodes = new ArrayList<Node>();
    nodes.add(node);
    node.processDefinition = this;
    nodesMap = null;
    
    if( (node instanceof StartState)
        && (this.startState==null)
      ) {
      this.startState = node;
    }
    return node;
  }

  // javadoc description in NodeCollection
  public Node removeNode(Node node) {
    Node removedNode = null;
    if (node == null) throw new IllegalArgumentException("can't remove a null node from a process definition");
    if (nodes != null) {
      if (nodes.remove(node)) {
        removedNode = node;
        removedNode.processDefinition = null;
        nodesMap = null;
      }
    }
    
    if (startState==removedNode) {
      startState = null;
    }
    return removedNode;
  }

  // javadoc description in NodeCollection
  public void reorderNode(int oldIndex, int newIndex) {
    if ( (nodes!=null)
         && (Math.min(oldIndex, newIndex)>=0)
         && (Math.max(oldIndex, newIndex)<nodes.size()) ) {
      Node o = nodes.remove(oldIndex);
      nodes.add(newIndex, o);
    } else {
      throw new IndexOutOfBoundsException("couldn't reorder element from index '"+oldIndex+"' to index '"+newIndex+"' in nodeList '"+nodes+"'");
    }
  }

  // javadoc description in NodeCollection
  public String generateNodeName() {
    return generateNodeName(nodes);
  }

  // javadoc description in NodeCollection
  public Node findNode(String hierarchicalName) {
    return findNode(this, hierarchicalName);
  }

  public static String generateNodeName(List<Node> nodes) {
    String name = null;
    if (nodes==null) {
      name = "1";
    } else {
      int n = 1;
      while (containsName(nodes, Integer.toString(n))) n++;
      name = Integer.toString(n);
    }
    return name;
  }

  static boolean containsName(List<Node> nodes, String name) {
    for (Node node : nodes) {
      if ( name.equals(node.getName()) ) {
        return true;
      }
    }
    return false;
  }

  public static Node findNode(NodeCollection nodeCollection, String hierarchicalName) {
    Node node = null;
    if ((hierarchicalName != null) && (!"".equals(hierarchicalName.trim())) ) {
      
      if ( (hierarchicalName.startsWith("/"))
            && (nodeCollection instanceof SuperState) ){
        nodeCollection = ((SuperState)nodeCollection).getProcessDefinition();
      }

      StringTokenizer tokenizer = new StringTokenizer(hierarchicalName, "/");
      while (tokenizer.hasMoreElements()) {
        String namePart = tokenizer.nextToken();
        if ("..".equals(namePart) ) {
          if (nodeCollection instanceof ProcessDefinition) {
            throw new JbpmException("couldn't find node '"+hierarchicalName+"' because of a '..' on the process definition.");
          }
          nodeCollection = (NodeCollection) ((GraphElement)nodeCollection).getParent();
        } else if ( tokenizer.hasMoreElements() ) {
          nodeCollection = (NodeCollection)nodeCollection.getNode(namePart);
        } else {
          node = nodeCollection.getNode(namePart);
        }
      }
    }
    return node;
  }
  
  public void setStartState(StartState startState) {
    if ( (this.startState!=startState)
         && (this.startState!=null) ){
      removeNode(this.startState);
    }
    this.startState = startState;
    if (startState!=null) {
      addNode(startState);
    }
  }

  public GraphElement getParent() {
    return null;
  }

  // actions //////////////////////////////////////////////////////////////////

  /**
   * creates a bidirectional relation between this process definition and the given action.
   * @throws IllegalArgumentException if action is null or if action.getName() is null.
   */
  public Action addAction(Action action) {
    if (action == null) throw new IllegalArgumentException("can't add a null action to an process definition");
    if (action.getName() == null) throw new IllegalArgumentException("can't add an unnamed action to an process definition");
    if (actions == null) actions = new HashMap<String, Action>();
    actions.put(action.getName(), action);
    action.processDefinition = this;
    return action;
  }

  /**
   * removes the bidirectional relation between this process definition and the given action.
   * @throws IllegalArgumentException if action is null or if the action was not present in the actions of this process definition.
   */
  public void removeAction(Action action) {
    if (action == null) throw new IllegalArgumentException("can't remove a null action from an process definition");
    if (actions != null) {
      if (! actions.containsValue(action)) {
        throw new IllegalArgumentException("can't remove an action that is not part of this process definition");
      }
      actions.remove(action.getName());
      action.processDefinition = null;
    }
  }

  public Action getAction(String name) {
    if (actions == null) return null;
    return actions.get(name);
  }
  
  public Map<String, Action> getActions() {
    return actions;
  }

  public boolean hasActions() {
    return ( (actions!=null)
             && (actions.size()>0) );
  }

  // module definitions ///////////////////////////////////////////////////////

  public Object createInstance() {
    return new ProcessInstance(this);
  }

  public ModuleDefinition addDefinition(ModuleDefinition moduleDefinition) {
    if (moduleDefinition == null) throw new IllegalArgumentException("can't add a null moduleDefinition to a process definition");
    if (definitions == null)
      definitions = new HashMap<String, ModuleDefinition>();
    definitions.put(moduleDefinition.getClass().getName(), moduleDefinition);
    moduleDefinition.setProcessDefinition(this);
    return moduleDefinition;
  }
  
  public ModuleDefinition removeDefinition(ModuleDefinition moduleDefinition) {
    ModuleDefinition removedDefinition = null;
    if (moduleDefinition == null) throw new IllegalArgumentException("can't remove a null moduleDefinition from a process definition");
    if (definitions != null) {
      removedDefinition = definitions.remove(moduleDefinition.getClass().getName());
      if (removedDefinition!=null) {
        moduleDefinition.setProcessDefinition(null);
      }
    }
    return removedDefinition;
  }

  public <D extends ModuleDefinition> D getDefinition(Class<D> clazz) {
    D moduleDefinition = null;
    if (definitions != null) {
      moduleDefinition = clazz.cast(definitions.get(clazz.getName()));
    }
    return moduleDefinition;
  }
  
  public ContextDefinition getContextDefinition() {
    return getDefinition(ContextDefinition.class);
  }

  public FileDefinition getFileDefinition() {
    return getDefinition(FileDefinition.class);
  }
  public TaskMgmtDefinition getTaskMgmtDefinition() {
    return getDefinition(TaskMgmtDefinition.class);
  }

  public Map<String, ModuleDefinition> getDefinitions() {
    return definitions;
  }

  public void setDefinitions(Map<String, ModuleDefinition> definitions) {
    this.definitions = definitions;
  }

  // getters and setters //////////////////////////////////////////////////////

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public Node getStartState() {
    return startState;
  }
  
  public void setStartState(Node startState) {
    this.startState = startState;
  }

  public boolean isTerminationImplicit() {
    return isTerminationImplicit;
  }

  public void setTerminationImplicit(boolean isTerminationImplicit) {
    this.isTerminationImplicit = isTerminationImplicit;
  }
}
