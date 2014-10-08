package org.grails.cli.profile;

import jline.console.completer.Completer;

public interface CompleterFactory {
    public Completer createCompleter(ProjectContext context);
}
