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

package org.codehaus.groovy.grails.compiler.injection.test;

import grails.test.mixin.TestMixin;
import grails.test.mixin.support.MixinMethod;
import grails.util.GrailsNameUtils;
import groovy.lang.GroovyObjectSupport;
import junit.framework.TestCase;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.junit.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * An AST transformation to be applied to tests for adding behavior to a target test class
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class TestMixinTransformation implements ASTTransformation{
    public static final AnnotationNode MIXIN_METHOD_ANNOTATION = new AnnotationNode(new ClassNode(MixinMethod.class));
    private static final ClassNode MY_TYPE = new ClassNode(TestMixin.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    public static final String OBJECT_CLASS = "java.lang.Object";
    public static final String SPEC_CLASS = "spock.lang.Specification";
    private static final String JUNIT3_CLASS = "junit.framework.TestCase";
    public static final String SET_UP_METHOD = "setUp";
    public static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    public static final String TEAR_DOWN_METHOD = "tearDown";
    public static final ClassNode GROOVY_OBJECT_CLASS_NODE = new ClassNode(GroovyObjectSupport.class);

    public void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode node = (AnnotationNode) astNodes[0];
        if (!MY_TYPE.equals(node.getClassNode()) || !(parent instanceof ClassNode)) {
            return;
        }

        ClassNode classNode = (ClassNode) parent;
        String cName = classNode.getName();
        if (classNode.isInterface()) {
            throw new RuntimeException("Error processing interface '" + cName + "'. " +
                    MY_TYPE_NAME + " not allowed for interfaces.");
        }

        ListExpression values = getListOfClasses(node);

        weaveMixinsIntoClass(classNode, values);


    }

    protected ListExpression getListOfClasses(AnnotationNode node) {
        Expression value = node.getMember("value");
        ListExpression values = null;
        if (value instanceof ListExpression) {
            values = (ListExpression) value;
        }
        else if (value instanceof ClassExpression) {
            values = new ListExpression();
            values.addExpression(value);
        }
        return values;
    }

    public void weaveMixinsIntoClass(ClassNode classNode, ListExpression values) {
        if (values != null) {
            boolean isJunit3 = isJunit3Test(classNode);
            List<MethodNode> beforeMethods = null;
            List<MethodNode> afterMethods = null;
            if (isJunit3) {
                beforeMethods = new ArrayList<MethodNode>();
                afterMethods = new ArrayList<MethodNode>();
            }
            for (Expression current : values.getExpressions()) {
                if (current instanceof ClassExpression) {
                    ClassExpression ce = (ClassExpression) current;

                    ClassNode mixinClassNode = ce.getType();

                    final String fieldName = '$' + GrailsNameUtils.getPropertyName(mixinClassNode.getName());

                    GrailsASTUtils.addFieldIfNonExistent(classNode, mixinClassNode, fieldName);
                    VariableExpression fieldReference = new VariableExpression(fieldName);

                    while (!mixinClassNode.getName().equals(OBJECT_CLASS)) {
                        final List<MethodNode> mixinMethods = mixinClassNode.getMethods();

                        int beforeClassMethodCount = 0;
                        int afterClassMethodCount = 0;
                        for (MethodNode mixinMethod : mixinMethods) {
                            if (isCandidateMethod(mixinMethod) && !hasDeclaredMethod(classNode, mixinMethod)) {
                                if (mixinMethod.isStatic()) {
                                    MethodNode methodNode = GrailsASTUtils.addDelegateStaticMethod(classNode, mixinMethod);
                                    if (methodNode != null) {
                                        methodNode.addAnnotation(MIXIN_METHOD_ANNOTATION);
                                    }
                                }
                                else {
                                    MethodNode methodNode = GrailsASTUtils.addDelegateInstanceMethod(classNode, fieldReference, mixinMethod, false);
                                    if (methodNode != null) {
                                        methodNode.addAnnotation(MIXIN_METHOD_ANNOTATION);
                                    }
                                }
                                if (isJunit3) {

                                    if (hasAnnotation(mixinMethod, Before.class)) {
                                        beforeMethods.add(mixinMethod);
                                    }
                                    if (hasAnnotation(mixinMethod, BeforeClass.class)) {
                                        beforeMethods.add(beforeClassMethodCount++, mixinMethod);
                                    }
                                    if (hasAnnotation(mixinMethod, After.class)) {
                                        afterMethods.add(mixinMethod);
                                    }
                                    if (hasAnnotation(mixinMethod, AfterClass.class)) {
                                        afterMethods.add(afterClassMethodCount++, mixinMethod);
                                    }
                                }
                            }
                        }

                        mixinClassNode = mixinClassNode.getSuperClass();
                    }
                }
            }

            if (isJunit3) {
                addMethodCallsToMethod(classNode, SET_UP_METHOD, beforeMethods);
                addMethodCallsToMethod(classNode, TEAR_DOWN_METHOD, afterMethods);
            }
        }
    }

    protected boolean hasDeclaredMethod(ClassNode classNode, MethodNode mixinMethod) {
        return classNode.hasDeclaredMethod(mixinMethod.getName(), mixinMethod.getParameters());
    }

    protected boolean hasAnnotation(MethodNode mixinMethod, Class<?> beforeClass) {
        return !mixinMethod.getAnnotations(new ClassNode(beforeClass)).isEmpty();
    }

    protected void addMethodCallsToMethod(ClassNode classNode, String name, List<MethodNode> methods) {
        if (methods != null && !methods.isEmpty()) {
            BlockStatement setupMethodBody = getOrCreateNoArgsMethodBody(classNode, name);
            for (MethodNode beforeMethod : methods) {
                setupMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, beforeMethod.getName(), GrailsArtefactClassInjector.ZERO_ARGS)));
            }
        }
    }

    protected BlockStatement getOrCreateNoArgsMethodBody(ClassNode classNode, String name) {
        MethodNode setupMethod = classNode.getMethod(name, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        return getOrCreateMethodBody(classNode, setupMethod, name);
    }

    protected BlockStatement getOrCreateMethodBody(ClassNode classNode, MethodNode setupMethod, String name) {
        BlockStatement methodBody;
        if (setupMethod.getDeclaringClass().getName().equals(TestCase.class.getName())) {
            methodBody = new BlockStatement();
            setupMethod = new MethodNode(name, Modifier.PUBLIC,setupMethod.getReturnType(), GrailsArtefactClassInjector.ZERO_PARAMETERS,null, methodBody);
            classNode.addMethod(setupMethod);
        }
        else {
            final Statement setupMethodBody = setupMethod.getCode();
            if (!(setupMethodBody instanceof BlockStatement)) {
                methodBody = new BlockStatement();
                if (setupMethodBody != null) {
                    if (!(setupMethodBody instanceof ReturnStatement)) {
                        methodBody.addStatement(setupMethodBody);
                    }
                }
                setupMethod.setCode(methodBody);
            }
            else {
                methodBody = (BlockStatement) setupMethodBody;
            }
        }
        return methodBody;
    }

    protected boolean isJunit3Test(ClassNode classNode) {
        return isSubclassOf(classNode, JUNIT3_CLASS);
    }

    protected boolean isSpockTest(ClassNode classNode) {
        return isSubclassOf(classNode, SPEC_CLASS);
    }

    private boolean isSubclassOf(ClassNode classNode, String testType) {
        ClassNode currentSuper = classNode.getSuperClass();
        while (currentSuper != null && !currentSuper.getName().equals(OBJECT_CLASS)) {
            if (currentSuper.getName().equals(testType)) return true;
            currentSuper = currentSuper.getSuperClass();
        }
        return false;
    }

    protected boolean isCandidateMethod(MethodNode declaredMethod) {
        ClassNode groovyMethods = GROOVY_OBJECT_CLASS_NODE;
        String methodName = declaredMethod.getName();
        return !declaredMethod.isSynthetic() &&
                !methodName.contains("$") &&
                Modifier.isPublic(declaredMethod.getModifiers()) &&
                !Modifier.isAbstract(declaredMethod.getModifiers()) &&
                !groovyMethods.hasMethod(declaredMethod.getName(), declaredMethod.getParameters());
    }
}
