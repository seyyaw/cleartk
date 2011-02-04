/** 
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
package org.cleartk.timeml.util;

import java.net.URL;
import java.util.MissingResourceException;

/**
 * For ClearTK internal use only.
 * 
 * Determines, from an annotator class name, where in the ClearTK file system (and jar file) the
 * model should be placed.
 * 
 * <br>
 * Copyright (c) 2011, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Steven Bethard
 */
public class CleartkInternalModelLocator {
  private Class<?> annotatorClass;

  public CleartkInternalModelLocator(Class<?> annotatorClass) {
    this.annotatorClass = annotatorClass;
  }
  
  public String getTrainingDirectory() {
    return "src/main/resources/" + this.annotatorClass.getName().toLowerCase().replace('.', '/');
  }
  
  public URL getClassifierJarURL() {
    String resourceName = this.annotatorClass.getSimpleName().toLowerCase() + "/model.jar";
    URL url = this.annotatorClass.getResource(resourceName);
    if (url == null) {
      String className = this.annotatorClass.getName();
      String format = "No classifier jar found at \"%s\" for class %s";
      String message = String.format(format, resourceName, className);
      throw new MissingResourceException(message, className, resourceName);
    }
    return url;
  }
}