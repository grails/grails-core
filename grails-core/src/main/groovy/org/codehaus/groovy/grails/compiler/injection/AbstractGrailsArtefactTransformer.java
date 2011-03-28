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

import grails.artefact.Enhanced;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Abstract transformer that takes an implementation class and creates methods in a target ClassNode that delegate to that
 * implementation class. Subclasses should override to provide the implementation class details
 *
 * @since 1.4
 * @author  Graeme Rocher
 */
public abstract class AbstractGrailsArtefactTransformer implements GrailsArtefactClassInjector, Comparable{

    private static final Parameter[] ZERO_PARAMETERS = new Parameter[0];
    protected static final ClassNode OBJECT_CLASS = new ClassNode(Object.class);
    private static final ClassNode[] EMPTY_CLASS_ARRAY = new ClassNode[0];
    protected static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final String INSTANCE_PREFIX = "instance";
    private static final String STATIC_PREFIX = "static";
    private static final ClassNode CLASS_CLASSNODE = new ClassNode(Class.class);
    private static final AnnotationNode AUTO_WIRED_ANNOTATION = new AnnotationNode(new ClassNode(Autowired.class));
    private static final ClassNode ENHANCED_CLASS_NODE = new ClassNode(Enhanced.class);
    public static final int PUBLIC_STATIC_MODIFIER = Modifier.PUBLIC | Modifier.STATIC;

    public String getArtefactType() {
        String simpleName = getClass().getSimpleName();
        if(simpleName.length()>11) {
            return simpleName.substring(0, simpleName.length()-11);
        }
        return simpleName;
    }

