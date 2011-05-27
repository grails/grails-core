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
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Helper methods for working with Groovy AST trees.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public class GrailsASTUtils {
    public static final String METHOD_MISSING_METHOD_NAME = "methodMissing";
    public static final String STATIC_METHOD_MISSING_METHOD_NAME = "$static_methodMissing";


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
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
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
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod) {
       return addDelegateInstanceMethod(classNode, delegate, declaredMethod, true);
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
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod, boolean thisAsFirstArgument) {
        Parameter[] parameterTypes = thisAsFirstArgument ? getRemainingParameterTypes(declaredMethod.getParameters()) : declaredMethod.getParameters();
        if (!classNode.hasDeclaredMethod(declaredMethod.getName(), parameterTypes)) {
            BlockStatement methodBody = new BlockStatement();
            ArgumentListExpression arguments = createArgumentListFromParameters(parameterTypes, thisAsFirstArgument);

            ClassNode returnType = nonGeneric(declaredMethod.getReturnType());

            MethodCallExpression methodCallExpression = new MethodCallExpression(delegate, declaredMethod.getName(), arguments);
            methodCallExpression.setMethodTarget(declaredMethod);
            methodBody.addStatement(new ExpressionStatement(methodCallExpression));
            MethodNode methodNode = new MethodNode(declaredMethod.getName(),
                                                   Modifier.PUBLIC,
                                                   returnType,
                                                   copyParameters(parameterTypes),
                                                   GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY,
                                                   methodBody);
            methodNode.addAnnotations(declaredMethod.getAnnotations());

            classNode.addMethod(methodNode);
            return methodNode;
        }
        return null;
    }

    /**
     * Creates an argument list from the given parameter types
     *
     * @param parameterTypes The parameter types
     * @param thisAsFirstArgument Whether to include a reference to 'this' as the first argument
     *
     * @return An argument list
     *
     */
    public static ArgumentListExpression createArgumentListFromParameters(Parameter[] parameterTypes, boolean thisAsFirstArgument) {
        ArgumentListExpression arguments = new ArgumentListExpression();

        if (thisAsFirstArgument) {
            arguments.addExpression(AbstractGrailsArtefactTransformer.THIS_EXPRESSION);
        }

        for (Parameter parameterType : parameterTypes) {
            arguments.addExpression(new VariableExpression(parameterType.getName()));
        }
        return arguments;
    }

    /**
     * Gets the remaining parameters excluding the first parameter in the given list
     *
     * @param parameters The parameters
     * @return A new array with the first parameter removed
     */
    public static Parameter[] getRemainingParameterTypes(Parameter[] parameters) {
        if (parameters.length>1) {
            Parameter[] newParameters = new Parameter[parameters.length-1];
            System.arraycopy(parameters, 1, newParameters, 0, parameters.length - 1);
            return newParameters;
        }
        return GrailsArtefactClassInjector.ZERO_PARAMETERS;
    }

    /**
     * Adds a static method call to given class node that delegates to the given method
     *
     * @param classNode The class node
     * @param delegateMethod The delegate method
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateStaticMethod(ClassNode classNode, MethodNode delegateMethod) {

        ClassExpression classExpression = new ClassExpression(delegateMethod.getDeclaringClass());
        return addDelegateStaticMethod(classExpression, classNode, delegateMethod);
    }

    /**
     * Adds a static method to the given class node that delegates to the given method and resolves the object to invoke the method
     * on from the given expression
     *
     * @param expression The expression
     * @param classNode The class node
     * @param delegateMethod The delegate method
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateStaticMethod(Expression expression, ClassNode classNode, MethodNode delegateMethod) {
        Parameter[] parameterTypes = delegateMethod.getParameters();
        String declaredMethodName = delegateMethod.getName();
        if (!classNode.hasDeclaredMethod(declaredMethodName, parameterTypes)) {
            BlockStatement methodBody = new BlockStatement();
            ArgumentListExpression arguments = new ArgumentListExpression();

            for (Parameter parameterType : parameterTypes) {
               arguments.addExpression(new VariableExpression(parameterType.getName()));
           }
            MethodCallExpression methodCallExpression = new MethodCallExpression(expression, declaredMethodName, arguments);
            methodCallExpression.setMethodTarget(delegateMethod);
            methodBody.addStatement(new ExpressionStatement(methodCallExpression));
            ClassNode returnType = nonGeneric(delegateMethod.getReturnType());
            if (METHOD_MISSING_METHOD_NAME.equals(declaredMethodName)) {
                     declaredMethodName = STATIC_METHOD_MISSING_METHOD_NAME;
            }
            MethodNode methodNode = new MethodNode(declaredMethodName,
                                                   Modifier.PUBLIC | Modifier.STATIC,
                                                   returnType,
                                                   copyParameters(parameterTypes),
                                                   GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY,
                                                   methodBody);
            methodNode.addAnnotations(delegateMethod.getAnnotations());

            classNode.addMethod(methodNode);
            return methodNode;
        }
        return null;
    }


    /**
     * Adds or modifies an existing constructor to delegate to the given static constructor method for initialization logic
     *
     * @param classNode The class node
     * @param constructorMethod The constructor static method
     */
    public static void addDelegateConstructor(ClassNode classNode, MethodNode constructorMethod) {
        BlockStatement constructorBody = new BlockStatement();
        Parameter[] constructorParams = getRemainingParameterTypes(constructorMethod.getParameters());
        ArgumentListExpression arguments = createArgumentListFromParameters(constructorParams, true);
        MethodCallExpression constructCallExpression = new MethodCallExpression(new ClassExpression(constructorMethod.getDeclaringClass()), "initialize", arguments);
        constructCallExpression.setMethodTarget(constructorMethod);
        constructorBody.addStatement(new ExpressionStatement(constructCallExpression));

        if (constructorParams.length == 0) {
            // handle default constructor

            ConstructorNode constructorNode = getDefaultConstructor(classNode);
            if (constructorNode != null) {
                constructorBody.addStatement(constructorNode.getCode());
                constructorNode.setCode(constructorBody);
            }else{
                classNode.addConstructor(new ConstructorNode(Modifier.PUBLIC, constructorBody));
            }
        }
        else {
            // create new constructor, restoring default constructor if there is none
            ConstructorNode cn = findConstructor(classNode, constructorParams);
            if (cn != null) {
                Statement code = cn.getCode();
                constructorBody.addStatement(code);
                cn.setCode(constructorBody);
            }
            else {
                cn = new ConstructorNode(Modifier.PUBLIC,copyParameters(constructorParams),null, constructorBody);
                classNode.addConstructor(cn);
            }

            ConstructorNode defaultConstructor = getDefaultConstructor(classNode);
            if (defaultConstructor == null) {
                // add empty
                classNode.addConstructor(new ConstructorNode(Modifier.PUBLIC, new BlockStatement()));
            }
        }
    }

    /**
     * Finds a constructor for the given class node and parameter types
     *
     * @param classNode The class node
     * @param constructorParams The parameter types
     * @return The located constructor or null
     */
    public static ConstructorNode findConstructor(ClassNode classNode,Parameter[] constructorParams) {
        List<ConstructorNode> declaredConstructors = classNode.getDeclaredConstructors();
        for (ConstructorNode declaredConstructor : declaredConstructors) {
            if (parametersEqual(constructorParams, declaredConstructor.getParameters())) {
                return declaredConstructor;
            }
        }
        return null;
    }

    /**
     * @return true if the two arrays are of the same size and have the same contents
     */
    public static boolean parametersEqual(Parameter[] a, Parameter[] b) {
        if (a.length == b.length) {
            boolean answer = true;
            for (int i = 0; i < a.length; i++) {
                if (!a[i].getType().equals(b[i].getType())) {
                    answer = false;
                    break;
                }
            }
            return answer;
        }
        return false;
    }

    /**
     * Obtains the default constructor for the given class node
     *
     * @param classNode The class node
     * @return The default constructor or null if there isn't one
     */
    public static ConstructorNode getDefaultConstructor(ClassNode classNode) {
        ConstructorNode constructorNode = null;
        for (ConstructorNode cons : classNode.getDeclaredConstructors()) {
            if (cons.getParameters().length == 0) {
                constructorNode = cons;
                break;
            }
        }
        return constructorNode;
    }

    private static Parameter[] copyParameters(Parameter[] parameterTypes) {
        Parameter[] newParameterTypes = new Parameter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameterType = parameterTypes[i];
            Parameter newParameter = new Parameter(nonGeneric(parameterType.getType()), parameterType.getName(), parameterType.getInitialExpression());
            newParameter.addAnnotations(parameterType.getAnnotations());
            newParameterTypes[i] = newParameter;
        }
        return newParameterTypes;
    }

    private static ClassNode nonGeneric(ClassNode type) {
        if (type.isUsingGenerics()) {
            final ClassNode nonGen = ClassHelper.makeWithoutCaching(type.getName());
            nonGen.setRedirect(type);
            nonGen.setGenericsTypes(null);
            nonGen.setUsingGenerics(false);
            return nonGen;
        }
        else if (type.isArray()) {
            final ClassNode nonGen = ClassHelper.makeWithoutCaching(Object.class);
            nonGen.setUsingGenerics(false);
            return nonGen.makeArray();
        }
        else {
            return type;
        }
    }

    public static boolean isCandidateInstanceMethod(ClassNode classNode, MethodNode declaredMethod) {
        Parameter[] parameterTypes = declaredMethod.getParameters();
        return isCandidateMethod(declaredMethod) && parameterTypes != null && parameterTypes.length > 0 && isAssignableFrom(parameterTypes[0].getType(), classNode);
    }

    /**
     *
     * Determines if the class or interface represented by the superClass argument is either the same as, or is a superclass or superinterface of, the class or interface represented by the specified subClass parameter.
     *
     * @param superClass The super class to check
     * @param childClass The sub class the check
     *
     * @return True if the childClass either equal to or a sub class of the specified superClass otherwise return false
     */
    private static boolean isAssignableFrom(ClassNode superClass, ClassNode childClass) {
        ClassNode currentSuper = childClass;
        while (currentSuper != null)  {
            if (currentSuper.equals(superClass)) {
                return true;
            }

            currentSuper = currentSuper.getSuperClass();
        }
        return false;
    }

    public static boolean isCandidateMethod(MethodNode declaredMethod) {
        return !declaredMethod.isSynthetic() &&
                !declaredMethod.getName().contains("$")
                && Modifier.isPublic(declaredMethod.getModifiers()) && !Modifier.isAbstract(declaredMethod.getModifiers());
    }

    public static boolean isConstructorMethod(MethodNode declaredMethod) {
        return declaredMethod.isStatic() && declaredMethod.isPublic() &&
                declaredMethod.getName().equals("initialize") && declaredMethod.getParameters().length >= 1 && declaredMethod.getParameters()[0].getType().equals(AbstractGrailsArtefactTransformer.OBJECT_CLASS);
    }

    public static void addDelegateInstanceMethods(ClassNode classNode, ClassNode delegateNode, Expression delegateInstance) {
        addDelegateInstanceMethods(classNode, classNode,delegateNode, delegateInstance);
    }

    public static void addDelegateInstanceMethods(ClassNode supportedSuperType, ClassNode classNode, ClassNode delegateNode, Expression delegateInstance) {
        while (!delegateNode.equals(AbstractGrailsArtefactTransformer.OBJECT_CLASS)) {
            List<MethodNode> declaredMethods = delegateNode.getMethods();
            for (MethodNode declaredMethod : declaredMethods) {

                if (isConstructorMethod(declaredMethod)) {
                    addDelegateConstructor(classNode, declaredMethod);

                }
                else if (isCandidateInstanceMethod(supportedSuperType, declaredMethod)) {
                    addDelegateInstanceMethod(classNode, delegateInstance, declaredMethod);
                }

            }
            delegateNode = delegateNode.getSuperClass();
        }
    }

    public static void addFieldIfNonExistent(ClassNode classNode, ClassNode fieldType, String fieldName) {
        if (classNode != null && classNode.getField(fieldName) == null) {
            classNode.addField(fieldName, Modifier.PRIVATE, fieldType, new ConstructorCallExpression(fieldType, new ArgumentListExpression()));
        }
    }
}
