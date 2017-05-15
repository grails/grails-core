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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import grails.compiler.ast.AllArtefactClassInjector;
import grails.compiler.ast.AstTransformer;
import org.grails.io.support.GrailsResourceUtils;

import java.lang.reflect.Modifier;
import java.net.URL;

/**
 * Adds a log field to all artifacts.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class LoggingTransformer implements AllArtefactClassInjector{
    public static final String LOG_PROPERTY = "log";
    private static final String FILTERS_ARTEFACT_TYPE_SUFFIX = "Filters";
    public static final String CONF_DIR = "conf";
    public static final String FILTERS_ARTEFACT_TYPE = "filters";

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        final FieldNode existingField = classNode.getDeclaredField(LOG_PROPERTY);
        if (existingField == null && !classNode.isInterface()) {
            final String path = source.getName();

            String artefactType = path != null ? GrailsResourceUtils.getArtefactDirectory(path) : null;

            // little bit of a hack, since filters aren't kept in a grails-app/filters directory as they probably should be
            if (artefactType != null && CONF_DIR.equals(artefactType) && classNode.getName().endsWith(FILTERS_ARTEFACT_TYPE_SUFFIX)) {
                artefactType = FILTERS_ARTEFACT_TYPE;
            }

            String logName = artefactType == null ? classNode.getName() : "grails.app." + artefactType + "." + classNode.getName();
            addLogField(classNode, logName);
        }
    }

    public static void addLogField(ClassNode classNode, String logName) {
        FieldNode logVariable = new FieldNode(LOG_PROPERTY,
                                              Modifier.STATIC | Modifier.PRIVATE,
                                              new ClassNode(Logger.class),
                                              classNode,
                                              new MethodCallExpression(new ClassExpression(new ClassNode(LoggerFactory.class)), "getLogger", new ArgumentListExpression(new ConstantExpression(logName))));

        classNode.addField(logVariable);
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    @Override
    public void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return true; // Add log property to all artifact types
    }
}
