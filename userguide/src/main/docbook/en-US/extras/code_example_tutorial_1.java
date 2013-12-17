public void testHelloWorldProcess() {
  // This method shows a process definition and one execution
  // of the process definition.  The process definition has
  // 3 nodes: an unnamed start-state, a state 's' and an
  // end-state named 'end'.
  // The next line parses a piece of xml text into a
  // ProcessDefinition.  A ProcessDefinition is the formal
  // description of a process represented as a java object.
  ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='s' />" +
      "  </start-state>" +
      "  <state name='s'>" +
      "    <transition to='end' />" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
  );

    // The next line creates one execution of the process definition.
    // After construction, the process execution has one main path
    // of execution (=the root token) that is positioned in the
    // start-state.
    ProcessInstance processInstance =
      new ProcessInstance(processDefinition);

    // After construction, the process execution has one main path
    // of execution (=the root token).
    Token token = processInstance.getRootToken();

    // Also after construction, the main path of execution is positioned
    // in the start-state of the process definition.
    assertSame(processDefinition.getStartState(), token.getNode());

    // Let's start the process execution, leaving the start-state
    // over its default transition.
    token.signal();
    // The signal method will block until the process execution
    // enters a wait state.

    // The process execution will have entered the first wait state
    // in state 's'. So the main path of execution is now
    // positioned in state 's'
    assertSame(processDefinition.getNode("s"), token.getNode());

    // Let's send another signal.  This will resume execution by
    // leaving the state 's' over its default transition.
    token.signal();
    // Now the signal method returned because the process instance
    // has arrived in the end-state.

    assertSame(processDefinition.getNode("end"), token.getNode());
}