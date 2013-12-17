package org.jbpm.sim.datasource;

import org.jbpm.command.CommandService;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Default implementation of <code>ProcessVariableSource</code> which gets
 * process variables from historical log data.
 * 
 * <b>NOT YET IMPLEMENTED</b>
 * TODO: implement
 * @author bernd.ruecker@camunda.com
 */
public class HistorialProcessVariableSource implements ProcessDataSource {

  /**
   * a CommandService reference is needed to query historical data
   */
  private CommandService commandService;
  
  public HistorialProcessVariableSource() {
  }
  
  public HistorialProcessVariableSource(CommandService commandService) {
    this.commandService = commandService;
  }

  public void reset() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  public CommandService getCommandService() {
    return commandService;
  }

  public void setCommandService(CommandService commandService) {
    this.commandService = commandService;
  }

  public boolean hasNext() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  public void addNextData(ExecutionContext ctx) {
    throw new UnsupportedOperationException("not yet implemented");
  }

}
