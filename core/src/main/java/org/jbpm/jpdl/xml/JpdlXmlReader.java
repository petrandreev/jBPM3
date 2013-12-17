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
package org.jbpm.jpdl.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xml.sax.InputSource;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmException;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.graph.action.ActionTypes;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ExceptionHandler;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.NodeCollection;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.node.Decision;
import org.jbpm.graph.node.NodeTypes;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.instantiation.Delegation;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.mail.Mail;
import org.jbpm.scheduler.def.CancelTimerAction;
import org.jbpm.scheduler.def.CreateTimerAction;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

public class JpdlXmlReader implements ProblemListener {

  protected InputSource inputSource;
  protected List problems = new ArrayList();
  protected ProblemListener problemListener;
  protected ProcessDefinition processDefinition;
  protected String initialNodeName;
  protected Collection unresolvedTransitionDestinations = new ArrayList();
  protected Collection unresolvedActionReferences = new ArrayList();

  /** process document, available after {@link #readProcessDefinition()} */
  protected Document document;

  /** for auto-numbering anonymous timers. */
  private int timerNumber;

  private XMLWriter xmlWriter;

  public JpdlXmlReader(InputSource inputSource) {
    this.inputSource = inputSource;
  }

  public JpdlXmlReader(InputSource inputSource, ProblemListener problemListener) {
    this.inputSource = inputSource;
    this.problemListener = problemListener;
  }

  public JpdlXmlReader(Reader reader) {
    this(new InputSource(reader));
  }

  public void close() throws IOException {
    InputStream byteStream = inputSource.getByteStream();
    if (byteStream != null)
      byteStream.close();
    else {
      Reader charStream = inputSource.getCharacterStream();
      if (charStream != null) charStream.close();
    }
    document = null;
  }

  public ProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  public void addProblem(Problem problem) {
    problems.add(problem);
    if (problemListener != null) problemListener.addProblem(problem);
  }

  public void addError(String description) {
    addProblem(new Problem(Problem.LEVEL_ERROR, description));
  }

  public void addError(String description, Throwable exception) {
    addProblem(new Problem(Problem.LEVEL_ERROR, description, exception));
  }

  public void addWarning(String description) {
    addProblem(new Problem(Problem.LEVEL_WARNING, description));
  }

  public ProcessDefinition readProcessDefinition() {
    // create a new definition
    processDefinition = ProcessDefinition.createNewProcessDefinition();

    // clear lists
    problems.clear();
    unresolvedTransitionDestinations.clear();
    unresolvedActionReferences.clear();

    try {
      // parse the document into a dom tree
      document = JpdlParser.parse(inputSource, this);
    }
    catch (DocumentException e) {
      throw new JpdlException("failed to parse process document", e);
    }

    // read the process name
    Element root = document.getRootElement();
    parseProcessDefinitionAttributes(root);

    // get the process description
    String description = root.elementTextTrim("description");
    if (description != null) processDefinition.setDescription(description);

    // first pass: read most content
    readSwimlanes(root);
    readActions(root, null, null);
    readNodes(root, processDefinition);
    readEvents(root, processDefinition);
    readExceptionHandlers(root, processDefinition);
    readTasks(root, null);

    // second pass processing
    resolveTransitionDestinations();
    resolveActionReferences();
    verifySwimlaneAssignments();

    if (Problem.containsProblemsOfLevel(problems, Problem.LEVEL_ERROR)) {
      throw new JpdlException(problems);
    }
    return processDefinition;
  }

  protected void parseProcessDefinitionAttributes(Element root) {
    processDefinition.setName(root.attributeValue("name"));
    initialNodeName = root.attributeValue("initial");
  }

  protected void readSwimlanes(Element processDefinitionElement) {
    TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
    for (Iterator iter = processDefinitionElement.elementIterator("swimlane"); iter.hasNext();) {
      Element swimlaneElement = (Element) iter.next();
      String swimlaneName = swimlaneElement.attributeValue("name");
      if (swimlaneName == null) {
        addWarning("unnamed swimlane detected");
      }
      else {
        Swimlane swimlane = new Swimlane(swimlaneName);
        Element assignmentElement = swimlaneElement.element("assignment");

        if (assignmentElement != null) {
          if (assignmentElement.attribute("actor-id") != null
            || assignmentElement.attribute("pooled-actors") != null) {
            swimlane.setActorIdExpression(assignmentElement.attributeValue("actor-id"));
            swimlane.setPooledActorsExpression(assignmentElement.attributeValue("pooled-actors"));
          }
          else {
            Delegation assignmentDelegation = readAssignmentDelegation(assignmentElement);
            swimlane.setAssignmentDelegation(assignmentDelegation);
          }
        }
        else {
          Task startTask = taskMgmtDefinition.getStartTask();
          if (startTask == null || startTask.getSwimlane() != swimlane) {
            addWarning("swimlane '" + swimlaneName + "' does not have an assignment");
          }
        }
        taskMgmtDefinition.addSwimlane(swimlane);
      }
    }
  }

