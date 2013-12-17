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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmContext;
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

public class ProcessDefinition extends GraphElement implements NodeCollection {

  private static final long serialVersionUID = 1L;

  protected int version = -1;
  protected boolean isTerminationImplicit;
  protected Node startState;
  protected List nodes;
  private transient Map nodesMap;
  protected Map actions;
  protected Map definitions;

  private static final Map moduleClassesByResource = new HashMap();

  // event types //////////////////////////////////////////////////////////////

  private static final String[] EVENT_TYPES = {
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

  /**
   * @deprecated arrays are mutable and thus vulnerable to external manipulation. use
   * {@link #getSupportedEventTypes()} instead
   */
  public static final String[] supportedEventTypes = (String[]) EVENT_TYPES.clone();

  public String[] getSupportedEventTypes() {
    return (String[]) EVENT_TYPES.clone();
  }

  // constructors /////////////////////////////////////////////////////////////

  public ProcessDefinition() {
    processDefinition = this;
  }

  public static ProcessDefinition createNewProcessDefinition() {
    ProcessDefinition processDefinition = new ProcessDefinition();

    // instantiate default modules
    List moduleClasses = getModuleClasses();
    for (Iterator iter = moduleClasses.iterator(); iter.hasNext();) {
      Class moduleClass = (Class) iter.next();
      try {
        ModuleDefinition moduleDefinition = (ModuleDefinition) moduleClass.newInstance();
        processDefinition.addDefinition(moduleDefinition);
      }
      catch (InstantiationException e) {
        throw new JbpmException("failed to instantiate " + moduleClass, e);
      }
      catch (IllegalAccessException e) {
        throw new JbpmException(ProcessDefinition.class + " has no access to " + moduleClass, e);
      }
    }

    return processDefinition;
  }

  private static List getModuleClasses() {
    String resource = Configs.getString("resource.default.modules");
    synchronized (moduleClassesByResource) {
      List moduleClasses = (List) moduleClassesByResource.get(resource);
      if (moduleClasses == null) {
        moduleClasses = loadModuleClasses(resource);
        moduleClassesByResource.put(resource, moduleClasses);
      }
      return moduleClasses;
    }
  }

  private static List loadModuleClasses(String resource) {
    Properties properties = ClassLoaderUtil.getProperties(resource);
    List moduleClasses = new ArrayList();

    Log log = LogFactory.getLog(ProcessDefinition.class);
    boolean debug = log.isDebugEnabled();

    for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
      String moduleClassName = (String) iter.next();
      try {
        Class moduleClass = ClassLoaderUtil.classForName(moduleClassName);
        moduleClasses.add(moduleClass);
        if (debug) log.debug("loaded module " + moduleClassName);
      }
      catch (ClassNotFoundException e) {
        if (debug) log.debug("module class not found: " + moduleClassName, e);
      }
    }
    return moduleClasses;
  }

  public ProcessDefinition(String name) {
    this();
    this.name = name;
  }

  public ProcessDefinition(String[] nodes, String[] transitions) {
    this();
    ProcessFactory.addNodesAndTransitions(this, nodes, transitions);
  }

  public ProcessInstance createProcessInstance() {
    return new ProcessInstance(this);
  }

  public ProcessInstance createProcessInstance(Map variables) {
    return new ProcessInstance(this, variables, null);
  }

  public ProcessInstance createProcessInstance(Map variables, String businessKey) {
    return new ProcessInstance(this, variables, businessKey);
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    if (!equals(processDefinition)) {
      throw new IllegalArgumentException("process definition cannot reference another process definition");
    }
  }

  // equals ///////////////////////////////////////////////////////////////////

