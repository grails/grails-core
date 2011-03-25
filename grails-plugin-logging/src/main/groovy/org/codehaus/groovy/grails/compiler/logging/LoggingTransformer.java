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
package org.codehaus.groovy.grails.compiler.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;

import java.lang.reflect.Modifier;
import java.net.URL;

/**
 *
 * Adds a log method to all artifacts
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class LoggingTransformer implements ClassInjector{
    public static final String LOG_PROPERTY = "log";

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        final PropertyNode existingProperty = classNode.getProperty(LOG_PROPERTY);
        if(existingProperty == null) {
            final String path = source.getName();

            if(path != null) {
                String artefactType = GrailsResourceUtils.getArtefactDirectory(path);

                String logName = artefactType == null ? classNode.getName() : "grails.app." + artefactType + "." + classNode.getName();
                FieldNode logVariable = new FieldNode(  LOG_PROPERTY,
                                                        Modifier.STATIC & Modifier.PRIVATE,
                                                        new ClassNode(Log.class),
                                                        classNode,
                                                        new MethodCallExpression(new ClassExpression(new ClassNode(LogFactory.class)), "getLog", new ArgumentListExpression(new ConstantExpression(logName))));

                classNode.addField(logVariable);
            }
        }
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return true; // Add log property to all artifact types
    }
}
