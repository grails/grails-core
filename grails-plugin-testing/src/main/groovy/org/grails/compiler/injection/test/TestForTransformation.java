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
package org.grails.compiler.injection.test;

import grails.compiler.ast.GrailsArtefactClassInjector;
import grails.test.mixin.TestFor;
import grails.test.mixin.domain.DomainClassUnitTestMixin;
import grails.test.mixin.support.MixinMethod;
import grails.test.mixin.support.TestMixinRegistrar;
import grails.test.mixin.support.TestMixinRegistry;
import grails.util.BuildSettings;
import grails.util.GrailsNameUtils;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.compiler.logging.LoggingTransformer;
import org.grails.core.io.DefaultResourceLocator;
import org.grails.core.io.ResourceLocator;
import org.grails.core.io.support.GrailsFactoriesLoader;
import org.grails.io.support.FileSystemResource;
import org.grails.io.support.GrailsResourceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Transformation used by the {@link grails.test.mixin.TestFor} annotation to signify the
 * class under test.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings("rawtypes")
public class TestForTransformation extends TestMixinTransformation implements TestMixinRegistry {

    private static final ClassNode MY_TYPE = new ClassNode(TestFor.class);
    private static final Token ASSIGN = Token.newSymbol("=", -1, -1);

    protected static final Map<String, Class> artefactTypeToTestMap = new HashMap<String, Class>();
    static {
        List<TestMixinRegistrar> registrars = GrailsFactoriesLoader.loadFactories(TestMixinRegistrar.class);
        TestMixinRegistry thisRegistry = new TestMixinRegistry() {
            @Override
            public void registerMixin(String artefactType, Class mixin) {
                artefactTypeToTestMap.put(artefactType, mixin);
            }
        };
        for(TestMixinRegistrar registrar : registrars) {
            registrar.registerTestMixins(thisRegistry);
        }
    }

    public static final String DOMAIN_TYPE = "Domain";
    public static final ClassNode BEFORE_CLASS_NODE = new ClassNode(Before.class);
    public static final AnnotationNode BEFORE_ANNOTATION = new AnnotationNode(BEFORE_CLASS_NODE);

    public static final ClassNode AFTER_CLASS_NODE = new ClassNode(After.class);
    public static final AnnotationNode AFTER_ANNOTATION = new AnnotationNode(AFTER_CLASS_NODE);

    public static final AnnotationNode TEST_ANNOTATION = new AnnotationNode(new ClassNode(Test.class));
    private static final String GROOVY_TEST_CASE_CLASS_NAME = "groovy.util.GroovyTestCase";
    public static final String VOID_TYPE = "void";

    private ResourceLocator resourceLocator;

    public ResourceLocator getResourceLocator() {
        if (resourceLocator == null) {
            resourceLocator = new DefaultResourceLocator();
            String basedir = BuildSettings.BASE_DIR.getAbsolutePath();
            resourceLocator.setSearchLocation(basedir);
        }
        return resourceLocator;
    }

    @Override
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
        if (classNode.isInterface() || Modifier.isAbstract(classNode.getModifiers())) {
            return;
        }

        boolean junit3Test = isJunit3Test(classNode);
        boolean spockTest = isSpockTest(classNode);
        boolean isJunit = !junit3Test && !spockTest;

        if (!junit3Test && !spockTest && !isJunit) return;

