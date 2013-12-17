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
package org.jbpm.instantiation;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Factory that keeps soft references to the class loaders it produces, in order to prevent
 * duplicate class loaders from eating up the permanent generation space. The cache does not
 * prevent the garbage collector from discarding the class loaders.
 * 
 * @author Alejandro Guizar
 */
public class SharedProcessClassLoaderFactory implements ProcessClassLoaderFactory {

  private JbpmConfiguration jbpmConfiguration;
  private transient Map classLoaderRefs = new HashMap();

  private static final long serialVersionUID = 1L;

  public ClassLoader getProcessClassLoader(ProcessDefinition processDefinition) {
    // use database identifier as key to lookup cached class loader
    long id = processDefinition.getId();
    // if process definition is transient, use hash code as key
    Long key = new Long(id == 0L ? processDefinition.hashCode() : id);

    // consider that the context class loader changes among applications
    ClassLoader parentClassLoader = ClassLoaderUtil.getClassLoader();

    synchronized (classLoaderRefs) {
      // lookup cached class loader
      ClassLoader processClassLoader = getProcessClassLoader(key, parentClassLoader);
      // if class loader is not cached,
      if (processClassLoader == null) {
        // (re-)create class loader
        processClassLoader = new ProcessClassLoader(parentClassLoader, processDefinition, jbpmConfiguration);
        // add class loader to cache
        putProcessClassLoader(key, processClassLoader);
      }
      return processClassLoader;
    }
  }

  private ClassLoader getProcessClassLoader(Long processDefinitionKey,
    ClassLoader parentClassLoader) {
    List referenceList = (List) classLoaderRefs.get(processDefinitionKey);
    if (referenceList != null) {
      for (Iterator i = referenceList.iterator(); i.hasNext();) {
        SoftReference reference = (SoftReference) i.next();
        ClassLoader processClassLoader = (ClassLoader) reference.get();
        // reference may have been cleared already
        if (processClassLoader == null) {
          // remove cleared reference
          i.remove();
        }
        // process class loader may have a different parent
        else if (processClassLoader.getParent() == parentClassLoader) {
          return processClassLoader;
        }
      }
    }
    return null;
  }

  private void putProcessClassLoader(Long processDefinitionKey, ClassLoader processClassLoader) {
    List referenceList = (List) classLoaderRefs.get(processDefinitionKey);
    if (referenceList == null) {
      referenceList = new ArrayList();
      classLoaderRefs.put(processDefinitionKey, referenceList);
    }
    referenceList.add(new SoftReference(processClassLoader));
  }
}
