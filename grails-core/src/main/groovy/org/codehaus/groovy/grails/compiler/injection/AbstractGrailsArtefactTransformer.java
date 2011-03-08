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

import grails.util.GrailsNameUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Abstract transformer that takes an implementation class and creates methods in a target ClassNode that delegate to that
 * implementation class. Subclasses should override to provide the implementation class details
 *
 * @since 1.4
 * @author  Graeme Rocher
 */
public abstract class AbstractGrailsArtefactTransformer implements ClassInjector{

    private static final Parameter[] ZERO_PARAMETERS = new Parameter[0];
    private static final ClassNode OBJECT_CLASS = new ClassNode(Object.class);
    private static final ClassNode[] EMPTY_CLASS_ARRAY = new ClassNode[0];
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final ArgumentListExpression ZERO_ARGS = new ArgumentListExpression();

    @Override
    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        Class implementation = getInstanceImplementation();

        Assert.notNull(implementation, "No implementation found for AST transform: " + getClass());

        ClassNode implementationNode = new ClassNode(implementation);

        String apiInstanceProperty = GrailsNameUtils.getPropertyNameRepresentation(implementation);
        VariableExpression apiInstance = new VariableExpression(apiInstanceProperty);


        classNode.addProperty(new PropertyNode(apiInstanceProperty, Modifier.PUBLIC, implementationNode, classNode, new ConstructorCallExpression(implementationNode, ZERO_ARGS), null, null));



        while(!implementationNode.equals(OBJECT_CLASS)) {
            List<MethodNode> declaredMethods = implementationNode.getMethods();
            for (MethodNode declaredMethod : declaredMethods) {

                if(isCandidateInstanceMethod(declaredMethod)) {

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
        return !declaredMethod.isSynthetic()
                && Modifier.isPublic(declaredMethod.getModifiers()) && !Modifier.isAbstract(declaredMethod.getModifiers());
    }

    /**
     * The class that provides the implementation of all instance methods and properties
     *
     * @return A class whose methods contain a first argument of type object that is the instance
     */
    public abstract Class getInstanceImplementation();

    @Override
    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }


}