        handleTestForAnnotation(classNode, source, node, junit3Test);
    }

    protected void handleTestForAnnotation(ClassNode classNode, SourceUnit source, AnnotationNode testForAnnotationNode, boolean junit3Test) {
        Expression value = testForAnnotationNode.getMember("value");
        ClassExpression ce;
        if (value instanceof ClassExpression) {
            ce = (ClassExpression) value;
            testFor(classNode, ce);
            return;
        }

        if (junit3Test) {
            return;
        }

        List<AnnotationNode> annotations = classNode.getAnnotations(MY_TYPE);
        if (annotations.size()>0) return; // bail out, in this case it was already applied as a local transform

        annotations = classNode.getAnnotations(TestMixinTransformation.MY_TYPE);
        if (annotations.size()>0) return; // bail out, another TestMixin transform already defines behavior

        // no explicit class specified try by convention
        String fileName = source.getName();
        String className = GrailsResourceUtils.getClassName(new FileSystemResource(fileName));
        if (className == null) {
            return;
        }

        String targetClassName = null;

        if (className.endsWith("Tests")) {
            targetClassName = className.substring(0, className.indexOf("Tests"));
        }
        else if (className.endsWith("Test")) {
            targetClassName = className.substring(0, className.indexOf("Test"));
        }
        else if (className.endsWith("Spec")) {
            targetClassName = className.substring(0, className.indexOf("Spec"));
        }

        if (targetClassName == null) {
            return;
        }

        Resource targetResource = getResourceLocator().findResourceForClassName(targetClassName);
        if (targetResource == null) {
            return;
        }

        try {
            if (GrailsResourceUtils.isDomainClass(targetResource.getURL())) {
                testFor(classNode, new ClassExpression(new ClassNode(targetClassName, 0, ClassHelper.OBJECT_TYPE)));
            }
            else {
                for (String artefactType : artefactTypeToTestMap.keySet()) {
                    if (targetClassName.endsWith(artefactType)) {
                        testFor(classNode, new ClassExpression(new ClassNode(targetClassName, 0, ClassHelper.OBJECT_TYPE)));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Main entry point for the calling the TestForTransformation programmatically.
     *
     * @param classNode The class node that represents th test
     * @param ce The class expression that represents the class to test
     */
    public void testFor(ClassNode classNode, ClassExpression ce) {

        autoAnnotateSetupTeardown(classNode);
        boolean isJunit3Test = isJunit3Test(classNode);

        // make sure the 'log' property is not the one from GroovyTestCase
        FieldNode log = classNode.getField("log");
        if (log == null || log.getDeclaringClass().getName().equals(GROOVY_TEST_CASE_CLASS_NAME)) {
            new LoggingTransformer().performInjectionOnAnnotatedClass(classNode.getModule().getContext(), classNode);
        }
        boolean isSpockTest = isSpockTest(classNode);

        boolean isJunit4 = !isSpockTest && !isJunit3Test;
        if (isJunit4) {
            // assume JUnit 4
            Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
            boolean hasTestMethods = false;
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
                    hasTestMethods = true;
                }
            }
            if (!hasTestMethods) {
                isJunit4 = false;
            }
        }

        if (isJunit4 || isJunit3Test || isSpockTest) {
            final MethodNode methodToAdd = weaveMock(classNode, ce, true);
            if (methodToAdd != null && isJunit3Test) {
                addMethodCallsToMethod(classNode,SET_UP_METHOD, Arrays.asList(methodToAdd));
            }
        }
    }

    private Map<ClassNode, List<Class>> wovenMixins = new HashMap<ClassNode, List<Class>>();
    protected MethodNode weaveMock(ClassNode classNode, ClassExpression value, boolean isClassUnderTest) {

        ClassNode testTarget = value.getType();
        String className = testTarget.getName();
        MethodNode testForMethod = null;
        for (String artefactType : artefactTypeToTestMap.keySet()) {
            if (className.endsWith(artefactType)) {
                Class mixinClass = artefactTypeToTestMap.get(artefactType);
                if (!isAlreadyWoven(classNode, mixinClass)) {
                    weaveMixinClass(classNode, mixinClass);
                    if (isClassUnderTest) {
                        testForMethod = addClassUnderTestMethod(classNode, value, artefactType);
                    }
                    else {
                        addMockCollaboratorToSetup(classNode, value, artefactType);
                    }
                    return testForMethod;
                }

                addMockCollaboratorToSetup(classNode, value, artefactType);
                return null;
            }
        }

        // must be a domain class
        boolean isDataTest = GrailsASTUtils.isSubclassOf(classNode, "grails.test.hibernate.HibernateSpec") || GrailsASTUtils.isSubclassOf(classNode, "grails.test.mongodb.MongoSpec");
        if(!isDataTest) {

            Class<?> domainClassPresent = null;
            try {
                domainClassPresent = Class.forName("org.grails.plugins.domain.DomainClassGrailsPlugin", true, TestForTransformation.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                // not on classpath ignore
            } catch (NoClassDefFoundError e) {
                // ignore
            }

            if(domainClassPresent != null) {

                weaveMixinClass(classNode, DomainClassUnitTestMixin.class);
                if (isClassUnderTest) {
                    testForMethod = addClassUnderTestMethod(classNode, value, DOMAIN_TYPE);
                }
                else {
                    addMockCollaboratorToSetup(classNode, value, DOMAIN_TYPE);
                }

                return testForMethod;
            }
        }
        return null;
    }

    protected Class getMixinClassForArtefactType(ClassNode classNode) {
        String className = classNode.getName();
        for (String artefactType : artefactTypeToTestMap.keySet()) {
            if (className.endsWith(artefactType)) {
                Class mixinClass = artefactTypeToTestMap.get(artefactType);
                if (mixinClass != null) {
                    return mixinClass;
                }
            }
        }
        return null;
    }


    private void addMockCollaboratorToSetup(ClassNode classNode, ClassExpression targetClassExpression, String artefactType) {
        BlockStatement methodBody = getOrCreateTestSetupMethod(classNode);
        addMockCollaborator(artefactType, targetClassExpression,methodBody);
    }

    protected BlockStatement getOrCreateTestSetupMethod(ClassNode classNode) {
        BlockStatement methodBody;
        if (isJunit3Test(classNode)) {
            methodBody = getJunit3Setup(classNode);
        }
        else {
            methodBody = getExistingOrCreateJUnit4Setup(classNode);
        }
        return methodBody;
    }

    protected BlockStatement getExistingOrCreateJUnit4Setup(ClassNode classNode) {
        Statement code = getExistingJUnit4BeforeMethod(classNode);
        if (code instanceof BlockStatement) {
            return (BlockStatement) code;
        }
        return getJunit4Setup(classNode);
    }

    protected Statement getExistingJUnit4BeforeMethod(ClassNode classNode) {
        Statement code = null;
        Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
        for (MethodNode methodNode : declaredMethodsMap.values()) {
            if (isDeclaredBeforeMethod(methodNode)) {
                code = getMethodBody(methodNode);
            }
        }
        return code;
    }

    private Statement getMethodBody(MethodNode methodNode) {
        Statement code = methodNode.getCode();
        if (!(code instanceof BlockStatement)) {
            BlockStatement body = new BlockStatement();
            body.addStatement(code);
            code = body;
        }
        return code;
    }

    private boolean isDeclaredBeforeMethod(MethodNode methodNode) {
        return isPublicInstanceMethod(methodNode) && hasAnnotation(methodNode, Before.class) && !hasAnnotation(methodNode, MixinMethod.class);
    }

    private boolean isPublicInstanceMethod(MethodNode methodNode) {
        return !methodNode.isSynthetic() && !methodNode.isStatic() && methodNode.isPublic();
    }

    private BlockStatement getJunit4Setup(ClassNode classNode) {
        MethodNode setupMethod = classNode.getDeclaredMethod(SET_UP_METHOD, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (setupMethod == null) {
            setupMethod = new MethodNode(SET_UP_METHOD,Modifier.PUBLIC,ClassHelper.VOID_TYPE,GrailsArtefactClassInjector.ZERO_PARAMETERS,null,new BlockStatement());
            setupMethod.addAnnotation(MIXIN_METHOD_ANNOTATION);
            classNode.addMethod(setupMethod);
        }
        if (setupMethod.getAnnotations(BEFORE_CLASS_NODE).size() == 0) {
            setupMethod.addAnnotation(BEFORE_ANNOTATION);
        }
        return getOrCreateMethodBody(classNode, setupMethod, SET_UP_METHOD);

    }

    private BlockStatement getJunit3Setup(ClassNode classNode) {
        boolean hasExistingSetupMethod = classNode.hasDeclaredMethod(SET_UP_METHOD, Parameter.EMPTY_ARRAY);
        BlockStatement setUpMethodBody = getOrCreateNoArgsMethodBody(classNode, SET_UP_METHOD);
        if(!hasExistingSetupMethod) {
            setUpMethodBody.getStatements().add(new ExpressionStatement(new MethodCallExpression(new VariableExpression("super"), SET_UP_METHOD, GrailsArtefactClassInjector.ZERO_ARGS)));
        }
        return setUpMethodBody;
    }

    private boolean isAlreadyWoven(ClassNode classNode, Class mixinClass) {
        List<Class> mixinClasses = wovenMixins.get(classNode);
        if (mixinClasses == null) {
            mixinClasses = new ArrayList<Class>();
            mixinClasses.add(mixinClass);
            wovenMixins.put(classNode, mixinClasses);
        }
        else {
            if (mixinClasses.contains(mixinClass)) {
                return true;
            }

            mixinClasses.add(mixinClass);
        }
        return false;
    }

    protected void weaveMixinClass(ClassNode classNode, Class mixinClass) {
        ListExpression listExpression = new ListExpression();
        listExpression.addExpression(new ClassExpression(new ClassNode(mixinClass)));
        weaveMixinsIntoClass(classNode,listExpression);
    }

    protected MethodNode addClassUnderTestMethod(ClassNode classNode, ClassExpression targetClass, String type) {

        String methodName = "setup" + type + "UnderTest";
        String fieldName = GrailsNameUtils.getPropertyName(type);
        String getterName = GrailsNameUtils.getGetterName(fieldName);
        fieldName = '$' +fieldName;

        if (classNode.getField(fieldName) == null) {
            classNode.addField(fieldName, Modifier.PRIVATE, targetClass.getType(),null);
        }

        MethodNode methodNode = classNode.getDeclaredMethod(methodName,GrailsArtefactClassInjector.ZERO_PARAMETERS);

        VariableExpression fieldExpression = new VariableExpression(fieldName, targetClass.getType());
        if (methodNode == null) {
            BlockStatement setupMethodBody = new BlockStatement();
            addMockCollaborator(type, targetClass, setupMethodBody);

            methodNode = new MethodNode(methodName, Modifier.PUBLIC, ClassHelper.VOID_TYPE, GrailsArtefactClassInjector.ZERO_PARAMETERS,null, setupMethodBody);
            methodNode.addAnnotation(BEFORE_ANNOTATION);
            methodNode.addAnnotation(MIXIN_METHOD_ANNOTATION);
            
            classNode.addMethod(methodNode);
            GrailsASTUtils.addCompileStaticAnnotation(methodNode);
        }

        MethodNode getter = classNode.getDeclaredMethod(getterName, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (getter == null) {
            BlockStatement getterBody = new BlockStatement();
            getter = new MethodNode(getterName, Modifier.PUBLIC, targetClass.getType().getPlainNodeReference(),GrailsArtefactClassInjector.ZERO_PARAMETERS,null, getterBody);

            BinaryExpression testTargetAssignment = new BinaryExpression(fieldExpression, ASSIGN, new ConstructorCallExpression(targetClass.getType(), GrailsArtefactClassInjector.ZERO_ARGS));

            IfStatement autowiringIfStatement = getAutowiringIfStatement(targetClass,fieldExpression, testTargetAssignment);
            getterBody.addStatement(autowiringIfStatement);

            getterBody.addStatement(new ReturnStatement(fieldExpression));
            classNode.addMethod(getter);
            GrailsASTUtils.addCompileStaticAnnotation(getter);
        }

        return methodNode;
    }

    private IfStatement getAutowiringIfStatement(ClassExpression targetClass, VariableExpression fieldExpression, BinaryExpression testTargetAssignment) {
        VariableExpression appCtxVar = new VariableExpression("applicationContext", ClassHelper.make(ApplicationContext.class));

        BooleanExpression applicationContextCheck = new BooleanExpression(
                new BinaryExpression(
                new BinaryExpression(fieldExpression, GrailsASTUtils.EQUALS_OPERATOR, GrailsASTUtils.NULL_EXPRESSION),
                        Token.newSymbol("&&",0,0),
                new BinaryExpression(appCtxVar, GrailsASTUtils.NOT_EQUALS_OPERATOR, GrailsASTUtils.NULL_EXPRESSION)));
        BlockStatement performAutowireBlock = new BlockStatement();
        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(fieldExpression);
        arguments.addExpression(new ConstantExpression(1));
        arguments.addExpression(new ConstantExpression(false));
        BlockStatement assignFromApplicationContext = new BlockStatement();
        ArgumentListExpression argWithClassName = new ArgumentListExpression();

        final PropertyExpression classNamePropertyExpression = new PropertyExpression(targetClass, new ConstantExpression("name"));
        argWithClassName.addExpression(classNamePropertyExpression);

        assignFromApplicationContext.addStatement(new ExpressionStatement(new BinaryExpression(fieldExpression, ASSIGN, new MethodCallExpression(appCtxVar, "getBean", argWithClassName))));
        BlockStatement elseBlock = new BlockStatement();
        elseBlock.addStatement(new ExpressionStatement(testTargetAssignment));
        performAutowireBlock.addStatement(new IfStatement(new BooleanExpression(new MethodCallExpression(appCtxVar, "containsBean", argWithClassName)), assignFromApplicationContext, elseBlock));
        performAutowireBlock.addStatement(new ExpressionStatement(new MethodCallExpression(new PropertyExpression(appCtxVar,"autowireCapableBeanFactory"), "autowireBeanProperties", arguments)));
        return new IfStatement(applicationContextCheck, performAutowireBlock, new BlockStatement());
    }

    protected void addMockCollaborator(String mockType, ClassExpression targetClass, BlockStatement methodBody) {
        ArgumentListExpression args = new ArgumentListExpression();
        args.addExpression(targetClass);
        methodBody.getStatements().add(0, new ExpressionStatement(new MethodCallExpression(new VariableExpression("this"), "mock" + mockType, args)));
    }

    protected void addMockCollaborators(ClassNode classNode, String mockType, List<ClassExpression> targetClasses) {
         addMockCollaborators(mockType, targetClasses, getOrCreateTestSetupMethod(classNode));
    }


    protected void addMockCollaborators(String mockType, List<ClassExpression> targetClasses, BlockStatement methodBody) {
        ArgumentListExpression args = new ArgumentListExpression();
        for(ClassExpression ce : targetClasses) {
            args.addExpression(ce);
        }
        methodBody.getStatements().add(0, new ExpressionStatement(new MethodCallExpression(new VariableExpression("this"), "mock" + mockType + 's', args)));
    }

    @Override
    public void registerMixin(String artefactType, Class mixin) {
        artefactTypeToTestMap.put(artefactType, mixin);
    }
}
