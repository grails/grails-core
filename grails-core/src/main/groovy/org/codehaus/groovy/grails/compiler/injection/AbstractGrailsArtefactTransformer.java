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
public abstract class AbstractGrailsArtefactTransformer implements GrailsArtefactClassInjector{

    private static final Parameter[] ZERO_PARAMETERS = new Parameter[0];
    protected static final ClassNode OBJECT_CLASS = new ClassNode(Object.class);
    private static final ClassNode[] EMPTY_CLASS_ARRAY = new ClassNode[0];
    protected static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    protected static final ArgumentListExpression ZERO_ARGS = new ArgumentListExpression();
    private static final String INSTANCE_PREFIX = "instance";
    private static final ClassNode CLASS_CLASSNODE = new ClassNode(Class.class);
    private static final AnnotationNode AUTO_WIRED_ANNOTATION = new AnnotationNode(new ClassNode(Autowired.class));
    private static final ClassNode ENHANCED_CLASS_NODE = new ClassNode(Enhanced.class);

    @Override
    public String getArtefactType() {
        String simpleName = getClass().getSimpleName();
        if(simpleName.length()>11) {
            return simpleName.substring(0, simpleName.length()-11);
        }
        return simpleName;
    }

    @Override
    public final void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        Class instanceImplementation = getInstanceImplementation();

        if(instanceImplementation != null) {
            ClassNode implementationNode = new ClassNode(instanceImplementation);



            String apiInstanceProperty = INSTANCE_PREFIX + instanceImplementation.getSimpleName();
            VariableExpression apiInstance = new VariableExpression(apiInstanceProperty);


            PropertyNode propertyNode = new PropertyNode(apiInstanceProperty, Modifier.PUBLIC, implementationNode, classNode, new ConstructorCallExpression(implementationNode, ZERO_ARGS), null, null);
            propertyNode.addAnnotation(AUTO_WIRED_ANNOTATION);
            classNode.addProperty(propertyNode);

            while(!implementationNode.equals(OBJECT_CLASS)) {
                List<MethodNode> declaredMethods = implementationNode.getMethods();
                for (MethodNode declaredMethod : declaredMethods) {

                    if(isConstructor(declaredMethod)) {
                        BlockStatement constructorBody = new BlockStatement();
                        ArgumentListExpression arguments = new ArgumentListExpression();
                        arguments.addExpression(THIS_EXPRESSION);
                        constructorBody.addStatement(new ExpressionStatement( new MethodCallExpression(new ClassExpression(implementationNode), "initialize",arguments)));
                        classNode.addConstructor(new ConstructorNode(Modifier.PUBLIC, constructorBody));
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


        if(classNode.getAnnotations(ENHANCED_CLASS_NODE) == null)
            classNode.addAnnotation(new AnnotationNode(ENHANCED_CLASS_NODE));

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
        return !declaredMethod.isSynthetic()
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

    @Override
    public final void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }


}
