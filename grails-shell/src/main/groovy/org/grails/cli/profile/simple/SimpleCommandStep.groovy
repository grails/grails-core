package org.grails.cli.profile.simple

import org.grails.cli.profile.ExecutionContext

abstract class SimpleCommandStep {
    Map<String, String> commandParameters
    SimpleCommand command
    
    /**
     * @param commandLine
     * @param console
     * @return true if should continue executing other steps
     */
    abstract public boolean handleStep(ExecutionContext context)
}
