JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
try {
	jbpmContext.setSession(SessionFactory.getCurrentSession());

	// your jBPM operations on jbpmContext

} 
finally {
	jbpmContext.close();
}