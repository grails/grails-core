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
package org.codehaus.groovy.grails.compiler.web;


import grails.artefact.Artefact;
import grails.util.BuildSettingsHolder;
import grails.web.Action;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhances controller classes by converting closures actions to method actions
 *
 * @author Stephane Maldini
 * @since 1.4
 */
/*

class TestController{

    //default  scope configurable in Config.groovy
	static scope = 'singleton'

	def peterTheFrenchService

    //--------------------------
    //allow use of methods as actions
	def someAction(){
        	render 'ata'
	}

	/ becomes behind the scene :
	@Action
    def someAction(){
        	render 'ata'
	}
	/

    //--------------------------
    //Compile time transformed to method
	def lol2 = {
        	render 'testxx'
	}

    / becomes behind the scene :
        @Action def lol2(){  render 'testxx'  }
    /

    //--------------------------

    def lol4 = { PeterCommand cmd ->
            render cmd.a
    }

    / becomes behind the scene :
               @Action def lol4(){
                 PeterCommand cmd = new PeterCommand(); bindData(cmd, params)
                 render 'testxx'
               }
    /
}


*/

@AstTransformer
public class MethodActionTransformer implements ClassInjector {

    private static final AnnotationNode ACTION_ANNOTATION_NODE = new AnnotationNode(new ClassNode(Action.class));
    private static final AnnotationNode ARTEFACT_ANNOTATION_NODE = new AnnotationNode(new ClassNode(Artefact.class));
    private static final Parameter[] EMPTY_PARAMS = new Parameter[0];
    private static final ClassNode[] EMPTY_EXCEPTIONS = new ClassNode[0];
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final VariableExpression PARAMS_EXPRESSION = new VariableExpression("params");
    private static final TupleExpression EMPTY_TUPLE = new TupleExpression();

    private Boolean converterEnabled;

    public MethodActionTransformer() {
        converterEnabled = BuildSettingsHolder.getSettings().getConvertClosuresArtefacts();
    }

    @Override
    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        annotateCandidateActionMethods(classNode);
        if (converterEnabled) {
            transformClosuresToMethods(classNode);
        }
    }

    private void annotateCandidateActionMethods(ClassNode classNode) {
        for (MethodNode method : classNode.getMethods()) {
            if (!method.isStatic() &&
                    method.getParameters().length == 0 &&
                    method.getAnnotations(ACTION_ANNOTATION_NODE.getClassNode()).isEmpty() &&
                    method.getLineNumber() >= 0
                    ) {

                method.addAnnotation(ACTION_ANNOTATION_NODE);
            }
        }
    }

    private void transformClosuresToMethods(ClassNode classNode) {
        List<PropertyNode> fields = new ArrayList<PropertyNode>(classNode.getProperties());

        Expression initialExpression;
        ClosureExpression closureAction;
        MethodNode actionMethod;

        for (PropertyNode field : fields) {
            initialExpression = field.getInitialExpression();
            if (!field.isStatic() &&
                    initialExpression != null && initialExpression.getClass().equals(ClosureExpression.class)) {
                closureAction = (ClosureExpression) initialExpression;
                actionMethod = new MethodNode(field.getName(), Modifier.PUBLIC, field.getType(), EMPTY_PARAMS, EMPTY_EXCEPTIONS, closureCode(closureAction));
                actionMethod.addAnnotation(ACTION_ANNOTATION_NODE);

                classNode.getProperties().remove(field);
                classNode.addMethod(actionMethod);
            }
        }
    }

    private Statement closureCode(ClosureExpression closureAction) {
        Statement newCommandCode;
        ConstructorCallExpression constructorCallExpression;
        ArgumentListExpression arguments;

        BlockStatement wrapper = new BlockStatement();

        for (Parameter param : closureAction.getParameters()) {
            constructorCallExpression = new ConstructorCallExpression(param.getType(), EMPTY_TUPLE);
            newCommandCode = new ExpressionStatement(
                    new DeclarationExpression(new VariableExpression(param.getName(), param.getType()),
                            Token.newSymbol(Types.EQUALS, 0, 0),
                            constructorCallExpression
                    ));

            wrapper.addStatement(newCommandCode);

            arguments = new ArgumentListExpression();
            arguments.addExpression(new VariableExpression(param.getName()));
            arguments.addExpression(new VariableExpression(PARAMS_EXPRESSION));
            wrapper.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "bindData", arguments)));
        }

        wrapper.addStatement(closureAction.getCode());

        return wrapper;
    }

    @Override
    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    @Override
    public boolean shouldInject(URL url) {
        return url != null && ControllerTransformer.CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
