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

import java.sql.Types;
import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.usertype.ParameterizedType;

/**
 * Mapping between SQL {@link Types#VARCHAR varchar} and Java {@link String} that truncates
 * parameter values to column size.
 * 
 * @author Alejandro Guizar
 * @author pan
 */
public class LimitedStringType extends AbstractSingleColumnStandardBasicType<String> implements
  DiscriminatorType<String>, ParameterizedType {

  private static final long serialVersionUID = 1L;

  private static final String TYPE_NAME = "ltdstring";

  private static final String PARAMETER_LIMIT = "limit";

  public LimitedStringType() {
    super(new LimitedVarcharTypeDescriptor(), StringTypeDescriptor.INSTANCE);
  }

  @Override
  protected boolean registerUnderJavaType() {
    return false;
  }

  public String objectToSQLString(String value, Dialect dialect) throws Exception {
    return '\'' + value + '\'';
  }

  public String stringToObject(String xml) throws Exception {
    return xml;
  }

  public String toString(String value) {
    return value;
  }

  public int getLimit() {
    return ((LimitedVarcharTypeDescriptor) getSqlTypeDescriptor()).getLimit();
  }

  public void setParameterValues(Properties parameters) {
    ((LimitedVarcharTypeDescriptor) getSqlTypeDescriptor()).setLimit(Integer.parseInt(parameters.getProperty(PARAMETER_LIMIT)));
  }

  public String getName() {
    return TYPE_NAME;
  }
}
