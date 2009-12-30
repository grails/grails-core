/* Copyright 2004-2005 the original author or authors.
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
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.grails.plugins.PluginInfo;
import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This AST transformation automatically annotates any class
 * with @Plugin(name="foo") if it is a plugin resource
 *
 * @author Graeme Rocher
 * @since 1.2
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GlobalPluginAwareEntityASTTransformation implements ASTTransformation {
    private boolean disableTransformation = Boolean.getBoolean("disable.grails.plugin.transform");
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if(disableTransformation) return;
        ASTNode astNode = astNodes[0];

        if(astNode instanceof ModuleNode) {
            ModuleNode moduleNode = (ModuleNode) astNode;

            List classes = moduleNode.getClasses();
            if(classes.size()>0) {
                ClassNode classNode = (ClassNode) classes.get(0);
                File sourcePath = new File(sourceUnit.getName());
                try {
                    String absolutePath = sourcePath.getCanonicalPath();
                    PluginBuildSettings pluginBuildSettings = GrailsPluginUtils.getPluginBuildSettings();
                    if(pluginBuildSettings!=null) {

                        PluginInfo info = pluginBuildSettings.getPluginInfoForSource(absolutePath);

                        if(info!=null) {
                            final ClassNode annotation = new ClassNode(GrailsPlugin.class);
                            final List list = classNode.getAnnotations(annotation);
                            if(list.size()==0) {
                                final AnnotationNode annotationNode = new AnnotationNode(annotation);
                                annotationNode.addMember(org.codehaus.groovy.grails.plugins.GrailsPlugin.NAME, new ConstantExpression(info.getName()));
                                annotationNode.addMember(org.codehaus.groovy.grails.plugins.GrailsPlugin.VERSION, new ConstantExpression(info.getVersion()));
                                annotationNode.setRuntimeRetention(true);
                                annotationNode.setClassRetention(true);


                                classNode.addAnnotation(annotationNode);
                            }
                        }
                    }
                }
                catch (IOException e) {
                    // ignore
                }

            }

        }

    }
}
