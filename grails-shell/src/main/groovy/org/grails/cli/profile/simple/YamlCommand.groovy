package org.grails.cli.profile.simple

import grails.build.logging.GrailsConsole

import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.Profile

class YamlCommand {
    String name
    File file
    Object yamlContent
    Profile profile
    private List<YamlCommandStep> steps
    
    CommandDescription getDescription() {
        new CommandDescription(name: name, description: yamlContent?.description, usage: yamlContent?.usage)
    }

    List<YamlCommandStep> getSteps() {
        if(steps==null) {
            steps = []
            yamlContent.steps.each { 
                Map<String, String> stepParameters = it.collectEntries { k,v -> [k as String, v as String] }
                switch(stepParameters.command) {
                    case 'render':
                        steps.add(new RenderCommandStep(commandParameters: stepParameters))
                        break
                }
            }
        }
        steps
    }
        
    public boolean handleCommand(CommandLine commandLine, GrailsConsole console) {
        if(!commandLine.getRemainingArgs()) {
            console.error("Expecting an argument to $name.")
            console.info("${description.usage}")
            return true
        }
        for(YamlCommandStep step : getSteps()) {
            if(!step.handleStep(commandLine, console)) {
                break
            }
        }
        true
    }
}
