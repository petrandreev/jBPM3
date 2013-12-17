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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmException;
import org.jbpm.bytes.ByteArray;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.util.IoUtil;

public class FileDefinition extends ModuleDefinition {

  private static final long serialVersionUID = 1L;

  private static String getRootDir() {
    return Configs.hasObject("jbpm.files.dir") ? Configs.getString("jbpm.files.dir") : null;
  }

  private String dir;
  private Map processFiles;

  public ModuleInstance createInstance() {
    return null;
  }

  // storing files /////////////////////////////////////////////////////////////

  /**
   * add a file to this definition.
   */
  public void addFile(String name, byte[] bytes) {
    try {
      if (isStoredOnFileSystem()) {
        storeFileInFileSystem(name, bytes);
      }
      else {
        // it is stored in the database
        storeFileInDb(name, bytes);
      }
    }
    catch (IOException e) {
      throw new JbpmException("file '" + name + "' could not be stored", e);
    }
  }

  private void storeFileInFileSystem(String name, byte[] bytes) throws IOException {
    File filePath = getFilePath(name);
    log.trace("storing '" + name + "' to file '" + filePath + "'");

    FileOutputStream fos = new FileOutputStream(filePath);
    fos.write(bytes);
    fos.close();
  }

  private void storeFileInDb(String name, byte[] bytes) {
    if (processFiles == null) {
      processFiles = new HashMap();
    }
    log.trace("preparing '" + name + "' for storage in the database");
    processFiles.put(name, new ByteArray(name, bytes));
  }

  /**
   * add a file to this definition.
   */
  public void addFile(String name, InputStream is) {
    try {
      if (isStoredOnFileSystem()) {
        storeFileInFileSystem(name, is);
      }
      else {
        // it is stored in the database
        storeFileInDb(name, is);
      }
    }
    catch (IOException e) {
      throw new JbpmException("file '" + name + "' could not be stored", e);
    }
  }

  private void storeFileInFileSystem(String name, InputStream is) throws IOException {
    File filePath = getFilePath(name);
    log.trace("storing '" + name + "' to file '" + filePath + "'");

    FileOutputStream fos = new FileOutputStream(filePath);
    IoUtil.transfer(is, fos);
    fos.close();
  }

  private void storeFileInDb(String name, InputStream is) throws IOException {
    if (processFiles == null) {
      processFiles = new HashMap();
    }
    log.trace("preparing '" + name + "' for storage in the database");
    processFiles.put(name, new ByteArray(name, IoUtil.readBytes(is)));
  }

  /**
   * retrieve a file of this definition as an input stream.
   */
  public InputStream getInputStream(String name) {
    InputStream inputStream;
    if (isStoredOnFileSystem()) {
      inputStream = getInputStreamFromFileSystem(name);
    }
    else {
      // it is stored in the database
      inputStream = getInputStreamFromDb(name);
    }
    return inputStream;
  }

  public boolean hasFile(String name) {
    if (isStoredOnFileSystem()) {
      return getFilePath(name).exists();
    }
    else {
      return processFiles != null ? processFiles.containsKey(name) : false;
    }
  }

  public Map getInputStreamMap() {
    HashMap result = new HashMap();
    if (processFiles != null) {
      for (Iterator iter = processFiles.keySet().iterator(); iter.hasNext();) {
        String name = (String) iter.next();
        result.put(name, getInputStream(name));
      }
    }
    return result;
  }

  public Map getBytesMap() {
    HashMap result = new HashMap();
    if (processFiles != null) {
      for (Iterator iter = processFiles.keySet().iterator(); iter.hasNext();) {
        String name = (String) iter.next();
        result.put(name, getBytes(name));
      }
    }
    return result;
  }

  private InputStream getInputStreamFromFileSystem(String name) {
    try {
      File filePath = getFilePath(name);
      if (log.isDebugEnabled()) {
        log.debug("loading '" + name + "' from file '" + filePath + "'");
      }
      return filePath.canRead() ? new FileInputStream(filePath) : null;
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  private InputStream getInputStreamFromDb(String name) {
    if (log.isDebugEnabled()) log.debug("loading '" + name + "' from database");
    byte[] bytes = getBytesFromDb(name);
    return bytes != null ? new ByteArrayInputStream(bytes) : null;
  }

  /**
   * retrieve a file of this definition as a byte array.
   */
  public byte[] getBytes(String name) {
    byte[] bytes;
    if (isStoredOnFileSystem()) {
      bytes = getBytesFromFileSystem(name);
    }
    else {
      // it is stored in the database
      bytes = getBytesFromDb(name);
    }
    return bytes;
  }

  private byte[] getBytesFromFileSystem(String name) {
    InputStream in = getInputStreamFromFileSystem(name);
    if (in == null) return null;

    try {
      return IoUtil.readBytes(in);
    }
    catch (IOException e) {
      return null;
    }
    finally {
      try {
        in.close();
      }
      catch (IOException e) {
        // disregard exception on close
      }
    }
  }

  private byte[] getBytesFromDb(String name) {
    if (processFiles != null) {
      ByteArray byteArray = (ByteArray) processFiles.get(name);
      if (byteArray != null) return byteArray.getBytes();
    }
    return null;
  }

  private boolean isStoredOnFileSystem() {
    String rootDir = getRootDir();
    boolean isStoredOnFileSystem = (rootDir != null);
    // if files should be stored on the file system
    // and no directory has been created yet...
    if (isStoredOnFileSystem && dir == null) {
      // create a new directory
      dir = findNewDirName();
      new File(rootDir, dir).mkdirs();
    }
    return isStoredOnFileSystem;
  }

  private String findNewDirName() {
    String dirName = "files-1";

    File parentFile = new File(getRootDir());
    if (parentFile.isDirectory()) {
      // get the current contents of the directory
      String[] fileNames = parentFile.list();
      Arrays.sort(fileNames);

      // find an unused name for the directory to be created
      StringBuffer nameBuilder = new StringBuffer("files-");
      for (int seqNr = 2; Arrays.binarySearch(fileNames, dirName) >= 0; seqNr++) {
        dirName = nameBuilder.append(seqNr).toString();
        // remove appended number
        nameBuilder.setLength(6);
      }
    }

    return dirName;
  }

  private File getFilePath(String name) {
    File filePath = new File(getRootDir(), dir + File.separatorChar + name);
    filePath.getParentFile().mkdirs();
    return filePath;
  }

  private static final Log log = LogFactory.getLog(FileDefinition.class);
}
