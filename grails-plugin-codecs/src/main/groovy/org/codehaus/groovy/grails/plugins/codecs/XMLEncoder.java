package org.codehaus.groovy.grails.plugins.codecs;

import grails.converters.XML;

public class XMLEncoder extends BasicXMLEncoder {
    @Override
    protected Object encodeAsXmlObject(Object o) {
        if(o instanceof XML) {
            return o;
        } else {
            return new XML(o); 
        }
    }
}
