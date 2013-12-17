JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
try {
	String processName = ...;
	ProcessInstance processInstance = 
		jbpmContext.newProcessInstance(processName);
} finally {
	jbpmContext.close();
}