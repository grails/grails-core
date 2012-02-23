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

import grails.test.mixin.TestFor;
import grails.test.mixin.domain.DomainClassUnitTestMixin;
import grails.test.mixin.services.ServiceUnitTestMixin;
import grails.test.mixin.support.MixinMethod;
import grails.test.mixin.web.ControllerUnitTestMixin;
import grails.test.mixin.web.FiltersUnitTestMixin;
import grails.test.mixin.web.GroovyPageUnitTestMixin;
import grails.test.mixin.web.UrlMappingsUnitTestMixin;
import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.GrailsNameUtils;
import groovy.util.GroovyTestCase;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.grails.compiler.logging.LoggingTransformer;
import org.codehaus.groovy.grails.core.io.DefaultResourceLocator;
import org.codehaus.groovy.grails.core.io.ResourceLocator;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.web.filters.FiltersConfigArtefactHandler;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transformation used by the {@link grails.test.mixin.TestFor} annotation to signify the
 * class under test.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings("rawtypes")
public class TestForTransformation extends TestMixinTransformation {

    private static final ClassNode MY_TYPE = new ClassNode(TestFor.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final Token ASSIGN = Token.newSymbol("=", -1, -1);

    protected static final Map<String, Class> artefactTypeToTestMap = new HashMap<String, Class>();
    static {
        artefactTypeToTestMap.put(ControllerArtefactHandler.TYPE, ControllerUnitTestMixin.class);
        artefactTypeToTestMap.put(TagLibArtefactHandler.TYPE, GroovyPageUnitTestMixin.class);
        artefactTypeToTestMap.put(FiltersConfigArtefactHandler.getTYPE().toString(), FiltersUnitTestMixin.class);
        artefactTypeToTestMap.put(UrlMappingsArtefactHandler.TYPE, UrlMappingsUnitTestMixin.class);
        artefactTypeToTestMap.put(ServiceArtefactHandler.TYPE, ServiceUnitTestMixin.class);
    }

    public static final String DOMAIN_TYPE = "Domain";
    public static final ClassNode BEFORE_CLASS_NODE = new ClassNode(Before.class);
    public static final AnnotationNode BEFORE_ANNOTATION = new AnnotationNode(BEFORE_CLASS_NODE);

    public static final AnnotationNode TEST_ANNOTATION = new AnnotationNode(new ClassNode(Test.class));
    public static final ClassNode GROOVY_TEST_CASE_CLASS = new ClassNode(GroovyTestCase.class);

    private ResourceLocator resourceLocator;

    public ResourceLocator getResourceLocator() {
        if (resourceLocator == null) {
            resourceLocator = new DefaultResourceLocator();
            BuildSettings buildSettings = BuildSettingsHolder.getSettings();
            String basedir;
            if (buildSettings != null) {
                basedir = buildSettings.getBaseDir().getAbsolutePath();
            }
            else {
                basedir = ".";
            }

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
        boolean isJunit = classNode.getName().endsWith("Tests");

        if(!junit3Test && !spockTest && !isJunit) return;

        Expression value = node.getMember("value");
        ClassExpression ce;
        if (value instanceof ClassExpression) {
            ce = (ClassExpression) value;
            testFor(classNode, ce);
        }
        else {
            if (!junit3Test){
                List<AnnotationNode> annotations = classNode.getAnnotations(MY_TYPE);
                if (annotations.size()>0) return; // bail out, in this case it was already applied as a local transform
                // no explicit class specified try by convention
                String fileName = source.getName();
                String className = GrailsResourceUtils.getClassName(new FileSystemResource(fileName));
                if (className != null) {
                    boolean isSpock = className.endsWith("Spec");
                    String targetClassName = null;

                    if (isJunit) {
                        targetClassName = className.substring(0, className.indexOf("Tests"));
                    }
                    else if (isSpock) {
                        targetClassName = className.substring(0, className.indexOf("Spec"));
                    }

                    if (targetClassName != null) {
                        Resource targetResource = getResourceLocator().findResourceForClassName(targetClassName);
                        if (targetResource != null) {
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
                    }
                }
            }
        }
    }

    /**
     * Main entry point for the calling the TestForTransformation programmatically.
     *
     * @param classNode The class node that represents th test
     * @param ce The class expression that represents the class to test
     */
    public void testFor(ClassNode classNode, ClassExpression ce) {
        boolean junit3Test = isJunit3Test(classNode);

        // make sure the 'log' property is not the one from GroovyTestCase
        FieldNode log = classNode.getField("log");
        if (log == null || log.getDeclaringClass().equals(GROOVY_TEST_CASE_CLASS)) {
            LoggingTransformer.addLogField(classNode, classNode.getName());
        }
        boolean isSpockTest = isSpockTest(classNode);

        if (!isSpockTest && !junit3Test) {
            // assume JUnit 4
            Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
            for (String methodName : declaredMethodsMap.keySet()) {
                MethodNode methodNode = declaredMethodsMap.get(methodName);
                if (isCandidateMethod(methodNode) && methodNode.getName().startsWith("test")) {
                    if (methodNode.getAnnotations().size()==0) {
                        methodNode.addAnnotation(TEST_ANNOTATION);
                    }
                }
            }
        }

        final MethodNode methodToAdd = weaveMock(classNode, ce, true);
        if (methodToAdd != null && junit3Test) {
            addMethodCallsToMethod(classNode,SET_UP_METHOD, Arrays.asList(methodToAdd));
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
                else {
                    addMockCollaboratorToSetup(classNode, value, artefactType);
                    return null;
                }
            }
        }

        // must be a domain class
        weaveMixinClass(classNode, DomainClassUnitTestMixin.class);
        if (isClassUnderTest) {
            testForMethod = addClassUnderTestMethod(classNode, value, DOMAIN_TYPE);
        }
        else {
            addMockCollaboratorToSetup(classNode, value, DOMAIN_TYPE);
        }

        return testForMethod;
    }

    private void addMockCollaboratorToSetup(ClassNode classNode, ClassExpression targetClassExpression, String artefactType) {

        BlockStatement methodBody;
        if (isJunit3Test(classNode)) {
            methodBody = getJunit3Setup(classNode);
            addMockCollaborator(artefactType, targetClassExpression,methodBody);
        }
        else {
            addToJunit4BeforeMethods(classNode, artefactType, targetClassExpression);
        }
    }

    private void addToJunit4BeforeMethods(ClassNode classNode, String artefactType, ClassExpression targetClassExpression) {
        Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
        boolean weavedIntoBeforeMethods = false;
        for (MethodNode methodNode : declaredMethodsMap.values()) {
            if (isDeclaredBeforeMethod(methodNode)) {
                Statement code = getMethodBody(methodNode);
                addMockCollaborator(artefactType, targetClassExpression, (BlockStatement) code);
                weavedIntoBeforeMethods = true;
            }
        }

        if (!weavedIntoBeforeMethods) {
            BlockStatement junit4Setup = getJunit4Setup(classNode);
            addMockCollaborator(artefactType,targetClassExpression,junit4Setup);
        }
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
        MethodNode setupMethod = classNode.getMethod(SET_UP_METHOD, GrailsArtefactClassInjector.ZERO_PARAMETERS);
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
        return getOrCreateNoArgsMethodBody(classNode, SET_UP_METHOD);
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

        MethodNode methodNode = classNode.getMethod(methodName,GrailsArtefactClassInjector.ZERO_PARAMETERS);

        VariableExpression fieldExpression = new VariableExpression(fieldName);
        if (methodNode == null) {
            BlockStatement setupMethodBody = new BlockStatement();
            addMockCollaborator(type, targetClass, setupMethodBody);


            methodNode = new MethodNode(methodName, Modifier.PUBLIC, ClassHelper.VOID_TYPE, GrailsArtefactClassInjector.ZERO_PARAMETERS,null, setupMethodBody);
            methodNode.addAnnotation(BEFORE_ANNOTATION);
            methodNode.addAnnotation(MIXIN_METHOD_ANNOTATION);
            classNode.addMethod(methodNode);
        }

        MethodNode getter = classNode.getMethod(getterName, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if(getter == null) {
            BlockStatement getterBody = new BlockStatement();
            getter = new MethodNode(getterName, Modifier.PUBLIC, targetClass.getType().getPlainNodeReference(),GrailsArtefactClassInjector.ZERO_PARAMETERS,null, getterBody);

            BinaryExpression testTargetAssignment = new BinaryExpression(fieldExpression, ASSIGN, new ConstructorCallExpression(targetClass.getType(), GrailsArtefactClassInjector.ZERO_ARGS));


            IfStatement autowiringIfStatement = getAutowiringIfStatement(targetClass,fieldExpression, testTargetAssignment);
            getterBody.addStatement(autowiringIfStatement);

            getterBody.addStatement(new ReturnStatement(fieldExpression));
            classNode.addMethod(getter);
        }

        return methodNode;
    }

    private IfStatement getAutowiringIfStatement(ClassExpression targetClass, VariableExpression fieldExpression, BinaryExpression testTargetAssignment) {
        VariableExpression appCtxVar = new VariableExpression("applicationContext");

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
        MethodCallExpression getClassNameMethodCall = new MethodCallExpression(targetClass, "getName", new ArgumentListExpression());
        argWithClassName.addExpression(getClassNameMethodCall);

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
        methodBody.getStatements().add(0, new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "mock" + mockType, args)));
    }
}
