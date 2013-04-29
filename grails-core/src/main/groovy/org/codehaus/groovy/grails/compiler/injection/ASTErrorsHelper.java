/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.compiler.injection;

import org.codehaus.groovy.ast.ClassNode;

/**
 * Enhances a class to contain an Errors property of type org.springframework.validation.Errors.  Methods added include:
 *
 *  <pre>
 *  public void setErrors(Errors errors)
 *  public Errors getErrors()
 *  public void clearErrors()
 *  public Boolean hasErrors()
 *  </pre>
 */
public interface ASTErrorsHelper {

    void injectErrorsCode(ClassNode classNode);
}
