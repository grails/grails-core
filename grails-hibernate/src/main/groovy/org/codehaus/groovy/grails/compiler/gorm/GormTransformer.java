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
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.grails.datastore.gorm.GormInstanceApi;
import org.grails.datastore.gorm.GormStaticApi;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.lang.reflect.Modifier;
import java.net.URL;

/**
 * Transforms GORM entities making the GORM API available to Java
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class GormTransformer extends AbstractGrailsArtefactTransformer {

    public static final String MISSING_GORM_ERROR_MESSAGE = "Cannot locate GORM API implementation. You either don't have a GORM implementation installed (such as the Hibernate plugin) or you are running Grails code outside the context of a Grails application.";
    public static final String HAS_ERRORS_METHOD = "hasErrors";

    @Override
    public String getArtefactType() {
        return DomainClassArtefactHandler.TYPE;
    }

    @Override
    public Class getInstanceImplementation() {
        return GormInstanceApi.class;
    }

    @Override
    public Class getStaticImplementation() {
        return GormStaticApi.class;
    }

    @Override
    protected boolean requiresStaticLookupMethod() {
        return true;
    }

    @Override
    protected MethodNode populateAutowiredApiLookupMethod(ClassNode implementationNode, String apiInstanceProperty, String methodName, BlockStatement methodBody) {
        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(new ConstantExpression(MISSING_GORM_ERROR_MESSAGE));
        methodBody.addStatement(new ThrowStatement(new ConstructorCallExpression(new ClassNode(IllegalStateException.class), arguments)));
        return new MethodNode(methodName, PUBLIC_STATIC_MODIFIER, implementationNode,ZERO_PARAMETERS,null,methodBody);
    }

    @Override
    protected void performInjectionInternal(String apiInstanceProperty, SourceUnit source, ClassNode classNode) {
        final PropertyNode errorsProperty = classNode.getProperty(GrailsDomainClassProperty.ERRORS);
        if(errorsProperty == null) {

            addErrorsProperty(classNode);
            addHasErrorsMethod(classNode);
        }
    }

    private void addHasErrorsMethod(ClassNode classNode) {
        final BlockStatement hasErrorsMethodBody = new BlockStatement();
        final MethodCallExpression hasErrorsMethodCall = new MethodCallExpression(new VariableExpression(GrailsDomainClassProperty.ERRORS), HAS_ERRORS_METHOD, ZERO_ARGS);
        hasErrorsMethodCall.setSafe(true); // null safe
        hasErrorsMethodBody.addStatement(new ExpressionStatement(hasErrorsMethodCall));
        classNode.addMethod(HAS_ERRORS_METHOD, Modifier.PUBLIC, new ClassNode(Boolean.class),ZERO_PARAMETERS, null, hasErrorsMethodBody);
    }

    private void addErrorsProperty(ClassNode classNode) {
        final ArgumentListExpression errorsConstructorArgs = new ArgumentListExpression();
        errorsConstructorArgs.addExpression(THIS_EXPRESSION)
                             .addExpression(new ConstantExpression(classNode.getName()));
        final ConstructorCallExpression emptyErrorsConstructorCall = new ConstructorCallExpression(new ClassNode(BeanPropertyBindingResult.class), errorsConstructorArgs);
        classNode.addProperty(GrailsDomainClassProperty.ERRORS, Modifier.PUBLIC, new ClassNode(Errors.class), emptyErrorsConstructorCall, null, null);

        final BlockStatement methodBody = new BlockStatement();
        final ArgumentListExpression setErrorsArguments = new ArgumentListExpression();
        setErrorsArguments.addExpression(emptyErrorsConstructorCall);
        methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "setErrors", setErrorsArguments)));
        classNode.addMethod(new MethodNode("clearErrors",Modifier.PUBLIC, null, ZERO_PARAMETERS,null, methodBody));
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }
}
