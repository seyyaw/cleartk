/*
 * Copyright (c) 2010, Regents of the University of Colorado 
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
package org.cleartk.maltparser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.syntax.dependency.type.DependencyNode;
import org.cleartk.syntax.dependency.type.DependencyRelation;
import org.cleartk.syntax.dependency.type.TopDependencyNode;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;
import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;

/**
 * <br>
 * Copyright (c) 2010, Regents of the University of Colorado <br>
 * All rights reserved.
 */
public class MaltParser extends JCasAnnotator_ImplBase {

  public static final String ENGMALT_RESOURCE_NAME = "/models/engmalt.linear-1.7.mco";

  public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
    // get the resource path and strip the ".mco" suffix
    String fileName = MaltParser.class.getResource(ENGMALT_RESOURCE_NAME).getFile();
    String fileBase = fileName.substring(0, fileName.length() - 4);
    return getDescription(fileBase);
  }

  public static AnalysisEngineDescription getDescription(String modelFileName)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        MaltParser.class,
        PARAM_MODEL_FILE_NAME,
        modelFileName);
  }

  @ConfigurationParameter(
      name = PARAM_MODEL_FILE_NAME,
      description = "The path to the model file, without the .mco suffix.",
      mandatory = true)
  private String modelFileName;

  public static final String PARAM_MODEL_FILE_NAME = "modelFileName";

  private MaltParserService service;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    try {
      OptionManager.instance().loadOptionDescriptionFile();
      OptionManager.instance().getOptionDescriptions().generateMaps();
      this.service = new MaltParserService();
      File modelFile = new File(this.modelFileName);
      String fileName = modelFile.getName();
      String workingDirectory = modelFile.getParent();
      String command = String.format("-c %s -m parse -w %s", fileName, workingDirectory);
      this.service.initializeParserModel(command);
    } catch (MaltChainedException e) {
      throw new ResourceInitializationException(e);
    }
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    super.collectionProcessComplete();
    try {
      this.service.terminateParserModel();
    } catch (MaltChainedException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
      List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);

      // convert tokens into MaltParser input array
      List<String> inputStrings = new ArrayList<String>();
      int lineNo = -1;
      for (Token token : tokens) {
        lineNo += 1;
        String text = token.getCoveredText();
        String pos = token.getPos();

        // line format is <index>\t<word>\t_\t<pos>\t<pos>\t_
        String lineFormat = "%1$d\t%2$s\t_\t%3$s\t%3$s\t_";
        inputStrings.add(String.format(lineFormat, lineNo + 1, text, pos));
      }

      try {
        // parse with MaltParser
        String[] input = inputStrings.toArray(new String[inputStrings.size()]);
        DependencyStructure graph = this.service.parse(input);

        // convert MaltParser structure into annotations
        Map<Integer, DependencyNode> nodes = new HashMap<Integer, DependencyNode>();
        SortedSet<Integer> tokenIndices = graph.getTokenIndices();
        for (int i : tokenIndices) {
          org.maltparser.core.syntaxgraph.node.DependencyNode maltNode = graph.getTokenNode(i);
          Token token = tokens.get(maltNode.getIndex() - 1);
          DependencyNode node;
          if (maltNode.getHead().getIndex() != 0) {
            node = new DependencyNode(jCas, token.getBegin(), token.getEnd());
          } else {
            node = new TopDependencyNode(jCas, token.getBegin(), token.getEnd());
          }
          nodes.put(i, node);
        }

        // add relation annotations
        Map<DependencyNode, List<DependencyRelation>> headRelations;
        headRelations = new HashMap<DependencyNode, List<DependencyRelation>>();
        Map<DependencyNode, List<DependencyRelation>> childRelations;
        childRelations = new HashMap<DependencyNode, List<DependencyRelation>>();
        SymbolTable table = graph.getSymbolTables().getSymbolTable("DEPREL");
        for (int i : tokenIndices) {
          org.maltparser.core.syntaxgraph.node.DependencyNode maltNode = graph.getTokenNode(i);
          int headIndex = maltNode.getHead().getIndex();
          if (headIndex != 0) {
            String label = maltNode.getHeadEdge().getLabelSymbol(table);
            DependencyNode node = nodes.get(i);
            DependencyNode head = nodes.get(headIndex);
            DependencyRelation rel = new DependencyRelation(jCas);
            rel.setHead(head);
            rel.setChild(node);
            rel.setRelation(label);
            rel.addToIndexes();
            if (!headRelations.containsKey(node)) {
              headRelations.put(node, new ArrayList<DependencyRelation>());
            }
            headRelations.get(node).add(rel);
            if (!childRelations.containsKey(head)) {
              childRelations.put(head, new ArrayList<DependencyRelation>());
            }
            childRelations.get(head).add(rel);
          }
        }

        // finalize nodes: add links between nodes and relations
        for (DependencyNode node : nodes.values()) {
          List<DependencyRelation> heads = headRelations.get(node);
          node.setHeadRelations(new FSArray(jCas, heads == null ? 0 : heads.size()));
          if (heads != null) {
            FSCollectionFactory.fillArrayFS(node.getHeadRelations(), heads);
          }
          List<DependencyRelation> children = childRelations.get(node);
          node.setChildRelations(new FSArray(jCas, children == null ? 0 : children.size()));
          if (children != null) {
            FSCollectionFactory.fillArrayFS(node.getChildRelations(), children);
          }
          node.addToIndexes();
        }

      } catch (MaltChainedException e) {
        throw new AnalysisEngineProcessException(e);
      }
    }
  }
}
