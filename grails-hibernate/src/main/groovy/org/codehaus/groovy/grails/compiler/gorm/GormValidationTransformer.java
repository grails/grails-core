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

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.compiler.injection.ASTValidationErrorsHelper;
import org.codehaus.groovy.grails.compiler.injection.ASTErrorsHelper;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.grails.datastore.gorm.GormValidationApi;

import java.net.URL;
import java.util.Arrays;

/**
 * Makes the validate methods statically available via an AST transformation.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class GormValidationTransformer extends AbstractGrailsArtefactTransformer{

    public static final String HAS_ERRORS_METHOD = "hasErrors";
    private static final java.util.List<String> EXCLUDES = Arrays.asList(
       "setErrors", "getErrors", HAS_ERRORS_METHOD,
       "getBeforeValidateHelper", "setBeforeValidateHelper",
       "getValidator", "setValidator");
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
        return declaredMethod.getParameters().length ==1 && GrailsClassUtils.isSetter(methodName, OBJECT_CLASS_ARG);
    }

    private boolean isGetter(String methodName, MethodNode declaredMethod) {
        return declaredMethod.getParameters().length == 0 && GrailsClassUtils.isGetter(methodName, EMPTY_JAVA_CLASS_ARRAY);
    }
    @Override
    protected boolean requiresStaticLookupMethod() {
        return true;
    }

    @Override
    public String getArtefactType() {
        return DomainClassArtefactHandler.TYPE;
    }

    @Override
    public Class<?> getInstanceImplementation() {
        return GormValidationApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // no static API
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }

    @Override
    protected boolean isCandidateInstanceMethod(ClassNode classNode, MethodNode declaredMethod) {
        return !EXCLUDES.contains(declaredMethod.getName()) && super.isCandidateInstanceMethod(classNode, declaredMethod);
    }

    @Override
    protected void performInjectionInternal(String apiInstanceProperty, SourceUnit source, ClassNode classNode) {
        final PropertyNode errorsProperty = classNode.getProperty(GrailsDomainClassProperty.ERRORS);
        if (errorsProperty == null) {
            addErrorsProperty(classNode);
        }
    }

    private void addErrorsProperty(ClassNode classNode) {
        ASTErrorsHelper errorsHelper = new ASTValidationErrorsHelper();
        errorsHelper.injectErrorsCode(classNode);
    }
}
