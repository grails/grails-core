package org.codehaus.groovy.grails.test;

import grails.util.GrailsUtil;
import junit.framework.Test;
import org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter;

/**
 * JUnit plain text formatter that sanitises the stack traces generated
 * by tests.
 */
public class PlainFormatter extends PlainJUnitResultFormatter {
    public void addFailure(Test test, Throwable throwable) {
        GrailsUtil.deepSanitize(throwable);
        super.addFailure(test, (Throwable)throwable);
    }

    public void addError(Test test, Throwable throwable) {
        GrailsUtil.deepSanitize(throwable);
        super.addError(test, throwable);
    }
}
