package org.grails.cli.profile.simple

import grails.build.logging.GrailsConsole

import org.codehaus.groovy.grails.cli.parsing.CommandLine

class RenderCommandStep extends YamlCommandStep {

    @Override
    public boolean handleStep(CommandLine commandLine, GrailsConsole console) {
        console.info("-render- $commandParameters")
        return true
    }
}
