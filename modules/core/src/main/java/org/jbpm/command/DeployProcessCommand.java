package org.jbpm.command;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * Deploys a process, given as XML-String or process archive.
 * 
 * If both are given, the byte-array will be preferred The deployed process definition is returned
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 * 
 */
public class DeployProcessCommand extends AbstractGetObjectBaseCommand
{
  private static final long serialVersionUID = -5861811926680981061L;

  private String xml;

  private byte[] par;

  private static final Log log = LogFactory.getLog(DeployProcessCommand.class);

  public DeployProcessCommand()
  {
  }

  public DeployProcessCommand(byte[] par)
  {
    setPar(par);
  }

  public DeployProcessCommand(String xml)
  {
    setXml(xml);
  }

  /**
   * @return deployed ProcessDefinition
   */
  public Object execute(JbpmContext jbpmContext) throws Exception
  {
    if (par == null && xml == null)
      throw new JbpmException("either xml string or process archive must be given.");

    ProcessDefinition processDefinition;
    if (par != null)
    {
      log.debug("parse process from archive");

      // Thanks to George Mournos who helped to improve this:
      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(par));
      processDefinition = ProcessDefinition.parseParZipInputStream(zipInputStream);
    }
    else
    {
      log.debug("parse process from string");
      if (log.isTraceEnabled())
        log.trace("deploy process:\n" + xml);

      processDefinition = ProcessDefinition.parseXmlString(xml);
    }

    jbpmContext.deployProcessDefinition(processDefinition);
    log.debug("deployment sucessfull");

    return retrieveProcessDefinition(processDefinition);
  }

  public byte[] getPar()
  {
    return par;
  }

  public void setPar(byte[] par)
  {
    this.par = par;

    if (par == null || par.length == 0)
      throw new IllegalArgumentException("Cannot process null process archive");
  }

  public String getXml()
  {
    return xml;
  }

  public void setXml(String xml)
  {
    this.xml = xml;

    if (xml == null || xml.length() == 0)
      throw new IllegalArgumentException("Cannot process null process definition");
  }
  
  // methods for fluent programming

  public DeployProcessCommand xml(String xml)
  {
    setXml(xml);
    return this;
  }
  
  public DeployProcessCommand par(byte[] par)
  {
    setPar(par);
    return this;
  }

}
