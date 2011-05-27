/* Copyright 2011 SpringSource
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

import grails.util.PluginBuildSettings;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Automatically annotates each class based on the plugin it originated from.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class PluginAwareAstTransformer implements ClassInjector {

    PluginBuildSettings pluginBuildSettings;

    public PluginAwareAstTransformer() {
        this.pluginBuildSettings = GrailsPluginUtils.getPluginBuildSettings();
    }

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        File sourcePath = new File(source.getName());
        try {
            String absolutePath = sourcePath.getCanonicalPath();
            if (pluginBuildSettings == null) {
                return;
            }

            GrailsPluginInfo info = pluginBuildSettings.getPluginInfoForSource(absolutePath);
            if (info == null) {
                return;
            }

            final ClassNode annotation = new ClassNode(GrailsPlugin.class);
            final List<?> list = classNode.getAnnotations(annotation);
            if (!list.isEmpty()) {
                return;
            }

            if (classNode.isAnnotationDefinition()) {
                return;
            }

            final AnnotationNode annotationNode = new AnnotationNode(annotation);
            annotationNode.addMember(org.codehaus.groovy.grails.plugins.GrailsPlugin.NAME,
                    new ConstantExpression(info.getName()));
            annotationNode.addMember(org.codehaus.groovy.grails.plugins.GrailsPlugin.VERSION,
                    new ConstantExpression(info.getVersion()));
            annotationNode.setRuntimeRetention(true);
            annotationNode.setClassRetention(true);

            classNode.addAnnotation(annotationNode);
        }
        catch (IOException e) {
            // ignore
        }
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return true;
    }
}