  public void readNodes(Element element, NodeCollection nodeCollection) {
    for (Iterator iter = element.elementIterator(); iter.hasNext();) {
      Element nodeElement = (Element) iter.next();
      String nodeName = nodeElement.getName();
      // get the node type
      Class nodeType = NodeTypes.getNodeType(nodeName);
      if (nodeType != null) {
        try {
          // instantiate the node
          Node node = (Node) nodeType.newInstance();
          node.setProcessDefinition(processDefinition);

          // check for duplicate start-states
          if (node instanceof StartState && processDefinition.getStartState() != null) {
            addError("only one start-state is allowed");
          }
          else {
            // read the common node parts of the element
            readNode(nodeElement, node, nodeCollection);
            // let node parse its specific configuration
            node.read(nodeElement, this);
          }
        }
        catch (InstantiationException e) {
          throw new JbpmException("failed to instantiate " + nodeType, e);
        }
        catch (IllegalAccessException e) {
          throw new JbpmException(getClass() + " has no access to " + nodeType, e);
        }
      }
    }
  }

  public void readTasks(Element element, TaskNode taskNode) {
    List elements = element.elements("task");
    if (elements.size() > 0) {
      TaskMgmtDefinition tmd = processDefinition.getTaskMgmtDefinition();
      for (Iterator iter = elements.iterator(); iter.hasNext();) {
        Element taskElement = (Element) iter.next();
        readTask(taskElement, tmd, taskNode);
      }
    }
  }

  public Task readTask(Element taskElement, TaskMgmtDefinition taskMgmtDefinition,
    TaskNode taskNode) {
    Task task = new Task();
    task.setProcessDefinition(processDefinition);

    // get the task name
    String name = taskElement.attributeValue("name");
    if (name != null) {
      task.setName(name);
      taskMgmtDefinition.addTask(task);
    }
    else if (taskNode != null) {
      task.setName(taskNode.getName());
      taskMgmtDefinition.addTask(task);
    }

    // get the task description
    String description = taskElement.elementTextTrim("description");
    if (description == null) description = taskElement.attributeValue("description");
    task.setDescription(description);

    // get the condition
    String condition = taskElement.elementTextTrim("condition");
    if (condition == null) condition = taskElement.attributeValue("condition");
    task.setCondition(condition);

    // parse common subelements
    readTaskTimers(taskElement, task);
    readEvents(taskElement, task);
    readExceptionHandlers(taskElement, task);

    // due date
    String duedateText = taskElement.attributeValue("duedate");
    if (duedateText == null) duedateText = taskElement.attributeValue("dueDate");
    task.setDueDate(duedateText);

    // priority
    String priorityText = taskElement.attributeValue("priority");
    if (priorityText != null) task.setPriority(Task.parsePriority(priorityText));

    // if this task is in the context of a taskNode, associate them
    if (taskNode != null) taskNode.addTask(task);

    // blocking
    String blockingText = taskElement.attributeValue("blocking");
    task.setBlocking(readBoolean(blockingText, false));

    // signalling
    String signallingText = taskElement.attributeValue("signalling");
    task.setSignalling(readBoolean(signallingText, true));

    // assignment
    String swimlaneName = taskElement.attributeValue("swimlane");
    Element assignmentElement = taskElement.element("assignment");

    // if there is a swimlane attribute specified
    if (swimlaneName != null) {
      Swimlane swimlane = taskMgmtDefinition.getSwimlane(swimlaneName);
      if (swimlane == null) {
        addWarning("task references unknown swimlane: " + taskElement.getPath());
      }
      else {
        task.setSwimlane(swimlane);
      }
    }
    // else if there is a direct assignment specified
    else if (assignmentElement != null) {
      if (assignmentElement.attribute("actor-id") != null
        || assignmentElement.attribute("pooled-actors") != null) {
        task.setActorIdExpression(assignmentElement.attributeValue("actor-id"));
        task.setPooledActorsExpression(assignmentElement.attributeValue("pooled-actors"));
      }
      else {
        Delegation assignmentDelegation = readAssignmentDelegation(assignmentElement);
        task.setAssignmentDelegation(assignmentDelegation);
      }
    }
    // if no assignment or swimlane is specified
    else {
      // user has to manage assignment manually, so better inform him/her
      addProblem(new Problem(Problem.LEVEL_INFO, "no assignment specified for task: "
        + taskElement.getPath()));
    }

    // notify
    String notificationsText = taskElement.attributeValue("notify");
    if (readBoolean(notificationsText, false)) {
      // create mail action
      Delegation delegation = createMailDelegation(Event.EVENTTYPE_TASK_ASSIGN);
      Action action = new Action(delegation);
      action.setProcessDefinition(processDefinition);
      action.setName(task.getName());

      // attach action to task assign event
      addAction(task, Event.EVENTTYPE_TASK_ASSIGN, action);
    }

    // task controller
    Element taskControllerElement = taskElement.element("controller");
    if (taskControllerElement != null) {
      task.setTaskController(readTaskController(taskControllerElement));
    }

    return task;
  }