  /**
   * Tells whether this process definition is equal to the given object. This method considers
   * two process definitions equal if they are equal in name and version, the name is not null
   * and the version is not negative.
   */
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ProcessDefinition)) return false;

    ProcessDefinition other = (ProcessDefinition) o;
    if (id != 0 && id == other.getId()) return true;

    return name != null && version >= 0 && name.equals(other.getName())
      && version == other.getVersion();
  }

  /**
   * Computes the hash code for this process definition. Process definitions with a null name or
   * a negative version will return their {@linkplain System#identityHashCode(Object) identity
   * hash code}.
   */
  public int hashCode() {
    if (name == null || version < 0) return System.identityHashCode(this);

    int result = 224001527 + name.hashCode();
    result = 1568661329 * result + version;
    return result;
  }

  // parsing //////////////////////////////////////////////////////////////////

  /**
   * parse a process definition from an xml string.
   * 
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseXmlString(String xml) {
    StringReader stringReader = new StringReader(xml);
    JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(stringReader));
    return jpdlReader.readProcessDefinition();
  }

  /**
   * parse a process definition from an xml resource file.
   * 
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
   * 
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseXmlInputStream(InputStream inputStream) {
    JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(inputStream));
    return jpdlReader.readProcessDefinition();
  }

  /**
   * parse a process definition from an xml reader.
   * 
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseXmlReader(Reader reader) {
    JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(reader));
    return jpdlReader.readProcessDefinition();
  }

  /**
   * parse a process definition from a process archive zip-stream.
   * 
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseParZipInputStream(ZipInputStream zipInputStream)
    throws IOException {
    return new ProcessArchive(zipInputStream).parseProcessDefinition();
  }

  /**
   * parse a process definition from a process archive resource.
   * 
   * @throws org.jbpm.jpdl.JpdlException if parsing reported an error.
   */
  public static ProcessDefinition parseParResource(String parResource) throws IOException {
    return parseParZipInputStream(new ZipInputStream(ClassLoaderUtil.getStream(parResource)));
  }

  // nodes ////////////////////////////////////////////////////////////////////

  // javadoc description in NodeCollection
  public List getNodes() {
    return nodes;
  }

  // javadoc description in NodeCollection
  public Map getNodesMap() {
    if (nodesMap == null) {
      nodesMap = new HashMap();
      if (nodes != null) {
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
          Node node = (Node) iter.next();
          nodesMap.put(node.getName(), node);
        }
      }
    }
    return nodesMap;
  }

  // javadoc description in NodeCollection
  public Node getNode(String name) {
    if (nodes == null) return null;
    return (Node) getNodesMap().get(name);
  }

  // javadoc description in NodeCollection
  public boolean hasNode(String name) {
    if (nodes == null) return false;
    return getNodesMap().containsKey(name);
  }

  // javadoc description in NodeCollection
  public Node addNode(Node node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (nodes == null) nodes = new ArrayList();
    nodes.add(node);
    node.processDefinition = this;
    nodesMap = null;

    if ((node instanceof StartState) && (this.startState == null)) {
      this.startState = node;
    }
    return node;
  }

  // javadoc description in NodeCollection
  public Node removeNode(Node node) {
    Node removedNode = null;
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (nodes != null) {
      if (nodes.remove(node)) {
        removedNode = node;
        removedNode.processDefinition = null;
        nodesMap = null;
      }
    }

    if (startState == removedNode) {
      startState = null;
    }
    return removedNode;
  }

  // javadoc description in NodeCollection
  public void reorderNode(int oldIndex, int newIndex) {
    if (nodes != null && Math.min(oldIndex, newIndex) >= 0
      && Math.max(oldIndex, newIndex) < nodes.size()) {
      Object node = nodes.remove(oldIndex);
      nodes.add(newIndex, node);
    }
    else {
      throw new IndexOutOfBoundsException("could not move node from " + oldIndex + " to "
        + newIndex);
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

  public static String generateNodeName(List nodes) {
    String name;
    if (nodes == null) {
      name = "1";
    }
    else {
      int n = 1;
      while (containsName(nodes, Integer.toString(n)))
        n++;
      name = Integer.toString(n);
    }
    return name;
  }

  private static boolean containsName(List nodes, String name) {
    for (Iterator iter = nodes.iterator(); iter.hasNext();) {
      Node node = (Node) iter.next();
      if (name.equals(node.getName())) return true;
    }
    return false;
  }

  public static Node findNode(NodeCollection nodeCollection, String hierarchicalName) {
    String[] nameParts = hierarchicalName.split("/");

    if (nameParts.length == 1) {
      String nodeName = nameParts[0];
      return nodeName.length() > 0 ? nodeCollection.getNode(nodeName) : null;
    }

    GraphElement currentElement = (GraphElement) nodeCollection;
    int startIndex = 0;
    if (nameParts[0].length() == 0) {
      // hierarchical name started with a '/'
      currentElement = currentElement.getProcessDefinition();
      startIndex = 1;
    }

    for (int i = startIndex; i < nameParts.length; i++) {
      String namePart = nameParts[i];
      if ("..".equals(namePart)) {
        // namePart calls for parent, but current element is absent
        if (currentElement == null) return null;
        currentElement = currentElement.getParent();
      }
      else {
        // namePart calls for child, but current element is not a collection
        if (!(currentElement instanceof NodeCollection)) return null;
        NodeCollection currentCollection = (NodeCollection) currentElement;
        currentElement = currentCollection.getNode(namePart);
      }
    }

    // current element could be the process definition or might be absent
    return currentElement instanceof Node ? (Node) currentElement : null;
  }

  public void setStartState(StartState startState) {
    if (this.startState != startState && this.startState != null) {
      removeNode(this.startState);
    }
    this.startState = startState;
    if (startState != null) {
      addNode(startState);
    }
  }

  public GraphElement getParent() {
    return null;
  }

  // actions //////////////////////////////////////////////////////////////////

  /**
   * creates a bidirectional relation between this process definition and the given action.
   * 
   * @throws IllegalArgumentException if action is null or if action.getName() is null.
   */
  public Action addAction(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("action is null");
    }
    if (action.getName() == null) {
      throw new IllegalArgumentException("action is unnamed");
    }
    if (actions == null) actions = new HashMap();
    actions.put(action.getName(), action);
    action.processDefinition = this;
    return action;
  }

  /**
   * removes the bidirectional relation between this process definition and the given action.
   * 
   * @throws IllegalArgumentException if action is null or if the action was not present in the
   * actions of this process definition.
   */
  public void removeAction(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("action is null");
    }
    if (actions != null) {
      if (!actions.containsValue(action)) {
        throw new IllegalArgumentException("action is not present in process definition");
      }
      actions.remove(action.getName());
      action.processDefinition = null;
    }
  }

  public Action getAction(String name) {
    return actions != null ? (Action) actions.get(name) : null;
  }

  public Map getActions() {
    return actions;
  }

  public boolean hasActions() {
    return actions != null && !actions.isEmpty();
  }

  // module definitions ///////////////////////////////////////////////////////

  public Object createInstance() {
    return new ProcessInstance(this);
  }

  public ModuleDefinition addDefinition(ModuleDefinition moduleDefinition) {
    if (moduleDefinition == null) {
      throw new IllegalArgumentException("module definition is null");
    }

    if (definitions == null) definitions = new HashMap();
    definitions.put(moduleDefinition.getName(), moduleDefinition);
    moduleDefinition.setProcessDefinition(this);
    return moduleDefinition;
  }

  public ModuleDefinition removeDefinition(ModuleDefinition moduleDefinition) {
    if (moduleDefinition == null) {
      throw new IllegalArgumentException("module definition is null");
    }

    ModuleDefinition removedDefinition = null;
    if (definitions != null) {
      removedDefinition = (ModuleDefinition) definitions.remove(moduleDefinition.getClass()
        .getName());
      if (removedDefinition != null) {
        moduleDefinition.setProcessDefinition(null);
      }
    }
    return removedDefinition;
  }

  public ModuleDefinition getDefinition(Class clazz) {
    ModuleDefinition moduleDefinition = null;
    if (definitions != null) {
      moduleDefinition = (ModuleDefinition) definitions.get(clazz.getName());
    }
    return moduleDefinition;
  }

  public ContextDefinition getContextDefinition() {
    return (ContextDefinition) getDefinition(ContextDefinition.class);
  }

  public FileDefinition getFileDefinition() {
    return (FileDefinition) getDefinition(FileDefinition.class);
  }

  public TaskMgmtDefinition getTaskMgmtDefinition() {
    return (TaskMgmtDefinition) getDefinition(TaskMgmtDefinition.class);
  }

  public Map getDefinitions() {
    return definitions;
  }

  public void setDefinitions(Map definitions) {
    this.definitions = definitions;
  }

  // getters and setters //////////////////////////////////////////////////////

  public int getVersion() {
    return version;
  }

  /**
   * Sets the version of this process. Generally the version is assigned automatically upon
   * {@linkplain JbpmContext#deployProcessDefinition(ProcessDefinition) deployment}.
   * 
   * @param version the version to assign. Automatic versioning starts from 1. Any negative
   * value is regarded as an unknown or <code>null</code> version. The meaning of version 0 is
   * undefined.
   */
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
