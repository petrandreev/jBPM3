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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.ImmutableType;
import org.jbpm.context.def.Access;

public class AccessType extends ImmutableType implements DiscriminatorType {

  private static final long serialVersionUID = 1L;

  public Object get(ResultSet rs, String name) throws SQLException {
    return new Access(rs.getString(name));
  }

  public Class getReturnedClass() {
    return Access.class;
  }

  public void set(PreparedStatement st, Object value, int index) throws SQLException {
    st.setString(index, ((Access) value).toString());
  }

  public int sqlType() {
    return Types.VARCHAR;
  }

  public String getName() {
    return "access";
  }

  public String objectToSQLString(Object value, Dialect dialect) throws Exception {
    return '\'' + value.toString() + '\'';
  }

  public Object stringToObject(String xml) throws Exception {
    return xml;
  }

  public String toString(Object value) {
    return value != null ? value.toString() : "null";
  }

  public Object fromStringValue(String xml) {
    return xml;
  }
}