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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    jbpmSchema = new JbpmSchema(new Configuration().configure());
  }

  public void testCreateSchema() {
    jbpmSchema.createSchema();
    Set<String> existingTables = jbpmSchema.getExistingTables();
    assert existingTables.containsAll(jbpmSchema.getJbpmTables()) : existingTables;
  }

  public void testDropSchema() {
    jbpmSchema.dropSchema();
    Set<String> existingTables = jbpmSchema.getExistingTables();
    for (String jbpmTable : jbpmSchema.getJbpmTables()) {
      assert !existingTables.contains(jbpmTable) : existingTables;
    }
  }

  public void testCleanSchema() {
    jbpmSchema.cleanSchema();
    Map<String, Long> rowsPerTable = jbpmSchema.getRowsPerTable();
    Set<String> existingTables = jbpmSchema.getExistingTables();
    assert existingTables.containsAll(rowsPerTable.keySet()) : existingTables;

    for (long rowCount : rowsPerTable.values()) {
      assertEquals(0, rowCount);
    }
  }
}
