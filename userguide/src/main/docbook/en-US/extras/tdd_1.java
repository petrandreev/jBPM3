public class AuctionTest extends TestCase {

  // parse the process definition
  static ProcessDefinition auctionProcess = 
      ProcessDefinition.parseParResource("org/jbpm/tdd/auction.par");

  // get the nodes for easy asserting
  static StartState start = auctionProcess.getStartState();
  static State auction = (State) auctionProcess.getNode("auction");
  static EndState end = (EndState) auctionProcess.getNode("end");

  // the process instance
  ProcessInstance processInstance;

  // the main path of execution
  Token token;

  public void setUp() {
    // create a new process instance for the given process definition
    processInstance = new ProcessInstance(auctionProcess);

    // the main path of execution is the root token
    token = processInstance.getRootToken();
  }
  
  public void testMainScenario() {
    // after process instance creation, the main path of 
    // execution is positioned in the start state.
    assertSame(start, token.getNode());
    
    token.signal();
    
    // after the signal, the main path of execution has 
    // moved to the auction state
    assertSame(auction, token.getNode());
    
    token.signal();
    
    // after the signal, the main path of execution has 
    // moved to the end state and the process has ended
    assertSame(end, token.getNode());
    assertTrue(processInstance.hasEnded());
  }
}