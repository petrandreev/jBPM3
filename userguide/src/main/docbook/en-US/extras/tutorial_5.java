public void testTransitionAction() {
    // The next process is a variant of the hello world process.
    // We have added an action on the transition from state 's' 
    // to the end-state.  The purpose of this test is to show 
    // how easy it is to integrate Java code in a jBPM process.
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='s' />" +
      "  </start-state>" +
      "  <state name='s'>" +
      "    <transition to='end'>" +
      "      <action class='org.jbpm.tutorial.action.MyActionHandler' />" +
      "    </transition>" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    // Let's start a new execution for the process definition.
    ProcessInstance processInstance = 
      new ProcessInstance(processDefinition);
    
    // The next signal will cause the execution to leave the start 
    // state and enter the state 's'
    processInstance.signal();

    // Here we show that MyActionHandler was not yet executed. 
    assertFalse(MyActionHandler.isExecuted);
    // ... and that the main path of execution is positioned in 
    // the state 's'
    assertSame(processDefinition.getNode("s"), 
               processInstance.getRootToken().getNode());
    
    // The next signal will trigger the execution of the root 
    // token.  The token will take the transition with the
    // action and the action will be executed during the  
    // call to the signal method.
    processInstance.signal();
    
    // Here we can see that MyActionHandler was executed during 
    // the call to the signal method.
    assertTrue(MyActionHandler.isExecuted);
  }