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
package org.codehaus.groovy.grails.compiler.injection;

import grails.util.GrailsNameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Helper methods for working with Groovy AST trees.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public class GrailsASTUtils {

    /**
     * Returns whether a classNode has the specified property or not
     *
     * @param classNode    The ClassNode
     * @param propertyName The name of the property
     * @return True if the property exists in the ClassNode
     */
    public static boolean hasProperty(ClassNode classNode, String propertyName) {
        if (classNode == null || StringUtils.isBlank(propertyName)) {
            return false;
        }

        final MethodNode method = classNode.getMethod(GrailsNameUtils.getGetterName(propertyName), new Parameter[0]);
        if (method != null) return true;

        for (PropertyNode pn : classNode.getProperties()) {
            if (pn.getName().equals(propertyName) && !pn.isPrivate()) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasOrInheritsProperty(ClassNode classNode, String propertyName) {
        if (hasProperty(classNode, propertyName)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (hasProperty(parent, propertyName)) {
                return true;
            }
            parent = parent.getSuperClass();
        }

        return false;
    }

    /**
     * Tests whether the ClasNode implements the specified method name.
     *
     * @param classNode  The ClassNode
     * @param methodName The method name
     * @return True if it does implement the method
     */
    public static boolean implementsZeroArgMethod(ClassNode classNode, String methodName) {
        MethodNode method = classNode.getDeclaredMethod(methodName, new Parameter[]{});
        return method != null && (method.isPublic() || method.isProtected()) && !method.isAbstract();
    }

    @SuppressWarnings("rawtypes")
    public static boolean implementsOrInheritsZeroArgMethod(ClassNode classNode, String methodName, List ignoreClasses) {
        if (implementsZeroArgMethod(classNode, methodName)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (!ignoreClasses.contains(parent) && implementsZeroArgMethod(parent, methodName)) {
                return true;
            }
            parent = parent.getSuperClass();
        }
        return false;
    }

    /**
     * Gets the full name of a ClassNode.
     *
     * @param classNode The class node
     * @return The full name
     */
    public static String getFullName(ClassNode classNode) {
        return classNode.getName();
    }

    public static ClassNode getFurthestParent(ClassNode classNode) {
        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object") ) {
            classNode = parent;
            parent = parent.getSuperClass();
        }
        return classNode;
    }

    public static ClassNode getFurthestUnresolvedParent(ClassNode classNode) {
        ClassNode parent = classNode.getSuperClass();

            while (parent != null && !getFullName(parent).equals("java.lang.Object") && !parent.isResolved() && !Modifier.isAbstract(parent.getModifiers())) {
            classNode = parent;
            parent = parent.getSuperClass();
        }
        return classNode;
    }

    /**
     * Adds a delegate method to the target class node where the first argument is to the delegate method is 'this'.
     * In other words a method such as foo(Object instance, String bar) would be added with a signature of foo(String)
     * and 'this' is passed to the delegate instance
     *
     * @param classNode The class node
     * @param delegate The expression that looks up the delegate
     * @param declaredMethod The declared method
     */
    public static void addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod) {
        addDelegateInstanceMethod(classNode, delegate, declaredMethod, true);
    }

    /**
     * Adds a delegate method to the target class node where the first argument is to the delegate method is 'this'.
     * In other words a method such as foo(Object instance, String bar) would be added with a signature of foo(String)
     * and 'this' is passed to the delegate instance
     *
     * @param classNode The class node
     * @param delegate The expression that looks up the delegate
     * @param declaredMethod The declared method
     * @param thisAsFirstArgument Whether 'this' should be passed as the first argument to the method
     */
    public static void addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod, boolean thisAsFirstArgument) {
        Parameter[] parameterTypes = thisAsFirstArgument ? getRemainingParameterTypes(declaredMethod.getParameters()) : declaredMethod.getParameters();
        if(!classNode.hasDeclaredMethod(declaredMethod.getName(), parameterTypes)) {
            BlockStatement methodBody = new BlockStatement();
            ArgumentListExpression arguments = new ArgumentListExpression();

            if(thisAsFirstArgument) {
                arguments.addExpression(AbstractGrailsArtefactTransformer.THIS_EXPRESSION);
            }

            for (Parameter parameterType : parameterTypes) {
                arguments.addExpression(new VariableExpression(parameterType.getName()));
            }
            methodBody.addStatement(new ExpressionStatement( new MethodCallExpression(delegate, declaredMethod.getName(), arguments)));
            MethodNode methodNode = new MethodNode(declaredMethod.getName(),
                                                   Modifier.PUBLIC,
                                                   declaredMethod.getReturnType(),
                                                   parameterTypes,
                                                   GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY,
                                                   methodBody
                                                    );
            methodNode.addAnnotations(declaredMethod.getAnnotations());

            classNode.addMethod(methodNode);
        }
    }

    private static Parameter[] getRemainingParameterTypes(Parameter[] parameters) {
        if(parameters.length>1) {
            Parameter[] newParameters = new Parameter[parameters.length-1];
            System.arraycopy(parameters, 1, newParameters, 0, parameters.length - 1);
            return newParameters;
        }
        return GrailsArtefactClassInjector.ZERO_PARAMETERS;
    }

    public static void addDelegateStaticMethod(ClassNode classNode, MethodNode declaredMethod) {

        ClassExpression classExpression = new ClassExpression(declaredMethod.getDeclaringClass());
        Parameter[] parameterTypes = declaredMethod.getParameters();
        if(!classNode.hasDeclaredMethod(declaredMethod.getName(), parameterTypes)) {
            BlockStatement methodBody = new BlockStatement();
            ArgumentListExpression arguments = new ArgumentListExpression();

            for (Parameter parameterType : parameterTypes) {
               arguments.addExpression(new VariableExpression(parameterType.getName()));
           }
            methodBody.addStatement(new ExpressionStatement( new MethodCallExpression(classExpression, declaredMethod.getName(), arguments)));
            MethodNode methodNode = new MethodNode(declaredMethod.getName(),
                                                   Modifier.PUBLIC | Modifier.STATIC,
                                                   declaredMethod.getReturnType(),
                                                   parameterTypes,
                                                   GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY,
                                                   methodBody
                                                    );
            methodNode.addAnnotations(declaredMethod.getAnnotations());


            classNode.addMethod(methodNode);
        }
    }
}
