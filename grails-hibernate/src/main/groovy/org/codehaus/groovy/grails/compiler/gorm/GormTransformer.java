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
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.grails.datastore.gorm.GormInstanceApi;
import org.grails.datastore.gorm.GormStaticApi;

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



    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }
}
