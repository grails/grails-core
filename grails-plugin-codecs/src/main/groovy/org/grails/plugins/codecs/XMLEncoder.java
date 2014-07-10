package org.grails.plugins.codecs;

import grails.converters.XML;
import org.grails.plugins.codecs.BasicXMLEncoder;

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
