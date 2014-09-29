package org.grails.cli.profile;

import java.util.List;

public interface CommandLineHandler {
    boolean handleCommand(ExecutionContext context); 
    List<CommandDescription> listCommands(ProjectContext context);
}
