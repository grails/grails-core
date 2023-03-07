package org.grails.core.cfg

import grails.util.Environment

trait EnvironmentAwareSpec {

    Environment previous = null

    void setEnvironment(Environment environment) {
        if (System.hasProperty(Environment.KEY)) {
            previous = Environment.getEnvironment(System.getProperty(Environment.KEY))
        }
        System.setProperty(Environment.KEY, environment ? environment.getName() : '')
    }

    void resetEnvironment() {
        if (previous) {
            System.setProperty(Environment.KEY, previous.getName())
        } else {
            System.setProperty(Environment.KEY, '')
        }
    }
}
