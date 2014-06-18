package org.codehaus.groovy.grails.web.mime;

import java.util.Map;

/**
 * @deprecated Use {@link grails.web.mime.MimeType} instead
 */
@Deprecated
public class MimeType extends grails.web.mime.MimeType {
    public MimeType(String name, Map params) {
        super(name, params);
    }

    public MimeType(String name) {
        super(name);
    }

    public MimeType(String name, String extension, Map<String, String> params) {
        super(name, extension, params);
    }

    public MimeType(String name, String extension) {
        super(name, extension);
    }
}
