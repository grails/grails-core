package org.codehaus.groovy.grails.plugins.codecs;

import grails.converters.JSON;

public class JSONEncoder extends BasicJSONEncoder {
    @Override
    protected Object encodeAsJsonObject(Object o) {
        if(o instanceof JSON) {
            return o;
        } else {
            return new JSON(o);
        }
    }
}
