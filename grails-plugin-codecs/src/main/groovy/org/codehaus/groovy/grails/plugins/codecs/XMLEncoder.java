package org.codehaus.groovy.grails.plugins.codecs;

import grails.converters.XML;

public class XMLEncoder extends BasicXMLEncoder {
    @Override
    protected Object encodeAsXmlObject(Object o) {
        return new XML(o).toString(); 
    }
}
