package org.jbpm.examples.taskinstance;

import org.jbpm.taskmgmt.exe.TaskInstance;

public class CustomTaskInstance extends TaskInstance {
	
	private static final long serialVersionUID = 1L;

	protected String customId = "";
	
	public String getCustomId() {
		return customId;
	}
	public void setCustomId(String customId) {
		this.customId = customId;
	}
	  
}
