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

import grails.build.logging.GrailsConsole;
import grails.persistence.Entity;
import grails.util.GrailsNameUtils;
import groovy.lang.MissingMethodException;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.control.Janitor;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

/**
 * Helper methods for working with Groovy AST trees.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public class GrailsASTUtils {

    public static final String METHOD_MISSING_METHOD_NAME = "methodMissing";
    public static final String STATIC_METHOD_MISSING_METHOD_NAME = "$static_methodMissing";
    public static final Token EQUALS_OPERATOR = Token.newSymbol("==", 0, 0);
    public static final Token NOT_EQUALS_OPERATOR = Token.newSymbol("!=", 0, 0);

    public static final ClassNode MISSING_METHOD_EXCEPTION = new ClassNode(MissingMethodException.class);
    public static final ConstantExpression NULL_EXPRESSION = new ConstantExpression(null);
    public static final Token ASSIGNMENT_OPERATOR = Token.newSymbol(Types.ASSIGNMENT_OPERATOR, 0, 0);

    
    public static void warning(final SourceUnit sourceUnit, final ASTNode node, final String warningMessage) {
        final String sample = sourceUnit.getSample(node.getLineNumber(), node.getColumnNumber(), new Janitor());
        GrailsConsole.getInstance().warning(warningMessage + "\n\n" + sample);
    }
    
    /**
     * Generates a fatal compilation error
     * 
     * @param sourceUnit the SourceUnit
     * @param astNode the ASTNode which caused the error
     * @param message The error message
     */
    public static void error(final SourceUnit sourceUnit, final ASTNode astNode, final String message) {
        error(sourceUnit, astNode, message, true);
    }

    /**
     * Generates a fatal compilation error
     * 
     * @param sourceUnit the SourceUnit
     * @param astNode the ASTNode which caused the error
     * @param message The error message
     * @param fatal indicates if this is a fatal error
     */
    public static void error(final SourceUnit sourceUnit, final ASTNode astNode, final String message, final boolean fatal) {
        final SyntaxException syntaxException = new SyntaxException(message, astNode.getLineNumber(), astNode.getColumnNumber());
        final SyntaxErrorMessage syntaxErrorMessage = new SyntaxErrorMessage(syntaxException, sourceUnit);
        sourceUnit.getErrorCollector().addError(syntaxErrorMessage, fatal);
    }
    
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

        final MethodNode method = classNode.getMethod(GrailsNameUtils.getGetterName(propertyName), Parameter.EMPTY_ARRAY);
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
        MethodNode method = classNode.getDeclaredMethod(methodName, Parameter.EMPTY_ARRAY);
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

        while (parent != null && !getFullName(parent).equals("java.lang.Object") &&
               !parent.isResolved() && !Modifier.isAbstract(parent.getModifiers())) {
            classNode = parent;
            parent = parent.getSuperClass();
        }
        return classNode;
    }

    /**
     * Adds a delegate method to the target class node where the first argument
     * is to the delegate method is 'this'. In other words a method such as
     * foo(Object instance, String bar) would be added with a signature of foo(String)
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
     * Adds a delegate method to the target class node where the first argument
     * is to the delegate method is 'this'. In other words a method such as
     * foo(Object instance, String bar) would be added with a signature of foo(String)
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
        String methodName = declaredMethod.getName();
        if (classNode.hasDeclaredMethod(methodName, parameterTypes)) {
            return null;
        }
        String propertyName = GrailsClassUtils.getPropertyForGetter(methodName);
        if(propertyName != null && parameterTypes.length == 0 && classNode.hasProperty(propertyName)) {
            return null;
        }
        propertyName = GrailsClassUtils.getPropertyForSetter(methodName);
        if(propertyName != null && parameterTypes.length == 1 && classNode.hasProperty(propertyName)) {
            return null;
        }

        BlockStatement methodBody = new BlockStatement();
        ArgumentListExpression arguments = createArgumentListFromParameters(parameterTypes, thisAsFirstArgument);

        ClassNode returnType = nonGeneric(declaredMethod.getReturnType());

        MethodCallExpression methodCallExpression = new MethodCallExpression(delegate, methodName, arguments);
        methodCallExpression.setMethodTarget(declaredMethod);
        ThrowStatement missingMethodException = createMissingMethodThrowable(classNode, declaredMethod);
        VariableExpression apiVar = addApiVariableDeclaration(delegate, declaredMethod, methodBody);
        IfStatement ifStatement = createIfElseStatementForApiMethodCall(methodCallExpression, apiVar, missingMethodException);

        methodBody.addStatement(ifStatement);
        MethodNode methodNode = new MethodNode(methodName,
                Modifier.PUBLIC, returnType, copyParameters(parameterTypes),
                GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, methodBody);
        methodNode.addAnnotations(declaredMethod.getAnnotations());

        classNode.addMethod(methodNode);
        return methodNode;
    }

    private static IfStatement createIfElseStatementForApiMethodCall(MethodCallExpression methodCallExpression, VariableExpression apiVar, ThrowStatement missingMethodException) {
        BlockStatement ifBlock = new BlockStatement();
        ifBlock.addStatement(missingMethodException);
        BlockStatement elseBlock = new BlockStatement();
        elseBlock.addStatement(new ExpressionStatement(methodCallExpression));

        return new IfStatement(new BooleanExpression(new BinaryExpression(apiVar, EQUALS_OPERATOR, NULL_EXPRESSION)),ifBlock,elseBlock);
    }

    private static VariableExpression addApiVariableDeclaration(Expression delegate, MethodNode declaredMethod, BlockStatement methodBody) {
        VariableExpression apiVar = new VariableExpression("$api_" + declaredMethod.getName());
        DeclarationExpression de = new DeclarationExpression(apiVar, ASSIGNMENT_OPERATOR, delegate);
        methodBody.addStatement(new ExpressionStatement(de));
        return apiVar;
    }

    private static ThrowStatement createMissingMethodThrowable(ClassNode classNode, MethodNode declaredMethodNode) {
        ArgumentListExpression exceptionArgs = new ArgumentListExpression();
        exceptionArgs.addExpression(new ConstantExpression(declaredMethodNode.getName()));
        exceptionArgs.addExpression(new ClassExpression(classNode));
        return new ThrowStatement(new ConstructorCallExpression(MISSING_METHOD_EXCEPTION, exceptionArgs));
    }

    /**
     * Creates an argument list from the given parameter types.
     *
     * @param parameterTypes The parameter types
     * @param thisAsFirstArgument Whether to include a reference to 'this' as the first argument
     *
     * @return the arguments
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
        if (parameters.length == 0) {
            return GrailsArtefactClassInjector.ZERO_PARAMETERS;
        }

        Parameter[] newParameters = new Parameter[parameters.length - 1];
        System.arraycopy(parameters, 1, newParameters, 0, parameters.length - 1);
        return newParameters;
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
     * Adds a static method to the given class node that delegates to the given method
     * and resolves the object to invoke the method on from the given expression.
     *
     * @param expression The expression
     * @param classNode The class node
     * @param delegateMethod The delegate method
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateStaticMethod(Expression expression, ClassNode classNode, MethodNode delegateMethod) {
        Parameter[] parameterTypes = delegateMethod.getParameters();
        String declaredMethodName = delegateMethod.getName();
        if (classNode.hasDeclaredMethod(declaredMethodName, parameterTypes)) {
            return null;
        }

        BlockStatement methodBody = new BlockStatement();
        ArgumentListExpression arguments = new ArgumentListExpression();

        for (Parameter parameterType : parameterTypes) {
           arguments.addExpression(new VariableExpression(parameterType.getName()));
       }
        MethodCallExpression methodCallExpression = new MethodCallExpression(
                expression, declaredMethodName, arguments);
        methodCallExpression.setMethodTarget(delegateMethod);

        ThrowStatement missingMethodException = createMissingMethodThrowable(classNode, delegateMethod);
        VariableExpression apiVar = addApiVariableDeclaration(expression, delegateMethod, methodBody);
        IfStatement ifStatement = createIfElseStatementForApiMethodCall(methodCallExpression, apiVar, missingMethodException);

        methodBody.addStatement(ifStatement);
        ClassNode returnType = nonGeneric(delegateMethod.getReturnType());
        if (METHOD_MISSING_METHOD_NAME.equals(declaredMethodName)) {
            declaredMethodName = STATIC_METHOD_MISSING_METHOD_NAME;
        }
        MethodNode methodNode = classNode.getDeclaredMethod(declaredMethodName, parameterTypes);
        if(methodNode == null) {
            methodNode = new MethodNode(declaredMethodName,
                Modifier.PUBLIC | Modifier.STATIC,
                returnType, copyParameters(parameterTypes),
                GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, methodBody);
            methodNode.addAnnotations(delegateMethod.getAnnotations());

            classNode.addMethod(methodNode);
        }
        return methodNode;
    }

    /**
     * Adds or modifies an existing constructor to delegate to the
     * given static constructor method for initialization logic.
     *
     * @param classNode The class node
     * @param constructorMethod The constructor static method
     */
    public static void addDelegateConstructor(ClassNode classNode, MethodNode constructorMethod) {
        BlockStatement constructorBody = new BlockStatement();
        Parameter[] constructorParams = getRemainingParameterTypes(constructorMethod.getParameters());
        ArgumentListExpression arguments = createArgumentListFromParameters(constructorParams, true);
        MethodCallExpression constructCallExpression = new MethodCallExpression(
                new ClassExpression(constructorMethod.getDeclaringClass()), "initialize", arguments);
        constructCallExpression.setMethodTarget(constructorMethod);
        ExpressionStatement constructorInitExpression = new ExpressionStatement(constructCallExpression);
        if(constructorParams.length>0) {
            constructorBody.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.THIS, GrailsArtefactClassInjector.ZERO_ARGS)));
        }
        constructorBody.addStatement(constructorInitExpression);

        if (constructorParams.length == 0) {
            // handle default constructor

            ConstructorNode constructorNode = getDefaultConstructor(classNode);
            if (constructorNode != null) {
                List<AnnotationNode> annotations = constructorNode.getAnnotations(new ClassNode(GrailsDelegatingConstructor.class));
                if(annotations.size() == 0) {
                    Statement existingBodyCode = constructorNode.getCode();
                    if(existingBodyCode instanceof BlockStatement) {
                        ((BlockStatement) existingBodyCode).addStatement(constructorInitExpression);
                    }
                    else {
                        constructorNode.setCode(constructorBody);
                    }
                }
            } else {
                constructorNode = new ConstructorNode(Modifier.PUBLIC, constructorBody);
                classNode.addConstructor(constructorNode);
            }
            constructorNode.addAnnotation(new AnnotationNode(new ClassNode(GrailsDelegatingConstructor.class)));
        }
        else {
            // create new constructor, restoring default constructor if there is none
            ConstructorNode cn = findConstructor(classNode, constructorParams);
            if (cn == null) {
                cn = new ConstructorNode(Modifier.PUBLIC, copyParameters(constructorParams), null, constructorBody);
                classNode.addConstructor(cn);
            }
            else {
                List<AnnotationNode> annotations = cn.getAnnotations(new ClassNode(GrailsDelegatingConstructor.class));
                if(annotations.size() == 0) {
                    Statement code = cn.getCode();
                    constructorBody.addStatement(code);
                    cn.setCode(constructorBody);
                }
            }

            ConstructorNode defaultConstructor = getDefaultConstructor(classNode);
            if (defaultConstructor == null) {
                // add empty
                classNode.addConstructor(new ConstructorNode(Modifier.PUBLIC, new BlockStatement()));
            }
            cn.addAnnotation(new AnnotationNode(new ClassNode(GrailsDelegatingConstructor.class)));
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
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (!a[i].getType().equals(b[i].getType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Obtains the default constructor for the given class node.
     *
     * @param classNode The class node
     * @return The default constructor or null if there isn't one
     */
    public static ConstructorNode getDefaultConstructor(ClassNode classNode) {
        for (ConstructorNode cons : classNode.getDeclaredConstructors()) {
            if (cons.getParameters().length == 0) {
                return cons;
            }
        }
        return null;
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

    public static ClassNode nonGeneric(ClassNode type) {
        if (type.isUsingGenerics()) {
            final ClassNode nonGen = ClassHelper.makeWithoutCaching(type.getName());
            nonGen.setRedirect(type);
            nonGen.setGenericsTypes(null);
            nonGen.setUsingGenerics(false);
            return nonGen;
        }

        if (type.isArray()) {
            final ClassNode nonGen = ClassHelper.makeWithoutCaching(Object.class);
            nonGen.setUsingGenerics(false);
            return nonGen.makeArray();
        }

        return type.getPlainNodeReference();
    }

    public static boolean isCandidateInstanceMethod(ClassNode classNode, MethodNode declaredMethod) {
        Parameter[] parameterTypes = declaredMethod.getParameters();
        return isCandidateMethod(declaredMethod) && parameterTypes != null &&
            parameterTypes.length > 0 && isAssignableFrom(parameterTypes[0].getType(), classNode);
    }

    /**
     * Determines if the class or interface represented by the superClass
     * argument is either the same as, or is a superclass or superinterface of,
     * the class or interface represented by the specified subClass parameter.
     *
     * @param superClass The super class to check
     * @param childClass The sub class the check
     * @return true if the childClass is either equal to or a sub class of the specified superClass
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
                && Modifier.isPublic(declaredMethod.getModifiers()) &&
                !Modifier.isAbstract(declaredMethod.getModifiers());
    }

    public static boolean isConstructorMethod(MethodNode declaredMethod) {
        return declaredMethod.isStatic() && declaredMethod.isPublic() &&
                declaredMethod.getName().equals("initialize") &&
                declaredMethod.getParameters().length >= 1 &&
                declaredMethod.getParameters()[0].getType().equals(AbstractGrailsArtefactTransformer.OBJECT_CLASS);
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
            classNode.addField(fieldName, Modifier.PRIVATE, fieldType,
                    new ConstructorCallExpression(fieldType, new ArgumentListExpression()));
        }
    }

    public static void addAnnotationIfNecessary(ClassNode classNode, @SuppressWarnings("unused") Class<Entity> entityClass) {
        List<AnnotationNode> annotations = classNode.getAnnotations();
        ClassNode annotationClassNode = new ClassNode(Entity.class);
        AnnotationNode annotationToAdd = new AnnotationNode(annotationClassNode);
        if(annotations.isEmpty()) {
            classNode.addAnnotation(annotationToAdd);
        }
        else {
            boolean foundAnn = findAnnotation(annotationClassNode, annotations) != null;
            if(!foundAnn) classNode.addAnnotation(annotationToAdd);
        }
    }

    public static AnnotationNode findAnnotation(ClassNode annotationClassNode, List<AnnotationNode> annotations){
        for (AnnotationNode annotation : annotations) {
            if (annotation.getClassNode().equals(annotationClassNode)) {
                return annotation;
            }
        }
        return null;
    }
    
    /**
     * Returns true if classNode is marked with annotationClass
     * @param classNode A ClassNode to inspect
     * @param annotationClass an annotation to look for
     * @return true if classNode is marked with annotationClass, otherwise false
     */
    public static boolean hasAnnotation(final ClassNode classNode, final Class<? extends Annotation> annotationClass) {
        List<AnnotationNode> annotations = classNode.getAnnotations(new ClassNode(annotationClass));
        return annotations.size() > 0;
    }
    
    /**
     * @param classNode a ClassNode to search
     * @param annotationsToLookFor Annotations to look for
     * @return true if classNode is marked with any of the annotations in annotationsToLookFor
     */
    public static boolean hasAnyAnnotations(final ClassNode classNode, final Class<? extends Annotation>... annotationsToLookFor) {
        return CollectionUtils.exists(Arrays.asList(annotationsToLookFor), new Predicate() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public boolean evaluate(Object object) {
                return hasAnnotation(classNode, (Class)object);
            }
        });
    }

    public static void addMethodIfNotPresent(ClassNode controllerClassNode, MethodNode methodNode) {
        MethodNode existing = controllerClassNode.getMethod(methodNode.getName(), methodNode.getParameters());
        if(existing == null) {
            controllerClassNode.addMethod(methodNode);
        }
    }

    public static ExpressionStatement createPrintlnStatement(String message) {
        return new ExpressionStatement(new MethodCallExpression(AbstractGrailsArtefactTransformer.THIS_EXPRESSION,"println", new ArgumentListExpression(new ConstantExpression(message))));
    }

    public static ExpressionStatement createPrintlnStatement(String message, String variable) {
        return new ExpressionStatement(new MethodCallExpression(AbstractGrailsArtefactTransformer.THIS_EXPRESSION,"println", new ArgumentListExpression(new BinaryExpression(new ConstantExpression(message),Token.newSymbol(Types.PLUS, 0, 0),new VariableExpression(variable)))));
    }

    /**
     * Wraps a method body in try / catch logic that catches any errors and logs an error, but does not rethrow!
     *
     * @param methodNode The method node
     */
    public static void wrapMethodBodyInTryCatchDebugStatements(MethodNode methodNode) {
        BlockStatement code = (BlockStatement) methodNode.getCode();
        BlockStatement newCode = new BlockStatement();
        TryCatchStatement tryCatchStatement = new TryCatchStatement(code, new BlockStatement());
        newCode.addStatement(tryCatchStatement);
        methodNode.setCode(newCode);
        BlockStatement catchBlock = new BlockStatement();
        ArgumentListExpression logArguments = new ArgumentListExpression();
        logArguments.addExpression(new BinaryExpression(new ConstantExpression("Error initializing class: "),Token.newSymbol(Types.PLUS, 0, 0),new VariableExpression("e")));
        logArguments.addExpression(new VariableExpression("e"));
        catchBlock.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("log"), "error", logArguments)));
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(Throwable.class), "e"),catchBlock));
    }

    @Target(ElementType.CONSTRUCTOR)
    @Retention(RetentionPolicy.SOURCE)
    private static @interface GrailsDelegatingConstructor {}
}

