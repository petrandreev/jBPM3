package org.jbpm.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Node;

/**
 * An iterator over the children of a DOM node.
 * 
 * @author Alejandro Guizar
 */
class NodeIterator implements Iterator {

  private Node currentNode;
  private Node lastReturned;

  /**
   * Creates an iterator over the children of the given node.
   * 
   * @param parentNode the node to iterate
   */
  public NodeIterator(Node parentNode) {
    currentNode = parentNode.getFirstChild();
  }

  public boolean hasNext() {
    return currentNode != null;
  }

  public Object next() {
    if (currentNode == null) throw new NoSuchElementException();
    lastReturned = currentNode;
    currentNode = lastReturned.getNextSibling();
    return lastReturned;
  }

  public void remove() {
    if (lastReturned == null) throw new IllegalStateException();
    Node parentNode = lastReturned.getParentNode();
    if (parentNode != null) parentNode.removeChild(lastReturned);
    lastReturned = null;
  }
}