package org.grails.commons.spring;

import grails.core.GrailsApplication;

public class GrailsMockDependantObject {

    GrailsApplication application;

    public GrailsApplication getApplication() {
        return application;
    }

    public void setApplication(GrailsApplication application) {
        this.application = application;
    }
}
