package org.jbpm.jpdl.xml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class SchemaTest extends AbstractJbpmTestCase
{

  private static Log log = LogFactory.getLog(SchemaTest.class);

  public static class TestEntityResolver implements EntityResolver
  {
    Map schemas;

    public TestEntityResolver(Map schemas)
    {
      this.schemas = schemas;
    }

    public InputSource resolveEntity(String publicId, String systemId)
    {
      String schemaXmlString = (String)schemas.get(systemId);
      if (schemaXmlString == null)
      {
        return null;
      }
      return new InputSource(new StringReader(schemaXmlString));
    }
  }

  public static class TestErrorHandler implements ErrorHandler
  {
    List errors = new ArrayList();

    public void error(SAXParseException exception) throws SAXException
    {
      errors.add("[" + exception.getLineNumber() + "] " + exception.getMessage());
    }

    public void fatalError(SAXParseException exception) throws SAXException
    {
      error(exception);
    }

    public void warning(SAXParseException exception) throws SAXException
    {
      error(exception);
    }
  }

  DocumentBuilderFactory dbf;
  DocumentBuilder db;
  Map schemas;
  TestEntityResolver testEntityResolver;

  protected void setUp() throws Exception
  {
    super.setUp();
    
    dbf = DocumentBuilderFactory.newInstance();
    dbf.setValidating(true);
    dbf.setNamespaceAware(true);
    dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

    schemas = new HashMap();
    testEntityResolver = new TestEntityResolver(schemas);
  }

  public void addSchema(String schemaXmlString)
  {
    String systemId = "urn:schema" + schemas.size();
    schemas.put(systemId, schemaXmlString);

    String[] schemaSources = new String[schemas.size()];
    for (int i = 0; i < schemas.size(); i++)
    {
      schemaSources[i] = "urn:schema" + i;
    }

    dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", schemaSources);
  }

  public Document assertValidXml(String documentXmlString)
  {
    try
    {
      db = dbf.newDocumentBuilder();
      db.setEntityResolver(testEntityResolver);

      TestErrorHandler errorHandler = new TestErrorHandler();
      db.setErrorHandler(errorHandler);
      Document document = db.parse(new InputSource(new StringReader(documentXmlString)));
      if (!errorHandler.errors.isEmpty())
      {
        StringBuffer buffer = new StringBuffer();
        buffer.append("parsing problems: \n");
        Iterator iter = errorHandler.errors.iterator();
        while (iter.hasNext())
        {
          buffer.append("ERR: ");
          buffer.append(iter.next());
        }
        throw new XmlException(buffer.toString());
      }

      return document;
    }
    catch (XmlException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new XmlException("couldn't parse", e);
    }
  }

  public static void printDocument(Document document)
  {
    log.debug(XmlUtil.toString(document.getDocumentElement()));
  }

  public static class XmlException extends RuntimeException
  {
    private static final long serialVersionUID = 1L;

    public XmlException(String message)
    {
      super(message);
    }

    public XmlException(String message, Throwable cause)
    {
      super(message, cause);
    }
  }

  /*
   * 
   * public void testSchemaChoices() { addSchema( "<xs:schema targetNamespace='urn:jbpm.org:test' \n" + "           xmlns='urn:jbpm.org:test' \n" +
   * "           xmlns:xs='http://www.w3.org/2001/XMLSchema'\n" + "           elementFormDefault='qualified'>\n" + "  <xs:element name='root'>\n" +
   * "    <xs:complexType>\n" + "      <xs:choice minOccurs='0' maxOccurs='3'>\n" + "        <xs:element name='hello' />\n" + "        <xs:element name='world' />\n" +
   * "      </xs:choice>\n" + "    </xs:complexType>\n" + "  </xs:element>\n" + "</xs:schema>" );
   * 
   * assertValidXml( "<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:test'>\n" + "  <hello />\n" + "</root>" );
   * 
   * assertValidXml( "<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:test'>\n" + "  <hello />\n" + "  <world />\n" + "  <hello />\n" + "</root>" );
   * 
   * try { assertValidXml( "<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:test'>\n" + "  <hello />\n" + "  <world />\n" + "  <hello />\n" + "  <hello />\n" +
   * "</root>" ); fail("expected exception"); } catch (XmlException e) { // OK } }
   * 
   * public void testSchemaSequenceOfChoice() { addSchema( "<xs:schema targetNamespace='urn:jbpm.org:test' \n" + "           xmlns='urn:jbpm.org:test' \n" +
   * "           xmlns:xs='http://www.w3.org/2001/XMLSchema'\n" + "           elementFormDefault='qualified'>\n" + "  <xs:element name='root'>\n" +
   * "    <xs:complexType>\n" + "      <xs:sequence maxOccurs='3'>\n" + "        <xs:choice>\n" + "          <xs:element name='hello' />\n" +
   * "          <xs:element name='world' />\n" + "        </xs:choice>\n" + "      </xs:sequence>\n" + "    </xs:complexType>\n" + "  </xs:element>\n" + "</xs:schema>"
   * );
   * 
   * assertValidXml( "<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:test'>\n" + "  <hello />\n" + "</root>" );
   * 
   * assertValidXml( "<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:test'>\n" + "  <hello />\n" + "  <world />\n" + "  <hello />\n" + "</root>" );
   * 
   * assertValidXml( "<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:test'>\n" + "  <hello />\n" + "  <world />\n" + "  <hello />\n" + "</root>" );
   * 
   * try { assertValidXml( "<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:test'>\n" + "  <hello />\n" + "  <world />\n" + "  <hello />\n" + "  <hello />\n" +
   * "</root>" ); fail("expected exception"); } catch (XmlException e) { // OK } }
   */

  public void testMultiSchemas()
  {
    addSchema("<xs:schema targetNamespace='urn:jbpm.org:default' \n" + "           xmlns='urn:jbpm.org:default' \n"
        + "           xmlns:xs='http://www.w3.org/2001/XMLSchema'\n" + "           elementFormDefault='qualified'>\n" + "  <xs:element name='root'>\n"
        + "    <xs:complexType>\n" + "      <xs:choice minOccurs='0' maxOccurs='3'>\n" + "        <xs:element name='hello' />\n"
        + "        <xs:element name='world' />\n" + "        <xs:any processContents='lax' minOccurs='0' maxOccurs='unbounded'/>\n"
        + "        <xs:any namespace='##other' processContents='lax' minOccurs='0' maxOccurs='unbounded'/>\n" + "      </xs:choice>\n" + "    </xs:complexType>\n"
        + "  </xs:element>\n" + "  <xs:element name='tuut' />\n" + "</xs:schema>");

    addSchema("<xs:schema targetNamespace='urn:jbpm.org:other' \n" + "           xmlns='urn:jbpm.org:other' \n"
        + "           xmlns:xs='http://www.w3.org/2001/XMLSchema'\n" + "           elementFormDefault='qualified'>\n" + "  <xs:element name='tree'>\n"
        + "    <xs:complexType>\n" + "      <xs:choice minOccurs='0' maxOccurs='3'>\n" + "        <xs:element name='tree' />\n"
        + "        <xs:element name='leaf' />\n" + "      </xs:choice>\n" + "    </xs:complexType>\n" + "  </xs:element>\n" + "  <xs:element name='leaf' />\n"
        + "</xs:schema>");

    assertValidXml("<?xml version='1.0'?>\n" + "<root xmlns='urn:jbpm.org:default'" + "      xmlns:other='urn:jbpm.org:other'>\n" + "  <hello />\n" + "  <hello />\n"
        + "  <tuut />" + "  <other:tree />" + "</root>");
  }

}
