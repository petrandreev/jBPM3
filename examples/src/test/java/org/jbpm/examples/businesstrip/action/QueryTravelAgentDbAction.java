package org.jbpm.examples.businesstrip.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class QueryTravelAgentDbAction implements ActionHandler {

	Log log = LogFactory.getLog(this.getClass());
	private static final long serialVersionUID = 1L;

	public void execute(ExecutionContext context) throws Exception {
		String country = String.valueOf(context.getVariable("country"));
		String city = String.valueOf(context.getVariable("city"));
		log.info("Query ticket for: " + country + ", " + city);
	}

}
