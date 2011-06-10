package org.codehaus.groovy.grails.cli.support;

import groovy.lang.Closure;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 10/06/2011
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */
public abstract class OwnerlessClosure extends Closure {
    public OwnerlessClosure() {
        super(new Object());
    }
}
