/*
 * Copyright (c) 2011, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package org.cleartk.syntax.constituent.type;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.JCasUtil;

/**
 * <br>
 * Copyright (c) 2011, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Steven Bethard
 */
public class TreebankNodeUtil {

  /**
   * Selects a single TreebankNode leaf that has the same span as the given annotation.
   * 
   * @param jCas
   *          The JCas containing the TreebankNodes.
   * @param annotation
   *          The Annotation whose span should match a TreebankNode leaf.
   * @return The single TreebankNode leaf that matches the annotation, or null if no such annotation
   *         exists.
   */
  public static TreebankNode selectMatchingLeaf(JCas jCas, Annotation annotation) {
    TreebankNode leaf = null;
    for (TreebankNode node : JCasUtil.selectCovered(jCas, TreebankNode.class, annotation)) {
      if (node.getLeaf() && node.getBegin() == annotation.getBegin()
          && node.getEnd() == annotation.getEnd()) {
        if (leaf == null) {
          leaf = node;
        } else {
          throw new IllegalArgumentException(String.format(
              "expected one leaf matching annotation %s, found %s",
              annotation,
              Arrays.asList(leaf, node)));
        }
      }
    }
    return leaf;
  }

  /**
   * Selects the highest TreebankNode in the parse tree that has the same span as the given
   * annotation.
   * 
   * @param jCas
   *          The JCas containing the TreebankNodes.
   * @param annotation
   *          The Annotation whose span should be matched.
   * @return The highest TreebankNode matching the given span, or null if no such annotation exists.
   */
  public static TreebankNode selectHighestMatchingTreebankNode(JCas jCas, Annotation annotation) {
    TreebankNode highestNode = null;
    int smallestDepth = Integer.MAX_VALUE;
    for (TreebankNode node : JCasUtil.selectCovered(jCas, TreebankNode.class, annotation)) {
      if (node.getBegin() == annotation.getBegin() && node.getEnd() == annotation.getEnd()) {
        int depth = getDepth(node);
        if (depth < smallestDepth) {
          highestNode = node;
          smallestDepth = depth;
        }
      }
    }
    return highestNode;
  }

  /**
   * Selects the highest TreebankNode in the parse tree that is at least partially covered by the
   * given annotation.
   * 
   * @param jCas
   *          The JCas containing the TreebankNodes.
   * @param annotation
   *          The Annotation whose span should be matched.
   * @return The highest TreebankNode at least partially covered by the given span, or null if no
   *         such annotation exists.
   */
  public static TreebankNode selectHighestCoveredTreebankNode(JCas jCas, Annotation annotation) {
    TreebankNode highestNode = null;
    int smallestDepth = Integer.MAX_VALUE;
    for (TreebankNode node : JCasUtil.selectCovered(jCas, TreebankNode.class, annotation)) {
      if (annotation.getBegin() <= node.getBegin() && node.getEnd() <= annotation.getEnd()) {
        int depth = getDepth(node);
        if (depth < smallestDepth) {
          highestNode = node;
          smallestDepth = depth;
        }
      }
    }
    return highestNode;
  }

  /**
   * Calculates the depth of the TreebankNode. The root node has depth 0, children of the root node
   * have depth 1, etc.
   * 
   * @param node
   *          The TreebankNode whose depth is to be calculated.
   * @return The depth of the TreebankNode.
   */
  public static int getDepth(TreebankNode node) {
    int depth = -1;
    while (node != null) {
      depth += 1;
      node = node.getParent();
    }
    return depth;
  }

  /**
   * Find the path from a TreebankNode to the root of the tree it belongs to.
   * 
   * @param startNode
   *          The start node of the path
   * 
   * @return A list of TreebankNodes that make up the path from <b>startNode</b> to the root of the
   *         tree
   */
  public static List<TreebankNode> getPathToRoot(TreebankNode startNode) {
    List<TreebankNode> nlist = new ArrayList<TreebankNode>(20);
    TreebankNode cursorNode = startNode;

    while (cursorNode != null) {
      nlist.add(cursorNode);
      cursorNode = cursorNode.getParent();
    }

    return nlist;
  }

  /**
   * Representation of a path from one TreebankNode to another, via a common ancestor in the tree.
   */
  public static class TreebankNodePath {
    private List<TreebankNode> sourceToAncestor;

    private TreebankNode commonAncestor;

    private List<TreebankNode> targetToAncestor;

    public TreebankNodePath(
        TreebankNode commonAncestor,
        List<TreebankNode> sourceToAncestor,
        List<TreebankNode> targetToAncestor) {
      this.commonAncestor = commonAncestor;
      this.sourceToAncestor = sourceToAncestor;
      this.targetToAncestor = targetToAncestor;
    }

    public TreebankNode getCommonAncestor() {
      return this.commonAncestor;
    }

    public List<TreebankNode> getSourceToAncestorPath() {
      return this.sourceToAncestor;
    }