    /**
     * Used for ordering not equality.
     *
     * Note: this class has a natural ordering that is inconsistent with equals.
     *
     *
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(Object o) {
        return 0; // treat all as the same by default for ordering
    }

    public final void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        Class instanceImplementation = getInstanceImplementation();

        if(instanceImplementation != null) {
            ClassNode implementationNode = new ClassNode(instanceImplementation);



            String apiInstanceProperty = INSTANCE_PREFIX + instanceImplementation.getSimpleName();
            VariableExpression apiInstance = new VariableExpression(apiInstanceProperty);


            if(requiresAutowiring()) {

                PropertyNode propertyNode = new PropertyNode(apiInstanceProperty, Modifier.PUBLIC, implementationNode, classNode, new ConstructorCallExpression(implementationNode, ZERO_ARGS), null, null);
                propertyNode.addAnnotation(AUTO_WIRED_ANNOTATION);
                classNode.addProperty(propertyNode);
            }
            else {
                final ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(implementationNode, ZERO_ARGS);
                FieldNode fieldNode = new FieldNode(apiInstanceProperty, PRIVATE_STATIC_MODIFIER,implementationNode, classNode,constructorCallExpression);
                classNode.addField(fieldNode);
            }



            while(!implementationNode.equals(OBJECT_CLASS)) {
                List<MethodNode> declaredMethods = implementationNode.getMethods();
                for (MethodNode declaredMethod : declaredMethods) {

                    if(isConstructor(declaredMethod)) {
                        BlockStatement constructorBody = new BlockStatement();
                        ArgumentListExpression arguments = new ArgumentListExpression();
                        arguments.addExpression(THIS_EXPRESSION);
                        constructorBody.addStatement(new ExpressionStatement( new MethodCallExpression(new ClassExpression(implementationNode), "initialize",arguments)));
                        ConstructorNode constructorNode = getDefaultConstructor(classNode);
                        if(constructorNode != null){
                            constructorBody.addStatement(constructorNode.getCode());
                            constructorNode.setCode(constructorBody);
                        }else{
                            classNode.addConstructor(new ConstructorNode(Modifier.PUBLIC, constructorBody));
                        }
                    }
                    else if(isCandidateInstanceMethod(declaredMethod)) {

                        Parameter[] parameterTypes = getParameterTypes(declaredMethod.getParameters());
                        if(!classNode.hasMethod(declaredMethod.getName(), parameterTypes)) {
                            BlockStatement methodBody = new BlockStatement();
                            ArgumentListExpression arguments = new ArgumentListExpression();
                            arguments.addExpression(THIS_EXPRESSION);
                            for (Parameter parameterType : parameterTypes) {
                                arguments.addExpression(new VariableExpression(parameterType.getName()));
                            }
                            methodBody.addStatement(new ExpressionStatement( new MethodCallExpression(apiInstance, declaredMethod.getName(), arguments)));
                            MethodNode methodNode = new MethodNode(declaredMethod.getName(),
                                                                   Modifier.PUBLIC,
                                                                   declaredMethod.getReturnType(),
                                                                   parameterTypes,
                                                                   EMPTY_CLASS_ARRAY,
                                                                   methodBody
                                                                    );
                            classNode.addMethod(methodNode);
                        }

                    }

                }
                implementationNode = implementationNode.getSuperClass();
            }
            performInjectionInternal(apiInstanceProperty, source, classNode);
        }

        Class staticImplementation = getStaticImplementation();

        if(staticImplementation != null) {
            ClassNode staticImplementationNode = new ClassNode(staticImplementation);

            final List<MethodNode> declaredMethods = staticImplementationNode.getMethods();
            final String staticImplementationSimpleName = staticImplementation.getSimpleName();
            String apiInstanceProperty = STATIC_PREFIX + staticImplementationSimpleName;
            final String lookupMethodName = "current" + staticImplementationSimpleName;

            if(requiresAutowiring()) {
                // if autowiring is required we add a default method that throws an exception
                // the method should be override via meta-programming in the Grails environment
                BlockStatement methodBody = new BlockStatement();
                MethodNode lookupMethod = populateAutowiredApiLookupMethod(staticImplementationNode, apiInstanceProperty, lookupMethodName, methodBody);
                classNode.addMethod(lookupMethod);
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
                if(isCandidateMethod(declaredMethod)) {
                    Parameter[] parameterTypes = declaredMethod.getParameters();
                    if(!classNode.hasMethod(declaredMethod.getName(), parameterTypes)) {
                        BlockStatement methodBody = new BlockStatement();
                        ArgumentListExpression arguments = new ArgumentListExpression();

                        for (Parameter parameterType : parameterTypes) {
                            arguments.addExpression(new VariableExpression(parameterType.getName()));
                        }
                        methodBody.addStatement(new ExpressionStatement( new MethodCallExpression(apiLookupMethod, declaredMethod.getName(), arguments)));

                        MethodNode methodNode = new MethodNode(declaredMethod.getName(),
                                                               PUBLIC_STATIC_MODIFIER,
                                                               declaredMethod.getReturnType(),
                                                               parameterTypes,
                                                               EMPTY_CLASS_ARRAY,
                                                               methodBody
                                                                );


                        classNode.addMethod(methodNode);
                    }
                }
            }

        }

        if(classNode.getAnnotations(ENHANCED_CLASS_NODE).isEmpty())
            classNode.addAnnotation(new AnnotationNode(ENHANCED_CLASS_NODE));

    }

    protected MethodNode populateAutowiredApiLookupMethod(ClassNode implementationNode, String apiInstanceProperty, String methodName, BlockStatement methodBody) {
        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(new ConstantExpression("Cannot locate Grails API implementation. Grails code can only be run within the context of a Grails application."));
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

    private ConstructorNode getDefaultConstructor(ClassNode classNode) {
        ConstructorNode constructorNode = null;
        for(ConstructorNode cons : classNode.getDeclaredConstructors()){
            if(cons.getParameters().length == 0){
                constructorNode = cons;
                break;
            }
        }
        return constructorNode;
    }

    private boolean isConstructor(MethodNode declaredMethod) {
        return declaredMethod.isStatic() && declaredMethod.isPublic() &&
                declaredMethod.getName().equals("initialize") && declaredMethod.getParameters().length == 1 && declaredMethod.getParameters()[0].getType().equals(OBJECT_CLASS);
    }

    private Parameter[] getParameterTypes(Parameter[] parameters) {
        if(parameters.length>1) {
            Parameter[] newParameters = new Parameter[parameters.length-1];
            System.arraycopy(parameters, 1, newParameters, 0, parameters.length - 1);
            return newParameters;
        }
        return ZERO_PARAMETERS;
    }


    private boolean isCandidateInstanceMethod(MethodNode declaredMethod) {
        Parameter[] parameterTypes = declaredMethod.getParameters();
        return isCandidateMethod(declaredMethod) && parameterTypes != null && parameterTypes.length > 0 && parameterTypes[0].getType().equals(OBJECT_CLASS);
    }

    private boolean isCandidateMethod(MethodNode declaredMethod) {
        return !declaredMethod.isSynthetic() &&
                !declaredMethod.getName().contains("$")
                && Modifier.isPublic(declaredMethod.getModifiers()) && !Modifier.isAbstract(declaredMethod.getModifiers());
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

    public final void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }


}
