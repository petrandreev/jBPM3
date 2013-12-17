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
package org.jbpm.jpdl.convert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jbpm.jpdl.par.ProcessArchive;

public class Converter {

  private static final String STYLESHEET_NAME = "convert-pdl-2.0-to-3.0.xslt";

  File indir;
  File outdir;

  public Document convert(Document document) throws Exception {
    // load the transformer using JAXP
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer(new StreamSource(this.getClass()
      .getResourceAsStream(STYLESHEET_NAME)));

    // apply the conversion stylesheet to the incoming process definition
    DocumentSource source = new DocumentSource(document);
    DocumentResult result = new DocumentResult();
    transformer.transform(source, result);

    // return the transformed document
    return result.getDocument();
  }

  public String convertPar(ProcessArchive pa) {
    try {
      // Parse the process definition XML into a DOM document
      Document doc = DocumentHelper.parseText(new String(pa.getEntry("processdefinition.xml")));

      // Convert from 2.0 to 3.0 PDL
      Document doc30 = convert(doc);

      // Serialize the resulting document as the result
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      serializetoXML(bos, doc30);

      return bos.toString();
    }
    catch (DocumentException de) {
      log.error("Conversion had trouble parsing the 2.0 process definition", de);
    }
    catch (Exception ex) {
      log.error("Unexpected error in conversion", ex);
    }

    // things did not go well
    return null;
  }

  public void serializetoXML(OutputStream out, Document document) throws Exception {
    OutputFormat outformat = OutputFormat.createPrettyPrint();
    // outformat.setEncoding(aEncodingScheme);
    XMLWriter writer = new XMLWriter(out, outformat);
    writer.write(document);
    writer.flush();
  }

  public static void main(String[] args) throws Exception {
    Converter converter = new Converter();

    if (!converter.parse(args)) {
      System.err.println();
      System.err.println("Usage: java -jar converter.jar input-directory output-directory\n\n"
        + "input-directory is the directory where you have 2.0 process archives (*.par)\n"
        + "The converted process files will be placed in the output-directory");
      System.exit(1);
    }

    converter.convertPars();
  }

  boolean parse(String[] args) {
    if (args.length != 2) return false;

    // Check for valid input and output directories
    indir = new File(args[0]);
    if (!indir.isDirectory()) {
      System.err.println("Input file " + args[0] + " is not a valid directory name.");
      return false;
    }

    outdir = new File(args[1]);
    if (!outdir.isDirectory()) {
      System.err.println("Output file " + args[1] + " is not a valid directory name.");
      return false;
    }

    return true;
  }

  void convertPars() throws Exception {
    String[] files = indir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".par");
      }
    });

    for (int i = 0; i < files.length; i++) {
      ZipInputStream zip = new ZipInputStream(new FileInputStream(indir.getPath() + "/"
        + files[i]));
      ProcessArchive pa = new ProcessArchive(zip);
      String xml = convertPar(pa);

      // Create new process archive in designated output directory
      ZipOutputStream zippo = new ZipOutputStream(new FileOutputStream(outdir.getPath() + "/"
        + files[i]));

      // Copy all non-pdl entries and insert new pdl
      for (Iterator iter = pa.getEntries().keySet().iterator(); iter.hasNext();) {
        String name = (String) iter.next();

        zippo.putNextEntry(new ZipEntry(name));
        if ("processdefinition.xml".equalsIgnoreCase(name)) {
          zippo.write(xml.getBytes());
        }
        else {
          zippo.write(pa.getEntry(name));
        }

        zippo.closeEntry();
      }

      zippo.close();
      System.out.println("Converted " + files[i]);
    } // process next PAR
  }

  private static final Log log = LogFactory.getLog(Converter.class);
}
