/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins;

import grails.util.GrailsNameUtils;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to read plugin information from the AST.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
public class AstPluginDescriptorReader implements PluginDescriptorReader {

    private GroovyClassLoader classLoader;

    public AstPluginDescriptorReader() {
        this(new GroovyClassLoader(Thread.currentThread().getContextClassLoader()));
    }

    public AstPluginDescriptorReader(GroovyClassLoader cl) {
        classLoader = cl;
    }

    public GrailsPluginInfo readPluginInfo(Resource pluginLocation) {
        CompilationUnit compilationUnit = new CompilationUnit(classLoader);
        BasicGrailsPluginInfo pluginInfo = new BasicGrailsPluginInfo(pluginLocation);

        try {
            compilationUnit.addSource("dummy",pluginLocation.getInputStream());
            compilationUnit.addPhaseOperation(new PluginReadingPhaseOperation(pluginInfo), Phases.CONVERSION);
            compilationUnit.compile(Phases.CONVERSION);
            return pluginInfo;
        }
        catch (IOException e) {
            throw new PluginException("Cannot read plugin info: " + e.getMessage());
        }
    }

    class PluginReadingPhaseOperation  extends CompilationUnit.PrimaryClassNodeOperation {
        private BasicGrailsPluginInfo pluginInfo;
        private BeanWrapper wrapper;
        public PluginReadingPhaseOperation(BasicGrailsPluginInfo pluginInfo) {
            this.pluginInfo = pluginInfo;
            wrapper = new BeanWrapperImpl(pluginInfo);
        }

        @Override
        public void call(final SourceUnit source, GeneratorContext context,
                ClassNode classNode) throws CompilationFailedException {

            ClassCodeVisitorSupport visitor = new ClassCodeVisitorSupport() {

                @Override
                public void visitProperty(PropertyNode node) {
                    String name = node.getName();
                    final Expression expr = node.getField().getInitialExpression();

                    if (expr != null) {
                        Object value;
                        if (expr instanceof ListExpression) {
                            final List<String> list = new ArrayList<String>();
                            value = list;
                            for (Expression i : ((ListExpression)expr).getExpressions()) {
                                list.add(i.getText());
                            }
                        }
                        else if (expr instanceof MapExpression) {
                            final Map<String, String> map = new LinkedHashMap<String, String>();
                            value = map;
                            for (MapEntryExpression mee : ((MapExpression)expr).getMapEntryExpressions()) {
                                map.put(mee.getKeyExpression().getText(), mee.getValueExpression().getText());
                            }
                        }
                        else {
                            value = expr.getText();
                        }

                        if (wrapper.isWritableProperty(name)) {
                                wrapper.setPropertyValue(name, value);
                        }
                        else {
                            pluginInfo.setProperty(name, value);
                        }
                        super.visitProperty(node);
                    }
                }

                @Override
                protected SourceUnit getSourceUnit() {
                    return source;
                }
            };

            classNode.visitContents(visitor);
            String className = classNode.getNameWithoutPackage();

            wrapper.setPropertyValue("name", GrailsNameUtils.getPluginName(className + ".groovy"));
        }
    }
}
