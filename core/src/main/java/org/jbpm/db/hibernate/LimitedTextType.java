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

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.type.TextType;
import org.hibernate.usertype.ParameterizedType;

/**
 * Mapping between SQL {@link Types#CLOB clob} and Java {@link String} that truncates parameter
 * values to column size.
 * 
 * @author Alejandro Guizar
 */
public class LimitedTextType extends TextType implements ParameterizedType {

  private int limit;

  private static final long serialVersionUID = 1L;

  public int getLimit() {
    return limit;
  }

  public void set(PreparedStatement st, Object value, int index) throws SQLException {
    String text = (String) value;
    int length = text.length();
    if (length > limit) {
      text = text.substring(0, limit);
      length = limit;
    }

    st.setCharacterStream(index, new StringReader(text), length);
  }

  public void setParameterValues(Properties parameters) {
    limit = Integer.parseInt(parameters.getProperty("limit"));
  }

}
