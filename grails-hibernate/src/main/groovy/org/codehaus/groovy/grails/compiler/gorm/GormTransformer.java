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
package org.codehaus.groovy.grails.compiler.gorm;

import grails.persistence.Entity;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.grails.datastore.gorm.GormInstanceApi;
import org.grails.datastore.gorm.GormStaticApi;

/**
 * Transforms GORM entities making the GORM API available to Java.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class GormTransformer extends AbstractGrailsArtefactTransformer {

    public static final String NEW_INSTANCE_METHOD = "newInstance";

    private static final List<String> EXCLUDES = Arrays.asList("create", "setTransactionManager");
    private static final Class<?>[] EMPTY_JAVA_CLASS_ARRAY = {};
    private static final Class<?>[] OBJECT_CLASS_ARG = { Object.class };

    @Override
    protected boolean isStaticCandidateMethod(ClassNode classNode, MethodNode declaredMethod) {
        String methodName = declaredMethod.getName();
        return !EXCLUDES.contains(methodName) &&
                !isGetter(methodName, declaredMethod) &&
                !isSetter(methodName, declaredMethod) &&
                super.isStaticCandidateMethod(classNode, declaredMethod);
    }

    private boolean isSetter(String methodName, MethodNode declaredMethod) {
        return declaredMethod.getParameters().length ==2 && GrailsClassUtils.isSetter(methodName, OBJECT_CLASS_ARG);
    }

    private boolean isGetter(String methodName, MethodNode declaredMethod) {
        return declaredMethod.getParameters().length == 1 && GrailsClassUtils.isGetter(methodName, EMPTY_JAVA_CLASS_ARRAY);
    }

    @Override
    public String getArtefactType() {
        return DomainClassArtefactHandler.TYPE;
    }

    @Override
    public Class<?> getInstanceImplementation() {
        return GormInstanceApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return GormStaticApi.class;
    }

    @Override
    protected boolean requiresStaticLookupMethod() {
        return true;
    }

    @Override
    protected MethodNode populateAutowiredApiLookupMethod(ClassNode classNode, ClassNode implementationNode, String apiInstanceProperty, String methodName, BlockStatement methodBody) {
        return new MethodNode(methodName, PUBLIC_STATIC_MODIFIER, implementationNode,ZERO_PARAMETERS,null,methodBody);
    }

    @Override
    protected void performInjectionInternal(String apiInstanceProperty, SourceUnit source, ClassNode classNode) {
        classNode.setUsingGenerics(true);
        GrailsASTUtils.addAnnotationIfNecessary(classNode, Entity.class);

        final BlockStatement methodBody = new BlockStatement();
        methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new ClassExpression(classNode), NEW_INSTANCE_METHOD,ZERO_ARGS)));
        MethodNode methodNode = classNode.getDeclaredMethod(CreateDynamicMethod.METHOD_NAME, ZERO_PARAMETERS);
        classNode = GrailsASTUtils.nonGeneric(classNode);
        if(methodNode == null) {
            classNode.addMethod(new MethodNode(CreateDynamicMethod.METHOD_NAME, PUBLIC_STATIC_MODIFIER, classNode, ZERO_PARAMETERS,null, methodBody));
        }
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }
}