  public boolean readBoolean(String text, boolean defaultValue) {
    if (text == null) return defaultValue;
    if ("true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)
      || "on".equalsIgnoreCase(text)) return true;
    if ("false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text)
      || "off".equalsIgnoreCase(text)) return false;
    return defaultValue;
  }

  protected Delegation readAssignmentDelegation(Element assignmentElement) {
    Delegation assignmentDelegation = new Delegation();
    assignmentDelegation.setProcessDefinition(processDefinition);

    String expression = assignmentElement.attributeValue("expression");
    if (expression != null) {
      // read assigment expression
      Element config = DocumentHelper.createElement("expression");
      config.setText(expression);

      assignmentDelegation.setClassName("org.jbpm.identity.assignment.ExpressionAssignmentHandler");
      assignmentDelegation.setConfiguration(writeElement(config));
    }
    else {
      String actorId = assignmentElement.attributeValue("actor-id");
      String pooledActors = assignmentElement.attributeValue("pooled-actors");
      if (actorId != null || pooledActors != null) {
        // read assignment actors
        Element config = DocumentHelper.createElement("configuration");
        if (actorId != null) config.addElement("actorId").setText(actorId);
        if (pooledActors != null) config.addElement("pooledActors").setText(pooledActors);

        assignmentDelegation.setClassName("org.jbpm.taskmgmt.assignment.ActorAssignmentHandler");
        assignmentDelegation.setConfiguration(writeElementContent(config));
      }
      else {
        // parse custom assignment handler
        assignmentDelegation.read(assignmentElement, this);
      }
    }

    return assignmentDelegation;
  }

  protected TaskController readTaskController(Element taskControllerElement) {
    TaskController taskController = new TaskController();

    if (taskControllerElement.attributeValue("class") != null) {
      Delegation taskControllerDelegation = new Delegation();
      taskControllerDelegation.read(taskControllerElement, this);
      taskController.setTaskControllerDelegation(taskControllerDelegation);
    }
    else {
      List variableAccesses = readVariableAccesses(taskControllerElement);
      taskController.setVariableAccesses(variableAccesses);
    }
    return taskController;
  }

  public List readVariableAccesses(Element element) {
    List variableAccesses = new ArrayList();
    for (Iterator iter = element.elementIterator("variable"); iter.hasNext();) {
      Element variableElement = (Element) iter.next();
      // name
      String variableName = variableElement.attributeValue("name");
      if (variableName == null) {
        addWarning("variable name not specified: " + variableElement.getPath());
      }
      // access
      String access = variableElement.attributeValue("access", "read,write");
      // mapped name
      String mappedName = variableElement.attributeValue("mapped-name");
      // variable access
      variableAccesses.add(new VariableAccess(variableName, access, mappedName));
    }
    return variableAccesses;
  }

