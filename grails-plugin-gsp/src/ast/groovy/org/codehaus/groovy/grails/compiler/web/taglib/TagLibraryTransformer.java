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
package org.codehaus.groovy.grails.compiler.web.taglib;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.plugins.web.api.TagLibraryApi;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.springframework.web.context.request.RequestContextHolder;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Enhances tag library classes with the appropriate API at compile time
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class TagLibraryTransformer extends AbstractGrailsArtefactTransformer{
    public static Pattern TAGLIB_PATTERN = Pattern.compile(".+/"+ GrailsResourceUtils.GRAILS_APP_DIR+"/taglib/(.+)TagLib\\.groovy");
    private static final String ATTRS_ARGUMENT = "attrs";
    private static final String BODY_ARGUMENT = "body";
    private static final Parameter[] MAP_CLOSURE_PARAMETERS = new Parameter[]{ new Parameter(new ClassNode(Map.class), ATTRS_ARGUMENT),new Parameter(new ClassNode(Closure.class), BODY_ARGUMENT) };
    private static final Parameter[] CLOSURE_PARAMETERS = new Parameter[]{ new Parameter(new ClassNode(Closure.class), BODY_ARGUMENT) };
    private static final Parameter[] MAP_PARAMETERS = new Parameter[]{ new Parameter(new ClassNode(Map.class), ATTRS_ARGUMENT) };
    private static final Parameter[] MAP_CHARSEQUENCE_PARAMETERS = new Parameter[]{ new Parameter(new ClassNode(Map.class), ATTRS_ARGUMENT),new Parameter(new ClassNode(CharSequence.class), BODY_ARGUMENT) };
    private static final ClassNode GROOVY_PAGE_CLASS_NODE = new ClassNode(GroovyPage.class);
    private static final VariableExpression ATTRS_EXPRESSION = new VariableExpression(ATTRS_ARGUMENT);
    private static final VariableExpression BODY_EXPRESSION = new VariableExpression(BODY_ARGUMENT);
    private static final MethodCallExpression CURRENT_REQUEST_ATTRIBUTES_METHOD_CALL = new MethodCallExpression(new ClassExpression(new ClassNode(RequestContextHolder.class)), "currentRequestAttributes", ZERO_ARGS);
    private static final Expression NULL_EXPRESSION = new ConstantExpression(null);
    private static final String NAMESPACE_PROPERTY = "namespace";

    @Override
    public Class<?> getInstanceImplementation() {
        return TagLibraryApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // no static methods
    }

    @Override
    protected void performInjectionInternal(String apiInstanceProperty, SourceUnit source, ClassNode classNode) {
        List<PropertyNode> tags = findTags(classNode);

        PropertyNode namespaceProperty = classNode.getProperty(NAMESPACE_PROPERTY);
        String namespace = GroovyPage.DEFAULT_NAMESPACE;
        if (namespaceProperty != null && namespaceProperty.isStatic()) {
            Expression initialExpression = namespaceProperty.getInitialExpression();
            if (initialExpression instanceof ConstantExpression) {
                namespace = initialExpression.getText();
            }
        }

        MethodCallExpression tagLibraryLookupMethodCall = new MethodCallExpression(new VariableExpression(apiInstanceProperty), "getTagLibraryLookup", ZERO_ARGS);
        for (PropertyNode tag : tags) {
            String tagName = tag.getName();
            addAttributesAndBodyMethod(classNode, tagLibraryLookupMethodCall, namespace, tagName);
            addAttributesAndStringBodyMethod(classNode, tagName);
            addAttributesAndBodyMethod(classNode, tagLibraryLookupMethodCall, namespace, tagName, false);
            addAttributesAndBodyMethod(classNode, tagLibraryLookupMethodCall, namespace, tagName, true, false);
            addAttributesAndBodyMethod(classNode, tagLibraryLookupMethodCall, namespace, tagName, false, false);
        }
    }

    private void addAttributesAndStringBodyMethod(ClassNode classNode, String tagName) {
        BlockStatement methodBody = new BlockStatement();
        ArgumentListExpression arguments = new ArgumentListExpression();
        ArgumentListExpression constructorArgs = new ArgumentListExpression();
        constructorArgs.addExpression(BODY_EXPRESSION);
        arguments.addExpression(ATTRS_EXPRESSION)
                 .addExpression(new ConstructorCallExpression(new ClassNode(GroovyPage.ConstantClosure.class), constructorArgs));
        methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, tagName, arguments)));
        classNode.addMethod(new MethodNode(tagName, Modifier.PUBLIC,OBJECT_CLASS, MAP_CHARSEQUENCE_PARAMETERS, null, methodBody));
    }

    private void addAttributesAndBodyMethod(ClassNode classNode, MethodCallExpression tagLibraryLookupMethodCall, String namespace, String tagName) {
        addAttributesAndBodyMethod(classNode, tagLibraryLookupMethodCall, namespace, tagName, true);
    }

    private void addAttributesAndBodyMethod(ClassNode classNode, MethodCallExpression tagLibraryLookupMethodCall, String namespace, String tagName, boolean includeBody) {
        addAttributesAndBodyMethod(classNode, tagLibraryLookupMethodCall, namespace, tagName, includeBody, true);
    }

    private void addAttributesAndBodyMethod(ClassNode classNode, MethodCallExpression tagLibraryLookupMethodCall, String namespace, String tagName, boolean includeBody, boolean includeAttrs) {
        BlockStatement methodBody = new BlockStatement();
        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(tagLibraryLookupMethodCall)
                 .addExpression(new ConstantExpression(namespace))
                 .addExpression(new ConstantExpression(tagName))
                 .addExpression(includeAttrs ? ATTRS_EXPRESSION : new MapExpression())
                 .addExpression(includeBody ? BODY_EXPRESSION : NULL_EXPRESSION)
                 .addExpression(CURRENT_REQUEST_ATTRIBUTES_METHOD_CALL);

        methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new ClassExpression(GROOVY_PAGE_CLASS_NODE),"captureTagOutput", arguments)));

        if (includeBody && includeAttrs) {
            if (!methodExists(classNode, tagName, MAP_CLOSURE_PARAMETERS))
                classNode.addMethod(new MethodNode(tagName, Modifier.PUBLIC,OBJECT_CLASS, MAP_CLOSURE_PARAMETERS, null, methodBody));
        }
        else if (includeAttrs && !includeBody) {
            if (!methodExists(classNode, tagName, MAP_PARAMETERS))
                classNode.addMethod(new MethodNode(tagName, Modifier.PUBLIC,OBJECT_CLASS, MAP_PARAMETERS, null, methodBody));
        }
        else if (includeBody) {
            if (!methodExists(classNode, tagName, CLOSURE_PARAMETERS))
                classNode.addMethod(new MethodNode(tagName, Modifier.PUBLIC,OBJECT_CLASS, CLOSURE_PARAMETERS, null, methodBody));
        }
        else  {
            if (!methodExists(classNode, tagName, Parameter.EMPTY_ARRAY))
                classNode.addMethod(new MethodNode(tagName, Modifier.PUBLIC,OBJECT_CLASS, Parameter.EMPTY_ARRAY, null, methodBody));
        }
    }

    private boolean methodExists(ClassNode classNode, String methodName, Parameter[] parameters) {
        return classNode.getMethod(methodName, parameters) != null;
    }

    private List<PropertyNode> findTags(ClassNode classNode) {
        List<PropertyNode> tags = new ArrayList<PropertyNode>();
        List<PropertyNode> properties = classNode.getProperties();

        for (PropertyNode property : properties) {
            if (property.isPublic()) {
                Expression initialExpression = property.getInitialExpression();
                if (initialExpression instanceof ClosureExpression) {
                    ClosureExpression ce = (ClosureExpression) initialExpression;
                    Parameter[] parameters = ce.getParameters();
                    if (parameters.length > 0) {
                        tags.add(property);
                    }
                }
            }
        }
        return tags;
    }

    public boolean shouldInject(URL url) {
        return url != null && TAGLIB_PATTERN.matcher(url.getFile()).find();
    }
}
