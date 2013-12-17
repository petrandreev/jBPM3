JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
try {
  // Invoke persistence operations here
} finally {
  jbpmContext.close();
}