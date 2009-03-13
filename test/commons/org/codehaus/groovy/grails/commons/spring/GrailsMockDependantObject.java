package org.codehaus.groovy.grails.commons.spring;

import org.codehaus.groovy.grails.commons.GrailsApplication;

public class GrailsMockDependantObject {

    GrailsApplication application;


    public GrailsApplication getApplication() {
        return application;
    }

    public void setApplication(GrailsApplication application) {
        this.application = application;
    }
}
