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
package org.codehaus.groovy.grails.compiler.injection;

import grails.artefact.Artefact;
import grails.artefact.Enhanced;
import grails.util.GrailsUtil;
import groovy.lang.Mixin;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract transformer that takes an implementation class and creates methods
 * in a target ClassNode that delegate to that implementation class. Subclasses
 * should override to provide the implementation class details
 *
 * @since 2.0
 * @author  Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractGrailsArtefactTransformer implements GrailsArtefactClassInjector, AnnotatedClassInjector, Comparable {

    private static final String INSTANCE_PREFIX = "instance";
    private static final String STATIC_PREFIX = "static";
    private static final AnnotationNode AUTO_WIRED_ANNOTATION = new AnnotationNode(new ClassNode(Autowired.class));
    private static final ClassNode ENHANCED_CLASS_NODE = new ClassNode(Enhanced.class);

    protected static final ClassNode OBJECT_CLASS = new ClassNode(Object.class);
    protected static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");

    public static final int PUBLIC_STATIC_MODIFIER = Modifier.PUBLIC | Modifier.STATIC;
    public static final String CURRENT_PREFIX = "current";
    public static final String METHOD_MISSING_METHOD_NAME = "methodMissing";
    public static final String STATIC_METHOD_MISSING_METHOD_NAME = "$static_methodMissing";
    
    private static final String[] DEFAULT_GENERICS_PLACEHOLDERS = new String[]{"D", "T"};

    public String[] getArtefactTypes() {
        return new String[]{getArtefactType()};
    }

    protected String getArtefactType() {
        String name = getClass().getSimpleName();
        if(name.endsWith("Transformer")) {
            return name.substring(0, name.length() - 11);
        }
        return name;
    }

    /**
     * Used for ordering not equality.
     *
     * Note: this class has a natural ordering that is inconsistent with equals.
     *
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(Object o) {
        return 0; // treat all as the same by default for ordering
    }

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        if(classNode instanceof InnerClassNode) return;
        // don't inject if already an @Artefact annotation is applied
        if(!classNode.getAnnotations(new ClassNode(Artefact.class)).isEmpty()) return;
        performInjectionOnAnnotatedClass(source, context, classNode);
    }

    @Override
    public void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        performInjectionOnAnnotatedClass(source, null, classNode);
    }

    public void performInjectionOnAnnotatedClass(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        if(classNode.isEnum()) return; // don't transform enums
        if(classNode instanceof InnerClassNode) return;
        if(classNode.getName().contains("$")) return;
        // only transform the targeted artefact type
        if(!DomainClassArtefactHandler.TYPE.equals(getArtefactType()) && !isValidArtefactTypeByConvention(classNode)) return;
        
        
        Map<String, ClassNode> genericsPlaceholders = resolveGenericsPlaceHolders(classNode);
        
        Class instanceImplementation = getInstanceImplementation();

        if (instanceImplementation != null) {
            ClassNode implementationNode = GrailsASTUtils.replaceGenericsPlaceholders(ClassHelper.make(instanceImplementation), genericsPlaceholders);

            String apiInstanceProperty = INSTANCE_PREFIX + instanceImplementation.getSimpleName();
            Expression apiInstance = new VariableExpression(apiInstanceProperty);

            if (requiresStaticLookupMethod()) {
                final String lookupMethodName = CURRENT_PREFIX + instanceImplementation.getSimpleName();
                createStaticLookupMethod(classNode, implementationNode, apiInstanceProperty, lookupMethodName);
                apiInstance = new MethodCallExpression(new ClassExpression(classNode),lookupMethodName, ZERO_ARGS);
            }
            else if (requiresAutowiring()) {

                final ConstructorCallExpression constructorCallExpression = GrailsASTUtils.hasZeroArgsConstructor(implementationNode) ? new ConstructorCallExpression(implementationNode, ZERO_ARGS) : null;

                PropertyNode propertyNode = new PropertyNode(apiInstanceProperty, Modifier.PUBLIC, implementationNode, classNode, constructorCallExpression, null, null);
                propertyNode.addAnnotation(AUTO_WIRED_ANNOTATION);
                if(getMarkerAnnotation() != null) {
                    propertyNode.addAnnotation(getMarkerAnnotation());
                }
                classNode.addProperty(propertyNode);
            }
            else {
                final ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(implementationNode, ZERO_ARGS);
                FieldNode fieldNode = classNode.getField(apiInstanceProperty);
                if (fieldNode == null || (Modifier.isPrivate(fieldNode.getModifiers()) && !fieldNode.getDeclaringClass().equals(classNode))) {
                    fieldNode = new FieldNode(apiInstanceProperty, PRIVATE_STATIC_MODIFIER,implementationNode, classNode,constructorCallExpression);
                    classNode.addField(fieldNode);
                }
            }

            while (!implementationNode.equals(AbstractGrailsArtefactTransformer.OBJECT_CLASS)) {
                List<MethodNode> declaredMethods = implementationNode.getMethods();
                for (MethodNode declaredMethod : declaredMethods) {
                    if (GrailsASTUtils.isConstructorMethod(declaredMethod)) {
                        GrailsASTUtils.addDelegateConstructor(classNode, declaredMethod, genericsPlaceholders);
                    }
                    else if (isCandidateInstanceMethod(classNode, declaredMethod)) {
                        addDelegateInstanceMethod(classNode, apiInstance, declaredMethod, getMarkerAnnotation(), genericsPlaceholders);
                    }
                }
                implementationNode = implementationNode.getSuperClass();
            }
            performInjectionInternal(apiInstanceProperty, source, classNode);
        }

        Class staticImplementation = getStaticImplementation();

        if (staticImplementation != null) {
            ClassNode staticImplementationNode = GrailsASTUtils.replaceGenericsPlaceholders(ClassHelper.make(staticImplementation), genericsPlaceholders);

            final List<MethodNode> declaredMethods = staticImplementationNode.getMethods();
            final String staticImplementationSimpleName = staticImplementation.getSimpleName();
            String apiInstanceProperty = STATIC_PREFIX + staticImplementationSimpleName;
            final String lookupMethodName = CURRENT_PREFIX + staticImplementationSimpleName;

            if (requiresStaticLookupMethod()) {
                createStaticLookupMethod(classNode, staticImplementationNode, apiInstanceProperty, lookupMethodName);
            }
            else {

                // just create the API
                final ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(staticImplementationNode, ZERO_ARGS);
                FieldNode fieldNode = new FieldNode(apiInstanceProperty, PRIVATE_STATIC_MODIFIER,staticImplementationNode, classNode,constructorCallExpression);
                classNode.addField(fieldNode);

                BlockStatement methodBody = new BlockStatement();
                MethodNode lookupMethod = populateDefaultApiLookupMethod(staticImplementationNode, apiInstanceProperty, lookupMethodName, methodBody);
                classNode.addMethod(lookupMethod);
            }

            MethodCallExpression apiLookupMethod = new MethodCallExpression(new ClassExpression(classNode), lookupMethodName, ZERO_ARGS);

            for (MethodNode declaredMethod : declaredMethods) {
                if (isStaticCandidateMethod(classNode,declaredMethod)) {
                    addDelegateStaticMethod(classNode, apiLookupMethod, declaredMethod, genericsPlaceholders);
                }
            }
        }

        if (classNode.getAnnotations(ENHANCED_CLASS_NODE).isEmpty()) {
            final AnnotationNode annotationNode = new AnnotationNode(ENHANCED_CLASS_NODE);
            annotationNode.setMember("version", new ConstantExpression(GrailsUtil.getGrailsVersion()));
            classNode.addAnnotation(annotationNode);

            AnnotationNode annotation = GrailsASTUtils.findAnnotation(classNode, Mixin.class);
            if (annotation != null) {
                Expression value = annotation.getMember("value");
                if (value != null) {
                    annotationNode.setMember("mixins", value);
                }
            }
        }
    }

    protected Map<String, ClassNode> resolveGenericsPlaceHolders(ClassNode classNode) {
        Map<String, ClassNode> genericsPlaceHolders = new HashMap<String, ClassNode>();
        for(String placeHolder : DEFAULT_GENERICS_PLACEHOLDERS) {
            genericsPlaceHolders.put(placeHolder, classNode);
        }
        return genericsPlaceHolders;
    }

    protected void addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod, AnnotationNode markerAnnotation, Map<String, ClassNode> genericsPlaceholders) {
        GrailsASTUtils.addDelegateInstanceMethod(classNode, delegate, declaredMethod, getMarkerAnnotation(), true, genericsPlaceholders);
    }

    protected void addDelegateStaticMethod(ClassNode classNode, MethodCallExpression apiLookupMethod,
            MethodNode declaredMethod, Map<String, ClassNode> genericsPlaceholders) {
        GrailsASTUtils.addDelegateStaticMethod(apiLookupMethod, classNode, declaredMethod, getMarkerAnnotation(), genericsPlaceholders);
    }
    
    private boolean isValidArtefactTypeByConvention(ClassNode classNode) {
        String[] artefactTypes = getArtefactTypes();
        for (String artefactType : artefactTypes) {
            if(artefactType.equals("*")) return true;
            if(classNode.getName().endsWith(artefactType)) return true;
        }
        return false;
    }

    protected boolean isCandidateInstanceMethod(ClassNode classNode, MethodNode declaredMethod) {
        return GrailsASTUtils.isCandidateInstanceMethod(classNode, declaredMethod);
    }

    protected boolean isStaticCandidateMethod(ClassNode classNode, MethodNode declaredMethod) {
        return GrailsASTUtils.isCandidateMethod(declaredMethod);
    }

    private void createStaticLookupMethod(ClassNode classNode, ClassNode implementationNode, String apiInstanceProperty, String lookupMethodName) {
        // if autowiring is required we add a default method that throws an exception
        // the method should be override via meta-programming in the Grails environment
        MethodNode lookupMethod = classNode.getMethod(lookupMethodName, ZERO_PARAMETERS);
        if (lookupMethod == null) {
            BlockStatement methodBody = new BlockStatement();
            lookupMethod = populateAutowiredApiLookupMethod(classNode, implementationNode, apiInstanceProperty, lookupMethodName, methodBody);
            classNode.addMethod(lookupMethod);
        }
    }

    /**
     * Subclasses should override in the instance API requires a static lookup method instead of autowiring.
     * Defaults to false.
     *
     * @return Whether a static lookup method is used for the instance API
     */
    protected boolean requiresStaticLookupMethod() {
        return false;
    }

    protected MethodNode populateAutowiredApiLookupMethod(ClassNode classNode, ClassNode implementationNode,
                                                          String apiInstanceProperty, String methodName, BlockStatement methodBody) {
        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(new ConstantExpression("Method on class ["+classNode+"] was used outside of a Grails application. If running in the context of a test using the mocking API or bootstrap Grails correctly."));
        methodBody.addStatement(new ThrowStatement(new ConstructorCallExpression(new ClassNode(IllegalStateException.class), arguments)));
        return new MethodNode(methodName, PUBLIC_STATIC_MODIFIER, implementationNode,ZERO_PARAMETERS,null,methodBody);
    }

    protected MethodNode populateDefaultApiLookupMethod(ClassNode implementationNode, String apiInstanceProperty, String methodName, BlockStatement methodBody) {
        methodBody.addStatement(new ReturnStatement(new VariableExpression(apiInstanceProperty)));
        return new MethodNode(methodName, Modifier.PRIVATE, implementationNode,ZERO_PARAMETERS,null,methodBody);
    }

    /**
     * If the API requires autowiring then a @Autowired property will be added. If not a private field
     * that instantiates the API will be crated. Defaults to true.
     *
     * @return Whether autowiring is required
     */
    protected boolean requiresAutowiring() {
        return true;
    }

    /**
     * Subclasses can override to provide additional transformation
     *
     * @param apiInstanceProperty
     * @param source The source
     * @param classNode The class node
     */
    protected void performInjectionInternal(String apiInstanceProperty, SourceUnit source, ClassNode classNode) {
        // do nothing
    }

    /**
     * The class that provides the implementation of all instance methods and properties
     *
     * @return A class whose methods contain a first argument of type object that is the instance
     */
    public abstract Class getInstanceImplementation();

    /**
     * The class that provides static methods
     *
     * @return A class or null if non is provided
     */
    public abstract Class getStaticImplementation();

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    /**
     * A marker annotation to be applied to added methods, defaults to null
     *
     * @return The annotation node
     */
    protected AnnotationNode getMarkerAnnotation() {
        return null;
    }
}
