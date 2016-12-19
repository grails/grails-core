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
package grails.compiler.ast;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;

/**
 * Interface specific to Grails artefacts that returns the artefact type.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public interface GrailsArtefactClassInjector extends ClassInjector{

    ArgumentListExpression ZERO_ARGS = new ArgumentListExpression();

    ClassNode[] EMPTY_CLASS_ARRAY = new ClassNode[0];

    Parameter[] ZERO_PARAMETERS = new Parameter[0];

    String[] getArtefactTypes();
}
