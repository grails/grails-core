package org.codehaus.groovy.grails.plugins.codecs;

import grails.converters.JSON;

public class JSONEncoder extends BasicJSONEncoder {
    @Override
    protected Object encodeAsJsonObject(Object o) {
        return new JSON(o).toString();
    }
}