  public void readStartStateTask(Element startTaskElement, StartState startState) {
    TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
    Task startTask = readTask(startTaskElement, taskMgmtDefinition, null);
    startTask.setStartState(startState);
    if (startTask.getName() == null) startTask.setName(startState.getName());
    taskMgmtDefinition.setStartTask(startTask);
  }

  public void readNode(Element nodeElement, Node node, NodeCollection nodeCollection) {
    // first put the node in its collection. this is done so that the
    // setName later on will be able to differentiate between nodes contained in
    // processDefinitions and nodes contained in superstates
    nodeCollection.addNode(node);

    // get the node name
    String name = nodeElement.attributeValue("name");
    if (name != null) {
      node.setName(name);

      // check if this is the initial node
      if (initialNodeName != null && initialNodeName.equals(node.getFullyQualifiedName())) {
        processDefinition.setStartState(node);
      }
    }

    // get the node description
    String description = nodeElement.elementTextTrim("description");
    if (description != null) node.setDescription(description);

    String asyncText = nodeElement.attributeValue("async");
    if ("exclusive".equalsIgnoreCase(asyncText)) {
      node.setAsyncExclusive(true);
    }
    else if (readBoolean(asyncText, false)) {
      node.setAsync(true);
    }

    // parse common subelements
    readNodeTimers(nodeElement, node);
    readEvents(nodeElement, node);
    readExceptionHandlers(nodeElement, node);

    // save the transitions and parse them at the end
    addUnresolvedTransitionDestination(nodeElement, node);
  }

  protected void readNodeTimers(Element nodeElement, Node node) {
    for (Iterator iter = nodeElement.elementIterator("timer"); iter.hasNext();) {
      Element timerElement = (Element) iter.next();
      readNodeTimer(timerElement, node);
    }
  }

  protected void readNodeTimer(Element timerElement, Node node) {
    String name = timerElement.attributeValue("name", node.getName());
    if (name == null) name = generateTimerName();

    CreateTimerAction createTimerAction = new CreateTimerAction();
    createTimerAction.read(timerElement, this);
    createTimerAction.setTimerName(name);
    createTimerAction.setTimerAction(readSingleAction(timerElement));
    addAction(node, Event.EVENTTYPE_NODE_ENTER, createTimerAction);

    CancelTimerAction cancelTimerAction = new CancelTimerAction();
    cancelTimerAction.setTimerName(name);
    addAction(node, Event.EVENTTYPE_NODE_LEAVE, cancelTimerAction);
  }

  private String generateTimerName() {
    return "timer-" + (timerNumber++);
  }

  protected void readTaskTimers(Element taskElement, Task task) {
    for (Iterator iter = taskElement.elementIterator(); iter.hasNext();) {
      Element element = (Element) iter.next();
      String elementName = element.getName();
      if ("timer".equals(elementName) || "reminder".equals(elementName)) {
        readTaskTimer(element, task);
      }
    }
  }

  protected void readTaskTimer(Element timerElement, Task task) {
    String name = timerElement.attributeValue("name", task.getName());
    if (name == null) name = generateTimerName();

    CreateTimerAction createTimerAction = new CreateTimerAction();
    createTimerAction.read(timerElement, this);
    createTimerAction.setTimerName(name);
    Action action = null;
    if ("timer".equals(timerElement.getName())) {
      action = readSingleAction(timerElement);
    }
    else {
      Delegation mailDelegation = createMailDelegation("task-reminder");
      action = new Action(mailDelegation);
    }
    createTimerAction.setTimerAction(action);
    addAction(task, Event.EVENTTYPE_TASK_CREATE, createTimerAction);

    // read the cancel-event types
    String cancelEventText = timerElement.attributeValue("cancel-event");
    if (cancelEventText != null) {
      // cancel-event is a comma separated list of events
      String[] cancelEventTypes = cancelEventText.split("[\\s,]+");
      if (cancelEventTypes.length > 1 || cancelEventTypes[0].length() > 0) {
        for (int i = 0; i < cancelEventTypes.length; i++) {
          CancelTimerAction cancelTimerAction = new CancelTimerAction();
          cancelTimerAction.setTimerName(name);
          addAction(task, cancelEventTypes[i], cancelTimerAction);
        }
      }
    }
    else {
      // cancel on task end by default
      CancelTimerAction cancelTimerAction = new CancelTimerAction();
      cancelTimerAction.setTimerName(name);
      addAction(task, Event.EVENTTYPE_TASK_END, cancelTimerAction);
    }
  }

