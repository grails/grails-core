package org.grails.cli.profile;

import jline.console.completer.Completer;

public interface Profile {
    String getName();
    Iterable<Completer> getCompleters(ProjectContext context);
    Iterable<CommandLineHandler> getCommandLineHandlers(ProjectContext context);
    Iterable<Profile> getExtends();
}
