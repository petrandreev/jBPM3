JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
try {
	// to inject resources in the jbpmContext
	//before they are used, you can use
	jbpmContext.setConnection(connection);
	// or
	jbpmContext.setSession(session);
	// or
	jbpmContext.setSessionFactory(sessionFactory);

} 
finally {
	jbpmContext.close();
}