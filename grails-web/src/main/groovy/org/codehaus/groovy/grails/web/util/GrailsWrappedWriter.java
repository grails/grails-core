package org.codehaus.groovy.grails.web.util;

import java.io.Writer;

public interface GrailsWrappedWriter {
    public boolean isAllowUnwrappingOut();
    public Writer unwrap();
    public void markUsed();
}
