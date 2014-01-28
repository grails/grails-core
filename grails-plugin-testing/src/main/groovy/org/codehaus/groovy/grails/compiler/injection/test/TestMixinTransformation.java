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
import grails.test.mixin.TestMixinTargetAware;
import grails.test.mixin.TestRuntimeAwareMixin;
import grails.test.mixin.support.MixinInstance;
import grails.test.mixin.support.MixinMethod;
import grails.test.runtime.TestRuntimeJunitAdapter;
import grails.util.GrailsNameUtils;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyObjectSupport;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.spockframework.runtime.model.FieldMetadata;

import spock.lang.Shared;

/**
 * An AST transformation to be applied to tests for adding behavior to a target test class.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class TestMixinTransformation implements ASTTransformation{
    private static final String RULE_FIELD_NAME_BASE = "$testRuntime";
    private static final String JUNIT_ADAPTER_FIELD_NAME = RULE_FIELD_NAME_BASE + "JunitAdapter";
    private static final String JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY = "JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY";  
    public static final AnnotationNode MIXIN_METHOD_ANNOTATION = new AnnotationNode(new ClassNode(MixinMethod.class));
    private static final ClassNode MY_TYPE = new ClassNode(TestMixin.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    public static final String OBJECT_CLASS = "java.lang.Object";
    public static final String SPEC_CLASS = "spock.lang.Specification";
    private static final String JUNIT3_CLASS = "junit.framework.TestCase";
    public static final String SET_UP_METHOD = "setUp";
    public static final String TEAR_DOWN_METHOD = "tearDown";
    public static final ClassNode GROOVY_OBJECT_CLASS_NODE = new ClassNode(GroovyObjectSupport.class);
    public static final AnnotationNode TEST_ANNOTATION = new AnnotationNode(new ClassNode(Test.class));
    public static final String VOID_TYPE = "void";
    private static final String EMC_STATEMENT_ADDED_KEY = "EMC_STATEMENT_ADDED_KEY";

    public void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0];
        if (!MY_TYPE.equals(annotationNode.getClassNode()) || !(parent instanceof ClassNode)) {
            return;
        }

        ClassNode classNode = (ClassNode) parent;
        ListExpression values = getListOfClasses(annotationNode);
        weaveTestMixins(classNode, values);
        addEnableEMCStatement(classNode);
    }

    protected void addEnableEMCStatement(ClassNode classNode) {
        if (classNode.redirect().getNodeMetaData(EMC_STATEMENT_ADDED_KEY) != Boolean.TRUE) {
            List<Statement> statements = new ArrayList<Statement>();
            ClassNode emcClassNode = ClassHelper.make(ExpandoMetaClass.class);
            // make direct static call to ExpandoMetaClass.enableGlobally() is static initializer block
            statements.add(new ExpressionStatement(GrailsASTUtils.applyDefaultMethodTarget(new MethodCallExpression(
                    new ClassExpression(emcClassNode), "enableGlobally", MethodCallExpression.NO_ARGUMENTS),
                    emcClassNode)));
            classNode.addStaticInitializerStatements(statements, true);
            classNode.redirect().setNodeMetaData(EMC_STATEMENT_ADDED_KEY, Boolean.TRUE);
        }
    }

    /**
     * @param classNode The class node to weave into
     * @param values A list of ClassExpression instances
     */
    public void weaveTestMixins(ClassNode classNode, ListExpression values) {
        String cName = classNode.getName();
        if (classNode.isInterface()) {
            throw new RuntimeException("Error processing interface '" + cName + "'. " +
                    MY_TYPE_NAME + " not allowed for interfaces.");
        }

        autoAnnotateSetupTeardown(classNode);
        autoAddTestAnnotation(classNode);

        weaveMixinsIntoClass(classNode, values);
    }

    private void autoAddTestAnnotation(ClassNode classNode) {
        if(isSpockTest(classNode)) return;
        Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
        for (String methodName : declaredMethodsMap.keySet()) {
            MethodNode methodNode = declaredMethodsMap.get(methodName);
            ClassNode testAnnotationClassNode = TEST_ANNOTATION.getClassNode();
            List<AnnotationNode> existingTestAnnotations = methodNode.getAnnotations(testAnnotationClassNode);
            if (isCandidateMethod(methodNode) && (methodNode.getName().startsWith("test") || existingTestAnnotations.size()>0)) {
                if (existingTestAnnotations.size()==0) {
                    ClassNode returnType = methodNode.getReturnType();
                    if (returnType.getName().equals(VOID_TYPE)) {
                        methodNode.addAnnotation(TEST_ANNOTATION);
                    }
                }
            }
        }

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
        if (values == null) {
            return;
        }

        Junit3TestFixtureMethodHandler junit3MethodHandler = isJunit3Test(classNode) ? new Junit3TestFixtureMethodHandler(classNode) : null; 

        for (Expression current : values.getExpressions()) {
            if (current instanceof ClassExpression) {
                ClassExpression ce = (ClassExpression) current;
                ClassNode mixinClassNode = ce.getType();
                weaveMixinIntoClass(classNode, mixinClassNode, junit3MethodHandler);
            }
        }

        if (junit3MethodHandler != null) {
            junit3MethodHandler.postProcessClassNode();
        }
    }

    protected void weaveMixinIntoClass(final ClassNode classNode, final ClassNode mixinClassNode,
            final Junit3TestFixtureMethodHandler junit3MethodHandler) {
        final String fieldName = '$' + GrailsNameUtils.getPropertyName(mixinClassNode.getName());
        
        boolean implementsTestRuntimeAwareMixin = GrailsASTUtils.findInterface(mixinClassNode, ClassHelper.make(TestRuntimeAwareMixin.class)) != null;
        
        FieldNode mixinFieldNode = null;
        if(!implementsTestRuntimeAwareMixin) { 
            mixinFieldNode = addLegacyMixinFieldIfNonExistent(classNode, mixinClassNode, fieldName);
        } else {
            mixinFieldNode = addTestRuntimeAwareMixinFieldIfNonExistent(classNode, mixinClassNode, fieldName);
        }

        if (mixinFieldNode == null) return; // already woven
        
        FieldExpression fieldReference = new FieldExpression(mixinFieldNode);

        ClassNode currentMixinClassNode = mixinClassNode; 
        while (!currentMixinClassNode.getName().equals(OBJECT_CLASS)) {
            final List<MethodNode> mixinMethods = currentMixinClassNode.getMethods();

            for (MethodNode mixinMethod : mixinMethods) {
                if (!isCandidateMethod(mixinMethod) || hasDeclaredMethod(classNode, mixinMethod)) {
                    continue;
                }

                MethodNode methodNode;
                if (mixinMethod.isStatic()) {
                    methodNode = GrailsASTUtils.addDelegateStaticMethod(classNode, mixinMethod);
                }
                else {
                    methodNode = GrailsASTUtils.addDelegateInstanceMethod(classNode, fieldReference, mixinMethod, false);
                }
                if (methodNode != null) {
                    methodNode.addAnnotation(MIXIN_METHOD_ANNOTATION);
                    GrailsASTUtils.addCompileStaticAnnotation(methodNode);
                }

                if (junit3MethodHandler != null) {
                    junit3MethodHandler.handle(mixinMethod, methodNode);
                }
            }

            currentMixinClassNode = currentMixinClassNode.getSuperClass();
            if (junit3MethodHandler != null) {
                junit3MethodHandler.mixinSuperClassChanged();
            }
        }
    }
    
    protected FieldNode addTestRuntimeAwareMixinFieldIfNonExistent(ClassNode classNode, ClassNode fieldType,
            String fieldName) {
        if (classNode == null || classNode.getField(fieldName) != null) {
            return null;
        }
        
        MapExpression constructorArguments = new MapExpression();
        constructorArguments.addMapEntryExpression(new MapEntryExpression(new ConstantExpression("testClass"), new ClassExpression(classNode)));
        FieldNode mixinInstanceFieldNode = classNode.addField(fieldName, Modifier.STATIC, fieldType, new ConstructorCallExpression(fieldType, constructorArguments));
        mixinInstanceFieldNode.addAnnotation(new AnnotationNode(ClassHelper.make(MixinInstance.class)));
        
        addJunitRuleFields(classNode);
        
        return mixinInstanceFieldNode;
    }

    protected void addJunitRuleFields(ClassNode classNode) {
        if(classNode.getField(JUNIT_ADAPTER_FIELD_NAME) != null) {
            return;
        }
        ClassNode junitAdapterType = ClassHelper.make(TestRuntimeJunitAdapter.class);
        FieldNode junitAdapterFieldNode = classNode.addField(JUNIT_ADAPTER_FIELD_NAME, Modifier.STATIC, junitAdapterType, new ConstructorCallExpression(junitAdapterType, MethodCallExpression.NO_ARGUMENTS));
        boolean spockTest = isSpockTest(classNode);
        FieldNode staticRuleFieldNode = classNode.addField(RULE_FIELD_NAME_BASE + "StaticClassRule", Modifier.PRIVATE | Modifier.STATIC, ClassHelper.make(TestRule.class), new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), "newClassRule", MethodCallExpression.NO_ARGUMENTS));
        AnnotationNode classRuleAnnotation = new AnnotationNode(ClassHelper.make(ClassRule.class));
        if(spockTest) {
            // @ClassRule must be added to @Shared field in spock
            FieldNode spockSharedRuleFieldNode = classNode.addField(RULE_FIELD_NAME_BASE + "SharedClassRule", Modifier.PUBLIC, ClassHelper.make(TestRule.class), new FieldExpression(staticRuleFieldNode));
            spockSharedRuleFieldNode.addAnnotation(classRuleAnnotation);
            spockSharedRuleFieldNode.addAnnotation(new AnnotationNode(ClassHelper.make(Shared.class)));
            if(spockTest) {
                addSpockFieldMetadata(spockSharedRuleFieldNode, 0);
            }
        } else {
            staticRuleFieldNode.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            staticRuleFieldNode.addAnnotation(classRuleAnnotation);
        }

        FieldNode ruleFieldNode = classNode.addField(RULE_FIELD_NAME_BASE + "Rule", Modifier.PUBLIC, ClassHelper.make(TestRule.class), new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), "newRule", MethodCallExpression.NO_ARGUMENTS));
        ruleFieldNode.addAnnotation(new AnnotationNode(ClassHelper.make(Rule.class)));
        if(spockTest) {
            addSpockFieldMetadata(ruleFieldNode, 0);
        }
    }
    
    private void addSpockFieldMetadata(FieldNode field, int ordinal) {
        AnnotationNode ann = new AnnotationNode(ClassHelper.make(FieldMetadata.class));
        ann.setMember(FieldMetadata.NAME, new ConstantExpression(field.getName()));
        ann.setMember(FieldMetadata.ORDINAL, new ConstantExpression(ordinal));
        ann.setMember(FieldMetadata.LINE, new ConstantExpression(field.getLineNumber()));
        field.addAnnotation(ann);
      }

    private static class Junit3TestFixtureMethodHandler {
        ClassNode classNode;
        List<MethodNode> beforeMethods = new ArrayList<MethodNode>();
        List<MethodNode> afterMethods = new ArrayList<MethodNode>();
        int beforeClassMethodCount = 0;
        int afterClassMethodCount = 0;
        boolean hasExistingSetUp;
        boolean hasExistingTearDown;
        
        public Junit3TestFixtureMethodHandler(ClassNode classNode) {
            this.classNode=classNode;
            hasExistingSetUp = classNode.hasDeclaredMethod(SET_UP_METHOD, Parameter.EMPTY_ARRAY);
            hasExistingTearDown = classNode.hasDeclaredMethod(TEAR_DOWN_METHOD, Parameter.EMPTY_ARRAY);
        }

        public void mixinSuperClassChanged() {
            beforeClassMethodCount = 0;
            afterClassMethodCount = 0;
        }
        
        public void handle(MethodNode mixinMethod, MethodNode weavedMethod) {
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
        
        public void postProcessClassNode() {
            addMethodCallsToMethod(classNode, SET_UP_METHOD, beforeMethods);
            addMethodCallsToMethod(classNode, TEAR_DOWN_METHOD, afterMethods);
            handleTestRuntimeJunitSetUpAndTearDownCalls();
        }

        private void handleTestRuntimeJunitSetUpAndTearDownCalls() {
            FieldNode junitAdapterFieldNode = classNode.getDeclaredField(JUNIT_ADAPTER_FIELD_NAME);
            if(junitAdapterFieldNode==null) {
                return;
            }

            // add rule calls to junit setup/teardown only once, there might be several test mixins applied for the same class
            if(classNode.redirect().getNodeMetaData(JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY) != Boolean.TRUE) {
                BlockStatement setUpMethodBody = getOrCreateNoArgsMethodBody(classNode, SET_UP_METHOD);
                if(!hasExistingSetUp) {
                    setUpMethodBody.getStatements().add(0, new ExpressionStatement(new MethodCallExpression(new VariableExpression("super"), SET_UP_METHOD, GrailsArtefactClassInjector.ZERO_ARGS)));
                }
                BlockStatement tearDownMethodBody = getOrCreateNoArgsMethodBody(classNode, TEAR_DOWN_METHOD);
                setUpMethodBody.getStatements().add(1, new ExpressionStatement(new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), SET_UP_METHOD, new VariableExpression("this"))));
                tearDownMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), TEAR_DOWN_METHOD, new VariableExpression("this"))));
                if(!hasExistingTearDown) {
                    tearDownMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("super"), TEAR_DOWN_METHOD, GrailsArtefactClassInjector.ZERO_ARGS)));
                }
                classNode.redirect().setNodeMetaData(JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY, Boolean.TRUE);
            }
        }
    }


    static protected FieldNode addLegacyMixinFieldIfNonExistent(ClassNode classNode, ClassNode fieldType, String fieldName) {
        ClassNode targetAwareInterface = GrailsASTUtils.findInterface(fieldType, new ClassNode(TestMixinTargetAware.class).getPlainNodeReference());
        if (classNode != null && classNode.getField(fieldName) == null) {
            Expression constructorArgument = new ArgumentListExpression();
            if(targetAwareInterface != null) {
                MapExpression namedArguments = new MapExpression();
                namedArguments.addMapEntryExpression(new MapEntryExpression(new ConstantExpression("target"), new VariableExpression("this")));
                constructorArgument = namedArguments;
            }
            return classNode.addField(fieldName, Modifier.PRIVATE, fieldType,
                new ConstructorCallExpression(fieldType, constructorArgument));
        }
        return null;
    }

    static protected boolean hasDeclaredMethod(ClassNode classNode, MethodNode mixinMethod) {
        return classNode.hasDeclaredMethod(mixinMethod.getName(), mixinMethod.getParameters());
    }

    static protected boolean hasAnnotation(MethodNode mixinMethod, Class<?> beforeClass) {
        return !mixinMethod.getAnnotations(new ClassNode(beforeClass)).isEmpty();
    }

    static protected void addMethodCallsToMethod(ClassNode classNode, String name, List<MethodNode> methods) {
        if (methods != null && !methods.isEmpty()) {
            BlockStatement setupMethodBody = getOrCreateNoArgsMethodBody(classNode, name);
            for (MethodNode beforeMethod : methods) {
                setupMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("this"), beforeMethod.getName(), GrailsArtefactClassInjector.ZERO_ARGS)));
            }
        }
    }

    static protected BlockStatement getOrCreateNoArgsMethodBody(ClassNode classNode, String name) {
        MethodNode setupMethod = classNode.getMethod(name, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        return getOrCreateMethodBody(classNode, setupMethod, name);
    }

    static protected BlockStatement getOrCreateMethodBody(ClassNode classNode, MethodNode methodNode, String name) {
        BlockStatement methodBody;
        if (!methodNode.getDeclaringClass().equals(classNode)) {
            methodBody = new BlockStatement();
            methodNode = new MethodNode(name, Modifier.PUBLIC, methodNode.getReturnType(), GrailsArtefactClassInjector.ZERO_PARAMETERS, null, methodBody);
            classNode.addMethod(methodNode);
        }
        else {
            final Statement setupMethodBody = methodNode.getCode();
            if (!(setupMethodBody instanceof BlockStatement)) {
                methodBody = new BlockStatement();
                if (setupMethodBody != null) {
                    if (!(setupMethodBody instanceof ReturnStatement)) {
                        methodBody.addStatement(setupMethodBody);
                    }
                }
                methodNode.setCode(methodBody);
            }
            else {
                methodBody = (BlockStatement) setupMethodBody;
            }
        }
        return methodBody;
    }

    public static boolean isJunit3Test(ClassNode classNode) {
        return isSubclassOf(classNode, JUNIT3_CLASS);
    }

    public static boolean isSpockTest(ClassNode classNode) {
        return isSubclassOf(classNode, SPEC_CLASS);
    }

    private static boolean isSubclassOf(ClassNode classNode, String testType) {
        ClassNode currentSuper = classNode.getSuperClass();
        while (currentSuper != null && !currentSuper.getName().equals(OBJECT_CLASS)) {
            if (currentSuper.getName().equals(testType)) return true;
            currentSuper = currentSuper.getSuperClass();
        }
        return false;
    }

    protected boolean isCandidateMethod(MethodNode declaredMethod) {
        return isAddableMethod(declaredMethod) && 
                !hasSimilarMethod(declaredMethod, ClassHelper.make(TestRuntimeAwareMixin.class));
    }

    public static boolean isAddableMethod(MethodNode declaredMethod) {
        ClassNode groovyMethods = GROOVY_OBJECT_CLASS_NODE;
        String methodName = declaredMethod.getName();
        return !declaredMethod.isSynthetic() &&
                !methodName.contains("$") &&
                Modifier.isPublic(declaredMethod.getModifiers()) &&
                !Modifier.isAbstract(declaredMethod.getModifiers()) &&
                !hasSimilarMethod(declaredMethod, groovyMethods);
    }

    protected static boolean hasSimilarMethod(MethodNode declaredMethod, ClassNode groovyMethods) {
        return groovyMethods.hasMethod(declaredMethod.getName(), declaredMethod.getParameters());
    }

    protected void error(SourceUnit source, String me) {
        source.getErrorCollector().addError(new SimpleMessage(me,source), true);
    }

    protected void autoAnnotateSetupTeardown(ClassNode classNode) {
        MethodNode setupMethod = classNode.getDeclaredMethod(SET_UP_METHOD, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if ( setupMethod != null && setupMethod.getAnnotations(TestForTransformation.BEFORE_CLASS_NODE).size() == 0) {
            setupMethod.addAnnotation(TestForTransformation.BEFORE_ANNOTATION);
        }

        MethodNode tearDown = classNode.getDeclaredMethod(TEAR_DOWN_METHOD, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if ( tearDown != null && tearDown.getAnnotations(TestForTransformation.AFTER_CLASS_NODE).size() == 0) {
            tearDown.addAnnotation(TestForTransformation.AFTER_ANNOTATION);
        }
    }
}