  protected void readEvents(Element parentElement, GraphElement graphElement) {
    for (Iterator iter = parentElement.elementIterator("event"); iter.hasNext();) {
      Element eventElement = (Element) iter.next();
      // register event of the defined type
      String eventType = eventElement.attributeValue("type");
      if (!graphElement.hasEvent(eventType)) graphElement.addEvent(new Event(eventType));
      // parse any actions associated to the event
      readActions(eventElement, graphElement, eventType);
    }
  }

  /** Reads actions associated to the given event. */
  public void readActions(Element eventElement, GraphElement graphElement, String eventType) {
    // for all the elements in the event element
    for (Iterator iter = eventElement.elementIterator(); iter.hasNext();) {
      Element actionElement = (Element) iter.next();
      if (ActionTypes.hasActionName(actionElement.getName())) {
        Action action = createAction(actionElement);
        if (graphElement != null && eventType != null) {
          // add the action to the event
          addAction(graphElement, eventType, action);
        }
      }
    }
  }

  protected void addAction(GraphElement graphElement, String eventType, Action action) {
    Event event = graphElement.getEvent(eventType);
    if (event == null) {
      event = new Event(eventType);
      graphElement.addEvent(event);
    }
    event.addAction(action);
  }

  /** Reads the action associated to the given node. */
  public Action readSingleAction(Element nodeElement) {
    // search for the first action element in the node
    for (Iterator iter = nodeElement.elementIterator(); iter.hasNext();) {
      Element candidate = (Element) iter.next();
      if (ActionTypes.hasActionName(candidate.getName())) {
        // parse the action and assign it to this node
        return createAction(candidate);
      }
    }
    return null;
  }

  /** Instantiates and configures an action. */
  public Action createAction(Element actionElement) {
    String actionName = actionElement.getName();
    Class actionType = ActionTypes.getActionType(actionName);
    try {
      // instantiate action
      Action action = (Action) actionType.newInstance();
      // read the common action parts of the element
      readAction(actionElement, action);
      // let action parse its specific configuration
      action.read(actionElement, this);
      return action;
    }
    catch (InstantiationException e) {
      throw new JbpmException("failed to instantiate " + actionType, e);
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(getClass() + " has no access to " + actionType, e);
    }
  }

  /** Configures the common action parts. */
  public void readAction(Element actionElement, Action action) {
    // if a name is specified for this action
    String actionName = actionElement.attributeValue("name");
    if (actionName != null) {
      action.setName(actionName);
      // add the action to the named process action repository
      processDefinition.addAction(action);
    }

    String acceptPropagatedEvents = actionElement.attributeValue("accept-propagated-events");
    action.setPropagationAllowed(readBoolean(acceptPropagatedEvents, true));

    String asyncText = actionElement.attributeValue("async");
    if ("exclusive".equalsIgnoreCase(asyncText)) {
      action.setAsyncExclusive(true);
    }
    else if (readBoolean(asyncText, false)) {
      action.setAsync(true);
    }
  }

  protected void readExceptionHandlers(Element graphDomElement, GraphElement graphElement) {
    for (Iterator iter = graphDomElement.elementIterator("exception-handler"); iter.hasNext();) {
      Element exceptionHandlerElement = (Element) iter.next();
      readExceptionHandler(exceptionHandlerElement, graphElement);
    }
  }

  protected void readExceptionHandler(Element exceptionHandlerElement, GraphElement graphElement) {
    // create the exception handler
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName(exceptionHandlerElement.attributeValue("exception-class"));
    // add it to the graph element
    graphElement.addExceptionHandler(exceptionHandler);

    // read the actions in the body of the exception-handler element
    for (Iterator iter = exceptionHandlerElement.elementIterator(); iter.hasNext();) {
      Element childElement = (Element) iter.next();
      if (ActionTypes.hasActionName(childElement.getName())) {
        Action action = createAction(childElement);
        exceptionHandler.addAction(action);
      }
    }
  }

  // transition destinations are parsed in a second pass //////////////////////

  public void addUnresolvedTransitionDestination(Element nodeElement, Node node) {
    unresolvedTransitionDestinations.add(new Object[] { nodeElement, node });
  }

