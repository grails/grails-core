package grails.validation;

import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY;
import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.ZERO_PARAMETERS;

import java.lang.reflect.Modifier;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.grails.compiler.injection.ASTValidationErrorsHelper;
import org.codehaus.groovy.grails.compiler.injection.ASTErrorsHelper;
import org.codehaus.groovy.grails.web.plugins.support.ValidationSupport;

public class DefaultASTValidateableHelper implements ASTValidateableHelper{

    private static final String VALIDATE_METHOD_NAME = "validate";
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");

    public void injectValidateableCode(ClassNode commandObjectTypeClassNode) {
        ASTErrorsHelper errorsHelper = new ASTValidationErrorsHelper();
        errorsHelper.injectErrorsCode(commandObjectTypeClassNode);
        addConstrainedPropertiesProperty(commandObjectTypeClassNode);
        addValidateMethod(commandObjectTypeClassNode);
    }
    protected void addConstrainedPropertiesProperty(
            final ClassNode classNode) {
        classNode.addProperty("constrainedProperties",Modifier.STATIC | Modifier.PUBLIC, new ClassNode(Object.class), null, null, null);
    }

    protected void addValidateMethod(final ClassNode classNode) {
        String fieldsToValidateParameterName = "$fieldsToValidate";
        final MethodNode listArgValidateMethod = classNode.getMethod(VALIDATE_METHOD_NAME, new Parameter[]{new Parameter(new ClassNode(List.class), fieldsToValidateParameterName)});
        if (listArgValidateMethod == null) {
            final BlockStatement validateMethodCode = new BlockStatement();
            final ArgumentListExpression validateInstanceArguments = new ArgumentListExpression();
            validateInstanceArguments.addExpression(THIS_EXPRESSION);
            validateInstanceArguments.addExpression(new VariableExpression(fieldsToValidateParameterName));
            final ClassNode validationSupportClassNode = new ClassNode(ValidationSupport.class);
            final StaticMethodCallExpression invokeValidateInstanceExpression = new StaticMethodCallExpression(validationSupportClassNode, "validateInstance", validateInstanceArguments);
            validateMethodCode.addStatement(new ExpressionStatement(invokeValidateInstanceExpression));
            final Parameter fieldsToValidateParameter = new Parameter(new ClassNode(List.class), fieldsToValidateParameterName);
            classNode.addMethod(new MethodNode(
                  VALIDATE_METHOD_NAME, Modifier.PUBLIC, new ClassNode(Boolean.class),
                  new Parameter[]{fieldsToValidateParameter}, EMPTY_CLASS_ARRAY, validateMethodCode));
        }
        final MethodNode noArgValidateMethod = classNode.getMethod(VALIDATE_METHOD_NAME,ZERO_PARAMETERS);
        if (noArgValidateMethod == null) {
            final BlockStatement validateMethodCode = new BlockStatement();

            final ArgumentListExpression validateInstanceArguments = new ArgumentListExpression();
            validateInstanceArguments.addExpression(new CastExpression(new ClassNode(List.class), new ConstantExpression(null)));
            final Expression callListArgValidateMethod = new MethodCallExpression(THIS_EXPRESSION, VALIDATE_METHOD_NAME, validateInstanceArguments);
            validateMethodCode.addStatement(new ReturnStatement(callListArgValidateMethod));
            classNode.addMethod(new MethodNode(
                  VALIDATE_METHOD_NAME, Modifier.PUBLIC, new ClassNode(Boolean.class),
                  ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, validateMethodCode));
        }
    }
}
