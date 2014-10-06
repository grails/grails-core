package org.grails.cli.profile;

import grails.build.logging.GrailsConsole;

import java.io.File;

public interface ProjectContext {
    GrailsConsole getConsole();
    File getBaseDir();
    String navigateConfig(String... path);
    <T> T navigateConfigForType(Class<T> requiredType, String... path);
}
