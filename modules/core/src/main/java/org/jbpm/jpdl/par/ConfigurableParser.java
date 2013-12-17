package org.jbpm.jpdl.par;

import org.w3c.dom.Element;

public interface ConfigurableParser extends ProcessArchiveParser {

  void configure(Element parserElement);
}
