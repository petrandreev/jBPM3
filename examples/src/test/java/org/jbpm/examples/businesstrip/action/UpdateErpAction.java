package org.jbpm.examples.businesstrip.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class UpdateErpAction implements ActionHandler {

	Log log = LogFactory.getLog(this.getClass());
	private static final long serialVersionUID = 1L;

	public void execute(ExecutionContext context) throws Exception {
		Integer budget = (Integer) context.getVariable("budget");
		log.info("Updating ERP for business trip");
		log.info("Allocating budget: " + budget);
		context.leaveNode();
	}

}
