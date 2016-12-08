/** 
 * Copyright (c) 2009-2011, Regents of the University of Colorado 
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
package org.cleartk.ml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.initializable.Initializable;
import org.apache.uima.fit.factory.initializable.InitializableFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.util.CleartkInitializationException;
import org.cleartk.util.ReflectionUtil;

/**
 * <br>
 * Copyright (c) 2009-2011, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 */

public abstract class CleartkSequenceAnnotator<OUTCOME_TYPE> extends JCasAnnotator_ImplBase
    implements Initializable {

  public static final String PARAM_CLASSIFIER_FACTORY_CLASS_NAME = "classifierFactoryClassName";

  private static final String DEFAULT_CLASSIFIER_FACTORY_CLASS_NAME = "org.cleartk.ml.jar.SequenceJarClassifierFactory";

  @ConfigurationParameter(
      name = PARAM_CLASSIFIER_FACTORY_CLASS_NAME,
      mandatory = false,
      description = "provides the full name of the SequenceClassifierFactory class to be used.",
      defaultValue = "org.cleartk.ml.jar.SequenceJarClassifierFactory")
  private String classifierFactoryClassName;

  public static final String PARAM_DATA_WRITER_FACTORY_CLASS_NAME = "dataWriterFactoryClassName";

  private static final String DEFAULT_DATA_WRITER_FACTORY_CLASS_NAME = "org.cleartk.ml.jar.DefaultSequenceDataWriterFactory";

  @ConfigurationParameter(
      name = PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
      mandatory = false,
      description = "provides the full name of the SequenceDataWriterFactory class to be used.",
      defaultValue = DEFAULT_DATA_WRITER_FACTORY_CLASS_NAME)
  private String dataWriterFactoryClassName;

  public static final String PARAM_IS_TRAINING = "isTraining";

  @ConfigurationParameter(
      name = PARAM_IS_TRAINING,
      mandatory = false,
      description = "determines whether this annotator is writing training data or using a classifier to annotate. Normally inferred automatically based on whether or not a DataWriterFactory class has been set.")
  private Boolean isTraining;

  private boolean primitiveIsTraining;

  protected SequenceDataWriter<OUTCOME_TYPE> dataWriter;

  protected SequenceClassifier<OUTCOME_TYPE> classifier;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);

    if (dataWriterFactoryClassName == null && classifierFactoryClassName == null) {
      CleartkInitializationException.neitherParameterSet(
          PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
          dataWriterFactoryClassName,
          PARAM_CLASSIFIER_FACTORY_CLASS_NAME,
          classifierFactoryClassName);
    }

    // determine whether we start out as training or predicting
    if (this.isTraining != null) {
      this.primitiveIsTraining = this.isTraining;
    } else if (!DEFAULT_DATA_WRITER_FACTORY_CLASS_NAME.equals(this.dataWriterFactoryClassName)) {
      this.primitiveIsTraining = true;
    } else if (context.getConfigParameterValue(DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY) != null) {
      this.primitiveIsTraining = true;
    } else if (!DEFAULT_CLASSIFIER_FACTORY_CLASS_NAME.equals(this.classifierFactoryClassName)) {
      this.primitiveIsTraining = false;
    } else if (context.getConfigParameterValue(GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH) != null) {
      this.primitiveIsTraining = false;
    } else {
      String message = "Please specify PARAM_IS_TRAINING - unable to infer it from context";
      throw new IllegalArgumentException(message);
    }

    if (this.isTraining()) {
      // create the factory and instantiate the data writer
      SequenceDataWriterFactory<?> factory = InitializableFactory.create(
          context,
          dataWriterFactoryClassName,
          SequenceDataWriterFactory.class);
      SequenceDataWriter<?> untypedDataWriter;
      try {
        untypedDataWriter = factory.createDataWriter();
      } catch (IOException e) {
        throw new ResourceInitializationException(e);
      }

      InitializableFactory.initialize(untypedDataWriter, context);
      this.dataWriter = ReflectionUtil.uncheckedCast(untypedDataWriter);
    } else {
      // create the factory and instantiate the classifier
      SequenceClassifierFactory<?> factory = InitializableFactory.create(
          context,
          classifierFactoryClassName,
          SequenceClassifierFactory.class);
      SequenceClassifier<?> untypedClassifier;
      try {
        untypedClassifier = factory.createClassifier();
      } catch (IOException e) {
        throw new ResourceInitializationException(e);
      }

      this.classifier = ReflectionUtil.uncheckedCast(untypedClassifier);
      ReflectionUtil.checkTypeParameterIsAssignable(
          CleartkSequenceAnnotator.class,
          "OUTCOME_TYPE",
          this,
          SequenceClassifier.class,
          "OUTCOME_TYPE",
          this.classifier);
      InitializableFactory.initialize(untypedClassifier, context);
    }
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    super.collectionProcessComplete();
    if (isTraining()) {
      dataWriter.finish();
    }
  }

  protected boolean isTraining() {
    return this.primitiveIsTraining;
  }

  protected List<OUTCOME_TYPE> classify(List<Instance<OUTCOME_TYPE>> instances)
      throws CleartkProcessingException {
    List<List<Feature>> instanceFeatures = new ArrayList<List<Feature>>();
    for (Instance<OUTCOME_TYPE> instance : instances) {
      instanceFeatures.add(instance.getFeatures());
    }
    return this.classifier.classify(instanceFeatures);
  }

  protected List<OUTCOME_TYPE> classify(
      Map<Integer, List<Instance<OUTCOME_TYPE>>> instances,
      File featureFile) throws CleartkProcessingException {
    Map<Integer, List<List<Feature>>> instanceFeaturesMap = new LinkedHashMap<Integer, List<List<Feature>>>();
    for (int i : instances.keySet()) {
      List<List<Feature>> instanceFeatures = new ArrayList<List<Feature>>();
      for (Instance<OUTCOME_TYPE> instance : instances.get(i)) {
        instanceFeatures.add(instance.getFeatures());
      }
      instanceFeaturesMap.put(i, instanceFeatures);
    }
    return this.classifier.classify(instanceFeaturesMap, featureFile);
  }

}
