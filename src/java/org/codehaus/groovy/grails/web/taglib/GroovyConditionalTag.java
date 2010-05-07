package org.codehaus.groovy.grails.web.taglib;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

abstract class GroovyConditionalTag extends GroovySyntaxTag {

    static final String ATTRIBUTE_TEST = "test";
    static final String ATTRIBUTE_ENV = "env";

    public void doStartTag() {
        String env = attributeValueOrNull(ATTRIBUTE_ENV);
        String test = attributeValueOrNull(ATTRIBUTE_TEST);

        if ((env == null) && (test == null)) {
            throw new GrailsTagException("Tag [" + getName()
                    + "] must have one or both of the attributes ["
                    + ATTRIBUTE_TEST + "] or [" + ATTRIBUTE_ENV + "]");
        }

        String envExpression = environmentExpressionOrTrue(env);
        String testExpression = testExpressionOrTrue(test);

        outputStartTag(envExpression, testExpression);
    }

    protected abstract void outputStartTag(String envExpression, String testExpression);

    public void doEndTag() {
        out.println("}");
    }

    protected String attributeValueOrNull(String attributeName) {
        String attributeValue = (String) attributes.get(attributeName);
        return StringUtils.isBlank(attributeValue) ? null : attributeValue;
    }

    private String environmentExpressionOrTrue(String envAttributeValue) {
        String expression = "true";
        if (envAttributeValue != null) {
            expression = "(GrailsUtil.environment == '"
                    + calculateExpression(envAttributeValue) + "')";
        }
        return expression;
    }

    private String testExpressionOrTrue(String testAttributeValue) {
        String expression = "true";
        if (testAttributeValue != null) {
            expression = "(" + testAttributeValue + ")";
        }
        return expression;
    }

    public boolean isKeepPrecedingWhiteSpace() {
        return true;
    }

    public boolean isAllowPrecedingContent() {
        return true;
    }

}
