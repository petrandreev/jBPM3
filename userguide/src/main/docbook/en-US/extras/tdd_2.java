static ProcessDefinition auctionProcess = 
    ProcessDefinition.parseXmlString(
  "<process-definition>" + 
  "  <start-state name='start'>" + 
  "    <transition to='auction'/>" + 
  "  </start-state>" + 
  "  <state name='auction'>" + 
  "    <transition to='end'/>" + 
  "  </state>" + 
  "  <end-state name='end'/>" + 
  "</process-definition>");
