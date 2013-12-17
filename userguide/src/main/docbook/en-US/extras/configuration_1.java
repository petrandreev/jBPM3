JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
try {
  // This is what we call a context block.
  // Here you can perform workflow operations

} finally {
  jbpmContext.close();
}