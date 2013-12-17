package org.jbpm.command;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * Deploys a process, given as XML-String or process archive.
 * 
 * If both are given, the byte-array will be preferred The deployed process definition is
 * returned
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 * 
 */
public class DeployProcessCommand extends AbstractGetObjectBaseCommand {

  private static final long serialVersionUID = -5861811926680981061L;

  private String xml;

  private byte[] par;

  public DeployProcessCommand() {
  }

  public DeployProcessCommand(byte[] par) {
    setPar(par);
  }

  public DeployProcessCommand(String xml) {
    setXml(xml);
  }

  /**
   * @return deployed ProcessDefinition
   */
  public Object execute(JbpmContext jbpmContext) throws Exception {
    if (par == null && xml == null) {
      throw new JbpmException("either xml string or process archive must be given.");
    }

    ProcessDefinition processDefinition;
    boolean debug = log.isDebugEnabled();
    if (par != null) {
      if (debug) log.debug("parsing process from archive");
      // Thanks to George Mournos who helped to improve this:
      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(par));
      processDefinition = ProcessDefinition.parseParZipInputStream(zipInputStream);
    }
    else {
      if (debug) log.debug("parsing process from string");
      processDefinition = ProcessDefinition.parseXmlString(xml);
    }

    jbpmContext.deployProcessDefinition(processDefinition);
    if (debug) log.debug(processDefinition + " deployed successfully");

    return retrieveProcessDefinition(processDefinition);
  }

  public byte[] getPar() {
    return par;
  }

  public void setPar(byte[] par) {
    if (par == null) throw new IllegalArgumentException("process archive is null");
    this.par = par;
  }

  public String getXml() {
    return xml;
  }

  public void setXml(String xml) {
    if (xml == null) throw new IllegalArgumentException("xml is null");
    this.xml = xml;
  }

  // methods for fluent programming

  public DeployProcessCommand xml(String xml) {
    setXml(xml);
    return this;
  }

  public DeployProcessCommand par(byte[] par) {
    setPar(par);
    return this;
  }

}
