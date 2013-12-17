/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */ 
package org.jbpm.examples.assignment;

import java.io.Serializable;

public class Role
    implements Serializable
{
    long    id;

    String  roleName;
    
	private static final long serialVersionUID = 1L;

 
    public Role() {
    	
    }
    
    public Role(String roleName) {
    	setRoleName(roleName);
    }
    public long getId() {
        return id;
    }                    
    public void setId(long id) {
        this.id = id;
    }     
    public String getRoleName() {
        return roleName;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }


}