    public List<TreebankNode> getTargetToAncestorPath() {
      return this.targetToAncestor;
    }
  }

  /**
   * Get the path from the source TreebankNode to the target TreebankNode via the least common
   * ancestor.
   * 
   * @param source
   *          The TreebankNode where the path should start.
   * @param target
   *          The TreebankNode where the path should end.
   * @return The path from the source node to the target node.
   */
  public static TreebankNodePath getPath(TreebankNode source, TreebankNode target) {
    List<TreebankNode> sourceToRoot = getPathToRoot(source);
    List<TreebankNode> targetToRoot = getPathToRoot(target);

    TreebankNode ancestor = null;
    while (sourceToRoot.size() > 0 && targetToRoot.size() > 0
        && sourceToRoot.get(sourceToRoot.size() - 1) == targetToRoot.get(targetToRoot.size() - 1)) {
      ancestor = sourceToRoot.remove(sourceToRoot.size() - 1);
      ancestor = targetToRoot.remove(targetToRoot.size() - 1);
    }

    return new TreebankNodePath(ancestor, sourceToRoot, targetToRoot);
  }

  /**
   * Format the TreebankNode as a Penn-Treebank-style parenthesized string.
   * 
   * @param node
   *          The TreebankNode to be formatted.
   * @return A parenthesized Penn-Treebank-style string.
   */
  public static String toTreebankString(TreebankNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append('(').append(node.getNodeType());
    if (node.getLeaf()) {
      builder.append(' ').append(node.getCoveredText());
    } else {
      for (TreebankNode child : JCasUtil.select(node.getChildren(), TreebankNode.class)) {
        builder.append(' ').append(toTreebankString(child));
      }
    }
    builder.append(')');
    return builder.toString();
  }

  public static TreebankNode getParent(TreebankNode node) {
    if (node != null) {
      node = node.getParent();
    }
    return node;
  }

  public static TreebankNode getAncestorWithType(TreebankNode node, String type) {
    while (node != null && !node.getNodeType().equals(type)) {
      node = node.getParent();
    }
    return node;
  }

  /**
   * Create a leaf TreebankNode in a JCas.
   * 
   * @param jCas
   *          The JCas which the annotation should be added to.
   * @param begin
   *          The begin offset of the node.
   * @param end
   *          The end offset of the node.
   * @param nodeType
   *          The part of speech tag of the node.
   * @return The TreebankNode which was added to the JCas.
   */
  public static TreebankNode newNode(JCas jCas, int begin, int end, String nodeType) {
    TreebankNode node = new TreebankNode(jCas, begin, end);
    node.setNodeType(nodeType);
    node.setChildren(new FSArray(jCas, 0));
    node.setLeaf(true);
    node.addToIndexes();
    return node;
  }

  /**
   * Create a branch TreebankNode in a JCas. The offsets of this node will be determined by its
   * children.
   * 
   * @param jCas
   *          The JCas which the annotation should be added to.
   * @param nodeType
   *          The phrase type tag of the node.
   * @param children
   *          The TreebankNode children of the node.
   * @return The TreebankNode which was added to the JCas.
   */
  public static TreebankNode newNode(JCas jCas, String nodeType, TreebankNode... children) {
    int begin = children[0].getBegin();
    int end = children[children.length - 1].getEnd();
    TreebankNode node = new TreebankNode(jCas, begin, end);
    node.setNodeType(nodeType);
    node.addToIndexes();
    FSArray fsArray = new FSArray(jCas, children.length);
    fsArray.copyFromArray(children, 0, 0, children.length);
    node.setChildren(fsArray);
    for (TreebankNode child : children) {
      child.setParent(node);
    }
    return node;
  }

  public static TopTreebankNode getTopNode(TreebankNode node) {
    if (node instanceof TopTreebankNode)
      return (TopTreebankNode) node;

    TreebankNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof TopTreebankNode)
        return (TopTreebankNode) parent;
      node = parent;
      parent = node.getParent();
    }
    return null;
  }

  /**
   * A "pretty print" of this node that may be useful for e.g. debugging.
   */
  public static void print(PrintStream out, TreebankNode node) {
    out.println(print(node, 0));
  }

  private static String print(TreebankNode node, int tabs) {
    StringBuffer returnValue = new StringBuffer();
    String tabString = getTabs(tabs);
    returnValue.append(tabString + node.getNodeType());
    if (node.getNodeValue() != null)
      returnValue.append(":" + node.getNodeValue() + "\n");
    else {
      returnValue.append(":" + node.getCoveredText() + "\n");
    }
    if (node.getChildren().size() > 0) {
      Collection<TreebankNode> children = JCasUtil.select(node.getChildren(), TreebankNode.class);
      for (TreebankNode child : children) {
        returnValue.append(print(child, (tabs + 1)));
      }
    }
    return returnValue.toString();
  }

  private static String getTabs(int tabs) {
    char[] chars = new char[tabs];
    Arrays.fill(chars, ' ');
    return new String(chars);
  }
}
