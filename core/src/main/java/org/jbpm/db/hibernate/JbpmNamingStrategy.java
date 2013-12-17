/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.db.hibernate;

import org.hibernate.cfg.NamingStrategy;

public class JbpmNamingStrategy implements NamingStrategy {

  public String classToTableName(String className) {
    className = className.substring(className.lastIndexOf('.')+1);
    return "JBPM_"+className.toUpperCase();
  }

  public String propertyToColumnName(String propertyName) {
    return propertyName.toUpperCase()+"_";
  }

  public String tableName(String tableName) {
    return "JBPM_"+tableName;
  }

  public String columnName(String columnName) {
    return columnName+"_";
  }

  public String propertyToTableName(String className, String propertyName) {
    return classToTableName(className)+"_"+propertyName.toUpperCase();
  }

  public String collectionTableName(String ownerEntityTable, String associatedEntityTable, String propertyName) {
    return null;
  }

  public String joinKeyColumnName(String joinedColumn, String joinedTable) {
    return null;
  }

  public String foreignKeyColumnName(String propertyName, String propertyTableName, String referencedColumnName) {
    return null;
  }

  public String logicalColumnName(String columnName, String propertyName) {
    return null;
  }

  public String logicalCollectionTableName(String tableName, String ownerEntityTable, String associatedEntityTable, String propertyName) {
    return null;
  }

  public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
    return null;
  }

  public String collectionTableName(String arg0, String arg1, String arg2, String arg3, String arg4) {
    return null;
  }

  public String foreignKeyColumnName(String arg0, String arg1, String arg2, String arg3) {
    return null;
  }
}
