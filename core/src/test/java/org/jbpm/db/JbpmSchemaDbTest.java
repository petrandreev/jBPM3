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
package org.jbpm.db;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.Configuration;

import org.jbpm.AbstractJbpmTestCase;

/**
 * Test the JbpmSchema utility
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Nov-2008
 */
public class JbpmSchemaDbTest extends AbstractJbpmTestCase {

  JbpmSchema jbpmSchema;

  protected void setUp() throws Exception {
    super.setUp();
    jbpmSchema = new JbpmSchema(new Configuration().configure());
  }

  public void testCreateSchema() {
    jbpmSchema.createSchema();
    Set existingTables = jbpmSchema.getExistingTables();
    assert existingTables.containsAll(jbpmSchema.getJbpmTables()) : existingTables;
  }

  public void testDropSchema() {
    jbpmSchema.dropSchema();
    Set existingTables = jbpmSchema.getExistingTables();
    for (Iterator i = jbpmSchema.getJbpmTables().iterator(); i.hasNext();) {
      String jbpmTable = (String) i.next();
      assert !existingTables.contains(jbpmTable) : jbpmTable;
    }
  }

  public void testCleanSchema() {
    jbpmSchema.cleanSchema();
    Map rowsPerTable = jbpmSchema.getRowsPerTable();
    Set existingTables = jbpmSchema.getExistingTables();
    assert existingTables.containsAll(rowsPerTable.keySet()) : existingTables;

    for (Iterator i = rowsPerTable.values().iterator(); i.hasNext();) {
      Long rowCount = (Long) i.next();
      assertEquals(0, rowCount.longValue());
    }
  }
}
