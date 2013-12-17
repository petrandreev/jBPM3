public void testTaskAssignment() {
  // The process shown below is based on the hello world process.
  // The state node is replaced by a task-node.  The task-node 
  // is a node in JPDL that represents a wait state and generates 
  // task(s) to be completed before the process can continue to 
  // execute.  
  ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
    "<process-definition name='the baby process'>" +
    "  <start-state>" +
    "    <transition name='baby cries' to='t' />" +
    "  </start-state>" +
    "  <task-node name='t'>" +
    "    <task name='change nappy'>" +
    "      <assignment" +
    "       class='org.jbpm.tutorial.taskmgmt.NappyAssignmentHandler' />" +
    "    </task>" +
    "    <transition to='end' />" +
    "  </task-node>" +
    "  <end-state name='end' />" +
    "</process-definition>"
  );
  
  // Create an execution of the process definition.
  ProcessInstance processInstance = 
      new ProcessInstance(processDefinition);
  Token token = processInstance.getRootToken();
  
  // Let's start the process execution, leaving the start-state 
  // over its default transition.
  token.signal();
  // The signal method will block until the process execution 
  // enters a wait state.   In this case, that is the task-node.
  assertSame(processDefinition.getNode("t"), token.getNode());

  // When execution arrived in the task-node, a task 'change nappy'
  // was created and the NappyAssignmentHandler was called to determine
  // to whom the task should be assigned.  The NappyAssignmentHandler 
  // returned 'papa'.

  // In a real environment, the tasks would be fetched from the
  // database with the methods in the org.jbpm.db.TaskMgmtSession.
  // Since we don't want to include the persistence complexity in 
  // this example, we just take the first task-instance of this 
  // process instance (we know there is only one in this test
  // scenario).
  TaskInstance taskInstance = (TaskInstance)  
      processInstance
        .getTaskMgmtInstance()
        .getTaskInstances()
        .iterator().next();

  // Now, we check if the taskInstance was actually assigned to 'papa'.
  assertEquals("papa", taskInstance.getActorId() );
  
  // Now we suppose that 'papa' has done his duties and mark the task 
  // as done. 
  taskInstance.end();
  // Since this was the last (only) task to do, the completion of this
  // task triggered the continuation of the process instance execution.
  
  assertSame(processDefinition.getNode("end"), token.getNode());
}