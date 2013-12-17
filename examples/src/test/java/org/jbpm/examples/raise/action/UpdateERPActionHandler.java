package org.jbpm.examples.raise.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class UpdateERPActionHandler implements ActionHandler {

	private static final long serialVersionUID = 1L;
	Log log = LogFactory.getLog(this.getClass());

	public void execute(ExecutionContext context) throws Exception {
		log.info("***************************************************************");
		log.info("Updating ERP");
		log.info("Update complete");
		log.info("***************************************************************");
	}

}
