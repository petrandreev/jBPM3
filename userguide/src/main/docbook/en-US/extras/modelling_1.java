public class RemoveEmployeeUpdate implements ActionHandler {
  public void execute(ExecutionContext ctx) throws Exception {
    // get the fired employee from the process variables.
    String firedEmployee =
      (String) ctx.getContextInstance().getVariable("fired employee");
    
    // by taking the same database connection as used for the jbpm
    // updates, we reuse the jbpm transaction for our database update.
    Connection connection =
    ctx.getProcessInstance().getJbpmSession().getSession().getConnection();
    Statement statement = connection.createStatement();
    statement.execute("DELETE FROM EMPLOYEE WHERE ...");
    statement.execute(); 
    statement.close();
  }
}