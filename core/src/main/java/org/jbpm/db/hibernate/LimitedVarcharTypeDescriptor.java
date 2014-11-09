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
import java.sql.SQLException;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Integration of limited string type.
 * 
 * @author pan
 */
public class LimitedVarcharTypeDescriptor extends VarcharTypeDescriptor {

  private static final long serialVersionUID = 1L;
  private int limit;

  LimitedVarcharTypeDescriptor() {

  }

  public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new BasicBinder<X>(javaTypeDescriptor, this) {
      @Override
      protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
        throws SQLException {
        String text = javaTypeDescriptor.unwrap(value, String.class, options);
        if (text.length() > limit) text = text.substring(0, limit);
        st.setString(index, text);
      }
    };
  }

  void setLimit(int limit) {
    this.limit = limit;
  }

  int getLimit() {
    return limit;
  }
}
