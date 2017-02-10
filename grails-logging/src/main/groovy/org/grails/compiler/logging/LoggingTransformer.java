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
package org.grails.compiler.logging;

import grails.compiler.ast.GlobalClassInjectorAdapter;
import groovy.util.logging.Slf4j;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.control.SourceUnit;
import grails.compiler.ast.AllArtefactClassInjector;
import grails.compiler.ast.AstTransformer;
import org.codehaus.groovy.transform.LogASTTransformation;
import java.util.List;

/**
 * Adds a log field to all artifacts.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class LoggingTransformer extends GlobalClassInjectorAdapter implements AllArtefactClassInjector {
    public static final String LOG_PROPERTY = "log";

    public static void addLogField(ClassNode classNode) {
        final FieldNode existingField = classNode.getDeclaredField(LOG_PROPERTY);
        final ClassNode slf4j = ClassHelper.make(Slf4j.class);

        final List<AnnotationNode> existingAnnotation = classNode.getAnnotations(slf4j);
        if (existingField == null && !classNode.isInterface() && existingAnnotation.size() == 0) {
            classNode.addAnnotation(new AnnotationNode(slf4j));
            classNode.addTransform(LogASTTransformation.class, slf4j);
        }
    }

    @Override
    public void performInjectionInternal(SourceUnit source, ClassNode classNode) {
        addLogField(classNode);
    }
}
