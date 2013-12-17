// This example also starts from the hello world process.
// This time even without modification.
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

ProcessInstance processInstance =
  new ProcessInstance(processDefinition);

// Fetch the context instance from the process instance 
// for working with the process variables.
ContextInstance contextInstance = 
  processInstance.getContextInstance();

// Before the process has left the start-state, 
// we are going to set some process variables in the 
// context of the process instance.
contextInstance.setVariable("amount", new Integer(500));
contextInstance.setVariable("reason", "i met my deadline");

// From now on, these variables are associated with the 
// process instance.  The process variables are now accessible 
// by user code via the API shown here, but also in the actions 
// and node implementations.  The process variables are also  
// stored into the database as a part of the process instance.

processInstance.signal();

// The variables are accessible via the contextInstance. 

assertEquals(new Integer(500), 
             contextInstance.getVariable("amount"));
assertEquals("i met my deadline", 
             contextInstance.getVariable("reason"));