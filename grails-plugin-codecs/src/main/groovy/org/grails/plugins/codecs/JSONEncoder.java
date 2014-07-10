package org.grails.plugins.codecs;

import grails.converters.JSON;
import org.grails.plugins.codecs.BasicJSONEncoder;

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
