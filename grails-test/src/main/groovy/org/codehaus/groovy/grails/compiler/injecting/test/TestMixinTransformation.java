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

package org.codehaus.groovy.grails.compiler.injecting.test;

import grails.test.mixin.TestMixin;
import grails.util.GrailsNameUtils;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    private static final ClassNode MY_TYPE = new ClassNode(TestMixin.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    public static final String OBJECT_CLASS = "java.lang.Object";
    public static final String SPEC_CLASS = "spock.lang.Specification";
    private static final String JUNIT3_CLASS = "junit.framework.TestCase" ;
    public static final String SET_UP_METHOD = "setUp";
    public static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    public static final String TEAR_DOWN_METHOD = "tearDown";


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

        Expression value = node.getMember("value");

        if(value instanceof ClassExpression) {
            ClassExpression ce = (ClassExpression) value;

            final ClassNode mixinClassNode = ce.getType();

            final String fieldName = '$' + GrailsNameUtils.getPropertyName(mixinClassNode.getName());
            classNode.addField(fieldName, Modifier.PRIVATE, mixinClassNode, new ConstructorCallExpression(mixinClassNode, new ArgumentListExpression()));
            VariableExpression fieldReference = new VariableExpression(fieldName);


            final List<MethodNode> mixinMethods = mixinClassNode.getMethods();

            boolean isJunit3 = isJunit3Test(classNode);

            List<MethodNode> beforeMethods = null;
            List<MethodNode> afterMethods = null;
            if (!isJunit3) {
                beforeMethods = new ArrayList<MethodNode>();
                afterMethods = new ArrayList<MethodNode>();
            }

            for (MethodNode mixinMethod : mixinMethods) {
                if(isCandidateMethod(mixinMethod) && !classNode.hasDeclaredMethod(mixinMethod.getName(), mixinMethod.getParameters())) {
                    GrailsASTUtils.addDelegateInstanceMethod(classNode,fieldReference, mixinMethod);
                    if(!isJunit3) {

                        if(!mixinMethod.getAnnotations(new ClassNode(Before.class)).isEmpty()) {
                             beforeMethods.add(mixinMethod);
                        }
                        if(!mixinMethod.getAnnotations(new ClassNode(After.class)).isEmpty()) {
                             afterMethods.add(mixinMethod);
                        }
                    }
                }
            }

            if(!isJunit3) {
                addMethodCallsToMethod(classNode, SET_UP_METHOD, beforeMethods);
                addMethodCallsToMethod(classNode, TEAR_DOWN_METHOD, afterMethods);
            }
        }
    }

    private void addMethodCallsToMethod(ClassNode classNode, String name, List<MethodNode> methods) {
        MethodNode setupMethod = classNode.getMethod(name, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        BlockStatement setupMethodBody = getOrCreateMethodBody(classNode, setupMethod, name);
        for (MethodNode beforeMethod : methods) {
            setupMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, beforeMethod.getName(), GrailsArtefactClassInjector.ZERO_ARGS)));
        }
    }

    private BlockStatement getOrCreateMethodBody(ClassNode classNode, MethodNode setupMethod, String name) {
        BlockStatement methodBody;
        if(setupMethod == null) {
            methodBody = new BlockStatement();
            setupMethod = new MethodNode(name, Modifier.PUBLIC,null, GrailsArtefactClassInjector.ZERO_PARAMETERS,null, methodBody);
            classNode.addMethod(setupMethod);
        }
        else {
            final Statement setupMethodBody = setupMethod.getCode();
            if(!(setupMethodBody instanceof BlockStatement)) {
                methodBody = new BlockStatement();
                if(setupMethodBody != null) {
                    methodBody.addStatement(setupMethodBody);
                }
                setupMethod.setCode(methodBody);
            }
            else {
                methodBody = (BlockStatement) setupMethodBody;
            }


        }
        return methodBody;
    }

    private boolean isJunit3Test(ClassNode classNode) {
        return isSubclassOf(classNode, JUNIT3_CLASS);
    }

    private boolean isSpockTest(ClassNode classNode) {
        return isSubclassOf(classNode, SPEC_CLASS);
    }

    private boolean isSubclassOf(ClassNode classNode, String testType) {
        ClassNode currentSuper = classNode.getSuperClass();
        while(currentSuper != null && !currentSuper.getName().equals(OBJECT_CLASS)) {
            if(currentSuper.getName().equals(testType)) return true;
        }
        return false;
    }

    private boolean isJunit4Test(ClassNode classNode) {
        return !classNode.getAnnotations(new ClassNode(Test.class)).isEmpty();
    }

    protected boolean isCandidateMethod(MethodNode declaredMethod) {
        return !declaredMethod.isSynthetic() &&
                !declaredMethod.getName().contains("$")
                && Modifier.isPublic(declaredMethod.getModifiers()) && !Modifier.isAbstract(declaredMethod.getModifiers());
    }

}