  public void resolveTransitionDestinations() {
    for (Iterator iter = unresolvedTransitionDestinations.iterator(); iter.hasNext();) {
      Object[] unresolvedTransition = (Object[]) iter.next();
      Element nodeElement = (Element) unresolvedTransition[0];
      Node node = (Node) unresolvedTransition[1];
      resolveTransitionDestinations(nodeElement.elements("transition"), node);
    }
  }

  public void resolveTransitionDestinations(List transitionElements, Node node) {
    for (Iterator iter = transitionElements.iterator(); iter.hasNext();) {
      Element transitionElement = (Element) iter.next();
      resolveTransitionDestination(transitionElement, node);
    }
  }

  /**
   * creates the transition object and configures it by the read attributes
   * 
   * @return the created <code>org.jbpm.graph.def.Transition</code> object (useful, if you want
   * to override this method to read additional configuration properties)
   */
  public Transition resolveTransitionDestination(Element transitionElement, Node node) {
    String transitionName = transitionElement.attributeValue("name");

    Transition transition = new Transition();
    transition.setProcessDefinition(processDefinition);
    transition.setName(transitionName);
    transition.setDescription(transitionElement.elementTextTrim("description"));

    // SOA-2010: conditions only valid on transitions leaving decisions
    boolean conditionPresentOnTransition = true;
    
    // read transition condition
    String condition = transitionElement.attributeValue("condition");
    if (condition == null) {
      conditionPresentOnTransition = false;
      Element conditionElement = transitionElement.element("condition");
      if (conditionElement != null) {
        conditionPresentOnTransition = true;
        condition = conditionElement.getTextTrim();
        // for backwards compatibility
        if (condition == null || condition.length() == 0) {
          condition = conditionElement.attributeValue("expression");
        }
      }
    }
    
    transition.setCondition(condition);

    // SOA-2010: conditions only valid on transitions leaving decisions
    if(conditionPresentOnTransition && ! (node instanceof Decision)) {
    
      Class nodeClass = node.getClass().getDeclaringClass();
      if( nodeClass == null) {
        nodeClass = node.getClass();
      }
      String simpleClassName = nodeClass.getName();
      simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf('.') + 1);
    
      addError("conditions on transitions used leaving a " + simpleClassName
          + ", conditions on transitions only usable leaving decisions." );
    }
    
    // register transition in origin
    node.addLeavingTransition(transition);

    // register transition in destination
    String toName = transitionElement.attributeValue("to");
    if (toName == null) {
      addWarning("node '" + node.getFullyQualifiedName()
        + "' has a transition without a 'to'-attribute");
    }
    else {
      NodeCollection parent = (NodeCollection) node.getParent();
      Node to = parent.findNode(toName);
      if (to == null) {
        addWarning("failed to resolve destination '" + toName + "' of transition '"
          + transitionName + "' leaving from " + node);
      }
      else {
        to.addArrivingTransition(transition);
      }
    }

