JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
try {
	long taskInstanceId = ...;
	TaskInstance taskInstance = 
		jbpmContext.loadTaskInstanceForUpdate(taskInstanceId);
	taskInstance.end();
	} 
finally {
		jbpmContext.close();
}