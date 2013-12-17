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
package org.jbpm.file.def;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.bytes.ByteArray;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.util.IoUtil;

public class FileDefinition extends ModuleDefinition {

  private static final long serialVersionUID = 1L;
  
  static String getRootDir() {
    String rootDir = null;
    if (JbpmConfiguration.Configs.hasObject("jbpm.files.dir")) {
      rootDir = JbpmConfiguration.Configs.getString("jbpm.files.dir");
    }
    return rootDir;
  }

  String dir = null;

  Map processFiles = null;

  public FileDefinition() {
  }

  public ModuleInstance createInstance() {
    return null;
  }

  // storing files /////////////////////////////////////////////////////////////

  /**
   * add a file to this definition.
   */
  public void addFile(String name, byte[] bytes) {
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    addFile(name, bais);
  }

  /**
   * add a file to this definition.
   */
  public void addFile(String name, InputStream is) {
    try {
      if (isStoredOnFileSystem()) {
        storeFileOnFileSystem(name, is);

      } else { // its stored in the database
        storeFileInDb(name, is);
      }
    } catch (Exception e) {
      throw new JbpmException("file '" + name + "' could not be stored", e);
    }
  }

  void storeFileOnFileSystem(String name, InputStream is) throws FileNotFoundException, IOException {
    String fileName = getFilePath(name);
    log.trace("storing file '" + name + "' on file system to '" + fileName + "'");
    FileOutputStream fos = new FileOutputStream(fileName);
    IoUtil.transfer(is, fos);
    fos.close();
  }

  void storeFileInDb(String name, InputStream is) throws IOException {
    if (processFiles == null) {
      processFiles = new HashMap();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    log.trace("preparing file '" + name + "' for storage in the database");
    IoUtil.transfer(is, baos);
    processFiles.put(name, new ByteArray(name, baos.toByteArray()));
  }

  // retrieving files //////////////////////////////////////////////////////////

  /**
   * retrieve a file of this definition as an inputstream.
   */
  public InputStream getInputStream(String name) {
    InputStream inputStream = null;
    try {
      if (isStoredOnFileSystem()) {
        inputStream = getInputStreamFromFileSystem(name);
      } else { // its stored in the database
        inputStream = getInputStreamFromDb(name);
      }
    } catch (Exception e) {
      throw new JbpmException("couldn't get inputstream for file '" + name + "'", e);
    }
    return inputStream;
  }

  public boolean hasFile(String name) {
    if (isStoredOnFileSystem()) {
      return new File(getFilePath(name)).exists();
    } else {
      return processFiles == null ? false : processFiles.containsKey(name);
    }
  }

  public Map getInputStreamMap() {
    HashMap result = new HashMap();
    if (processFiles != null) {
      Iterator iterator = processFiles.keySet().iterator();
      while (iterator.hasNext()) {
        String name = (String) iterator.next();
        result.put(name, getInputStream(name));
      }
    }
    return result;
  }

  public Map getBytesMap() {
    HashMap result = new HashMap();
    if (processFiles != null) {
      Iterator iterator = processFiles.keySet().iterator();
      while (iterator.hasNext()) {
        String name = (String) iterator.next();
        result.put(name, getBytes(name));
      }
    }
    return result;
  }

  private InputStream getInputStreamFromFileSystem(String name) throws FileNotFoundException {
    InputStream inputStream = null;
    String fileName = getFilePath(name);
    log.trace("loading file '" + name + "' from file system '" + fileName + "'");
    inputStream = new FileInputStream(fileName);
    return inputStream;
  }

  private InputStream getInputStreamFromDb(String name) {
    InputStream inputStream = null;
    log.trace("loading file '" + name + "' from database");
    ByteArray byteArray = getByteArray(name);
    if (byteArray != null) {
    	inputStream = new ByteArrayInputStream(byteArray.getBytes());
    }
    return inputStream;
  }

  /**
   * retrieve a file of this definition as a byte array.
   */
  public byte[] getBytes(String name) {
    byte[] bytes = null;
    try {
      if (isStoredOnFileSystem()) {
        bytes = getBytesFromFileSystem(name);
      } else { // its stored in the database
        bytes = getBytesFromDb(name);
      }
    } catch (Exception e) {
      throw new JbpmException("couldn't get value for file '" + name + "'", e);
    }
    return bytes;
  }

  byte[] getBytesFromFileSystem(String name) throws IOException {
    byte[] bytes = null;
    InputStream in = getInputStreamFromFileSystem(name);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IoUtil.transfer(in, out);
    bytes = out.toByteArray();
    return bytes;
  }

  byte[] getBytesFromDb(String name) {
    byte[] bytes;
    ByteArray byteArray = getByteArray(name);
    bytes = byteArray.getBytes();
    return bytes;
  }

  ByteArray getByteArray(String name) {
    return (ByteArray) (processFiles != null ? processFiles.get(name) : null);
  }

  boolean isStoredOnFileSystem() {
    String rootDir = getRootDir();
    boolean isStoredOnFileSystem = (rootDir != null);
    // if files should be stored on the file system and no directory has been
    // created yet...
    if ((isStoredOnFileSystem) && (dir == null)) {
      // create a new directory
      dir = findNewDirName();
      new File(rootDir + "/" + dir).mkdirs();
    }
    return isStoredOnFileSystem;
  }

  String findNewDirName() {
    String newDirName = "files-1";

    File parentFile = new File(getRootDir());
    if (parentFile.exists()) {
      // get the current contents of the directory
      String[] children = parentFile.list();
      List fileNames = new ArrayList();
      if (children != null) {
        fileNames = new ArrayList(Arrays.asList(children));
      }

      // find an unused name for the directory to be created
      int seqNr = 1;
      while (fileNames.contains(newDirName)) {
        seqNr++;
        newDirName = "files-" + seqNr;
      }
    }

    return newDirName;
  }

  String getFilePath(String name) {
    String filePath = getRootDir() + "/" + dir + "/" + name;
    new File(filePath).getParentFile().mkdirs();
    return filePath;
  }
  
  private static final Log log = LogFactory.getLog(FileDefinition.class);
}
