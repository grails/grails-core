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
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AllArtefactClassInjector;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;

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
public class LoggingTransformer implements AllArtefactClassInjector{
    public static final String LOG_PROPERTY = "log";
    private static final String FILTERS_ARTEFACT_TYPE_SUFFIX = "Filters";
    public static final String CONF_DIR = "conf";
    public static final String FILTERS_ARTEFACT_TYPE = "filters";

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        final FieldNode existingField = classNode.getField(LOG_PROPERTY);
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
                                              new ClassNode(Log.class),
                                              classNode,
                                              new MethodCallExpression(new ClassExpression(new ClassNode(LogFactory.class)), "getLog", new ArgumentListExpression(new ConstantExpression(logName))));

        classNode.addField(logVariable);
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return true; // Add log property to all artifact types
    }
}
