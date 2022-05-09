/*
 * Copyright 2014-2022 original authors
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
package org.grails.compiler.injection;

import grails.plugins.GrailsPluginInfo;
import grails.util.GrailsNameUtils;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.grails.io.support.Resource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads plugin info from the AST
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
class PluginAstReader {
    private BasicGrailsPluginInfo pluginInfo;

    public PluginAstReader() {
        this.pluginInfo = new BasicGrailsPluginInfo();
    }

    GrailsPluginInfo getPluginInfo() {
        return this.pluginInfo;
    }

    public GrailsPluginInfo readPluginInfo(ClassNode classNode) throws CompilationFailedException {
        String className = classNode.getNameWithoutPackage();

        if(className.endsWith("GrailsPlugin")) {
            visitContents(className, classNode);
        }

        pluginInfo.setName(GrailsNameUtils.getPluginName(className + ".groovy"));

        Map<String, Object> pluginProperties = pluginInfo.getProperties();
        for (Map.Entry<String, Object> entry : pluginProperties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof String) {
                String val = (String)value;
                if (val != null && val.length() > 2 && val.startsWith("@") && val.endsWith("@")) {
                    String token = val.substring(1, val.length() - 1);
                    val = String.valueOf(pluginProperties.get(token));
                    pluginInfo.setProperty(key, val);
                }
                if (key.equals("version")) {
                    pluginInfo.setVersion(val);
                }
            }
            else if (value instanceof Map) {
                Map<String, String> map = (Map<String, String>)value;
                for (Map.Entry me : map.entrySet()) {
                    final String k = String.valueOf(me.getKey());
                    final String v = String.valueOf(me.getValue());

                    if (v != null && v.length() > 2 && v.startsWith("@") && v.endsWith("@")) {
                        String token = v.substring(1, v.length() - 1);
                        String newValue = String.valueOf(pluginProperties.get(token));
                        if (newValue != null && newValue.length() > 2 && newValue.startsWith("@") && newValue.endsWith("@")) {
                            token = newValue.substring(1, newValue.length() - 1);
                            newValue = String.valueOf(pluginProperties.get(token));
                        }
                        map.put(k, newValue);
                    }
                }
            }
        }

        return pluginInfo;
    }

    protected void visitContents(String className, final ClassNode classNode) {
        ClassCodeVisitorSupport visitor = new ClassCodeVisitorSupport() {

            @Override
            public void visitProperty(PropertyNode node) {
                String name = node.getName();

                final Expression expr = node.getField().getInitialExpression();

                if (expr != null) {
                    Object value = null;
                    if (expr instanceof ListExpression) {
                        final List<String> list = new ArrayList<String>();
                        for (Expression i : ((ListExpression)expr).getExpressions()) {
                            list.add(i.getText());
                        }
                        value = list;
                    }
                    else if (expr instanceof MapExpression) {
                        final Map<String, String> map = new LinkedHashMap<String, String>();
                        MapExpression mapExpr = (MapExpression)expr;
                        for (MapEntryExpression mee : mapExpr.getMapEntryExpressions()) {
                            Expression keyExpr = mee.getKeyExpression();
                            Expression valueExpr = mee.getValueExpression();
                            String valueObj = valueExpr.getText();
                            if(valueExpr instanceof ConstantExpression) {
                                valueObj = String.valueOf(((ConstantExpression)valueExpr).getValue());
                            }
                            else if (valueExpr instanceof VariableExpression) {
                                VariableExpression ve = (VariableExpression)valueExpr;
                                valueObj = String.format("@%s@", ve.getName());
                            }
                            map.put(keyExpr.getText(), valueObj);
                        }
                        value = map;
                    }
                    else if (expr instanceof MethodCallExpression) {
                        Expression objectExpr = ((MethodCallExpression)expr).getObjectExpression();
                        Expression methodExpr = ((MethodCallExpression)expr).getMethod();
                        if (objectExpr instanceof ClassExpression && methodExpr instanceof ConstantExpression) {
                            String objectExprName = objectExpr.getText();
                            String methodNameExprName = String.valueOf(((ConstantExpression)methodExpr).getValue());
                            if (objectExprName.equals("grails.util.GrailsUtil") && methodNameExprName.equals("getGrailsVersion")) {
                                value = getClass().getPackage().getImplementationVersion();
                            }
                        }
                    }
                    else if (expr instanceof VariableExpression) {
                        VariableExpression ve = (VariableExpression)expr;
                        value = String.format("@%s@", ve.getName());
                    }
                    else if (expr instanceof ConstantExpression)  {
                        value = String.valueOf(((ConstantExpression)expr).getValue());
                    }
                    else {
                        value = expr.getText();
                    }
                    if(value != null) {
                        pluginInfo.setProperty(name, value);
                        super.visitProperty(node);
                    }
                }
            }

            @Override
            protected SourceUnit getSourceUnit() {
                return classNode.getModule().getContext();
            }

        };

        classNode.visitContents(visitor);
    }


    /**
     * Simple Javabean implementation of the GrailsPluginInfo interface.
     *
     * @author Graeme Rocher
     * @since 1.3
     */
    public class BasicGrailsPluginInfo implements GrailsPluginInfo {

        private String name;
        private String version;
        private Map<String,Object> attributes = new ConcurrentHashMap<String,Object>();

        public BasicGrailsPluginInfo() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public void setProperty(String property, Object newValue) {
            attributes.put(property, newValue);
        }

        public Object getProperty(String property) {
           return attributes.get(property);
        }

        public String getFullName() {
            return name + '-' + version;
        }

        public Resource getDescriptor() {
            return null;
        }

        public Resource getPluginDir() {
            return null;
        }

        public Map<String, Object> getProperties() {
            Map<String, Object> props = new HashMap<>();
            props.putAll(attributes);
            if (name != null) {
                props.put(NAME, name);
            }
            if (version != null) {
                props.put(VERSION, version);
            }
            return props;
        }

    }
}
