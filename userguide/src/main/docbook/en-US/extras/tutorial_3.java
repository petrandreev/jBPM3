// MyActionHandler represents a class that could execute 
// some user code during the execution of a jBPM process.
public class MyActionHandler implements ActionHandler {

  // Before each test (in the setUp), the isExecuted member 
  // will be set to false.
  public static boolean isExecuted = false;  

  // The action will set the isExecuted to true so the 
  // unit test will be able to show when the action
  // is being executed.
  public void execute(ExecutionContext executionContext) {
    isExecuted = true;
  }
}