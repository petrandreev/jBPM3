JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
try {
	long processInstanceId = ...;
	ProcessInstance processInstance = 
		jbpmContext.loadProcessInstance(processInstanceId);
		processInstance.signal();
		jbpmContext.save(processInstance);
} finally {
	jbpmContext.close();
}