    // read the actions
    readActions(transitionElement, transition, Event.EVENTTYPE_TRANSITION);
    readExceptionHandlers(transitionElement, transition);
    return transition;
  }

  // action references are parsed in a second pass ////////////////////////////

  public void addUnresolvedActionReference(Element actionElement, Action action) {
    unresolvedActionReferences.add(new Object[] { actionElement, action });
  }

  public void resolveActionReferences() {
    for (Iterator iter = unresolvedActionReferences.iterator(); iter.hasNext();) {
      Object[] unresolvedActionReference = (Object[]) iter.next();
      Element actionElement = (Element) unresolvedActionReference[0];
      Action refAction = processDefinition.getAction(actionElement.attributeValue("ref-name"));
      if (refAction == null) {
        addWarning("referenced action not found: " + actionElement.getPath());
      }
      else {
        Action action = (Action) unresolvedActionReference[1];
        action.setReferencedAction(refAction);
      }
    }
  }

  // verify swimlane assignments in second pass ///////////////////////////////
  public void verifySwimlaneAssignments() {
    TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
    Map swimlanes;
    if (taskMgmtDefinition != null && (swimlanes = taskMgmtDefinition.getSwimlanes()) != null) {
      Task startTask = taskMgmtDefinition.getStartTask();
      Swimlane startTaskSwimlane = startTask != null ? startTask.getSwimlane() : null;

      for (Iterator iter = swimlanes.values().iterator(); iter.hasNext();) {
        Swimlane swimlane = (Swimlane) iter.next();
        if (swimlane.getAssignmentDelegation() == null && swimlane != startTaskSwimlane) {
          addWarning("swimlane '" + swimlane.getName() + "' does not have an assignment");
        }
      }
    }
  }

  // mail delegations /////////////////////////////////////////////////////////

  private Delegation createMailDelegation(Element config) {
    // read mail class name
    String mailClassName;
    if (Configs.hasObject("jbpm.mail.class.name")) {
      mailClassName = Configs.getString("jbpm.mail.class.name");
    }
    else {
      mailClassName = Mail.class.getName();
    }

    Delegation delegation = new Delegation(mailClassName);
    delegation.setProcessDefinition(processDefinition);
    delegation.setConfiguration(writeElementContent(config));
    return delegation;
  }

  private Delegation createMailDelegation(String template) {
    Element config = DocumentHelper.createElement("config");
    config.addElement("template").setText(template);
    return createMailDelegation(config);
  }

  /** @deprecated call {@link #readMailDelegation(Element)} instead */
  public Delegation createMailDelegation(String template, String actors, String to,
    String subject, String text) {
    Element config = DocumentHelper.createElement("config");
    // template
    if (template != null) config.addElement("template").setText(template);
    // to - addresses
    if (to != null) config.addElement("to").setText(to);
    // to - actors
    if (actors != null) config.addElement("actors").setText(actors);
    // subject
    if (subject != null) config.addElement("subject").setText(subject);
    // text
    if (text != null) config.addElement("text").setText(text);

    return createMailDelegation(config);
  }

  public Delegation readMailDelegation(Element element) {
    Element config = DocumentHelper.createElement("config");
    // template
    String template = element.attributeValue("template");
    if (template != null) config.addElement("template").setText(template);
    // to - addresses
    String to = element.attributeValue("to");
    if (to != null) config.addElement("to").setText(to);
    // to - actors
    String actors = element.attributeValue("actors");
    if (actors != null) config.addElement("actors").setText(actors);
    // cc - addresses
    String cc = element.attributeValue("cc");
    if (cc != null) config.addElement("cc").setText(cc);
    // cc - actors
    String ccActors = element.attributeValue("cc-actors");
    if (ccActors != null) config.addElement("ccActors").setText(ccActors);
    // bcc - addresses
    String bcc = element.attributeValue("bcc");
    if (bcc != null) config.addElement("bcc").setText(bcc);
    // bcc - actors
    String bccActors = element.attributeValue("bcc-actors");
    if (bccActors != null) config.addElement("bccActors").setText("bcc-actors");
    // subject
    String subject = getProperty("subject", element);
    if (subject != null) config.addElement("subject").setText(subject);
    // text
    String text = getProperty("text", element);
    if (text != null) config.addElement("text").setText(text);

    return createMailDelegation(config);
  }

  public String getProperty(String property, Element element) {
    String propertyAttribute = element.attributeValue(property);
    if (propertyAttribute == null) {
      Element propertyElement = element.element(property);
      if (propertyElement != null) propertyAttribute = propertyElement.getText();
    }
    return propertyAttribute;
  }

  private XMLWriter getXmlWriter() {
    if (xmlWriter == null) {
      try {
        // configuration is saved to database, write in a compact format
        xmlWriter = new XMLWriter(OutputFormat.createCompactFormat());
      }
      catch (UnsupportedEncodingException e) {
        // WTF?
        throw new JbpmException("failed to create xml writer", e);
      }
    }
    return xmlWriter;
  }

  private String writeElement(Element element) {
    StringWriter stringWriter = new StringWriter();
    XMLWriter xmlWriter = getXmlWriter();
    xmlWriter.setWriter(stringWriter);
    try {
      xmlWriter.write(element);
    }
    catch (IOException e) {
      // cannot happen, writing to memory
      addError("failed to write mail configuration", e);
    }
    return stringWriter.toString();
  }

  public String writeElementContent(Element element) {
    StringWriter stringWriter = new StringWriter();
    XMLWriter xmlWriter = getXmlWriter();
    xmlWriter.setWriter(stringWriter);
    try {
      for (Iterator i = element.content().iterator(); i.hasNext();) {
        xmlWriter.write((org.dom4j.Node) i.next());
      }
    }
    catch (IOException e) {
      // cannot happen, writing to memory
      addError("failed to write mail configuration", e);
    }
    return stringWriter.toString().trim();
  }
}
