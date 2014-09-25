package org.grails.cli.profile.simple

import grails.build.logging.GrailsConsole

import org.codehaus.groovy.grails.cli.parsing.CommandLine

abstract class YamlCommandStep {
    Map<String, String> commandParameters
    
    /**
     * @param commandLine
     * @param console
     * @return true if should continue executing other steps
     */
    abstract public boolean handleStep(CommandLine commandLine, GrailsConsole console)
}
