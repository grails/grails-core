/* Copyright 2006-2007 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.converters;


import com.thoughtworks.xstream.XStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.converters.AbstractConverter;
import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.xtream.DomainClassConverter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * A converter that converts domain classes to XML
 *
 * @author Siegfried Puchbauer
 */
public class XML extends AbstractConverter implements Converter {

    public static final Log log = LogFactory.getLog(XML.class);

    private Object target;

    private static XStream xstream;

    public static XStream getXStream() {
        if (xstream == null) {
            xstream = setupXStream();
        }
        return xstream;
    }

    public static void addAlias(String alias, Class clazz) {
        getXStream().alias(alias, clazz);
    }

    public static XStream setupXStream() {
        XStream xstream = new XStream();
        xstream.registerConverter(new DomainClassConverter(), XStream.PRIORITY_VERY_HIGH);
        xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        return xstream;
    }

    public XML() {

    }

    public XML(Object target) {
        this.target = target;
    }

    public void render(Writer out) throws ConverterException {
        getXStream().toXML(target, out);
    }

    public void render(HttpServletResponse response) throws ConverterException {
        response.setContentType("text/xml");
        try {
            Writer writer = response.getWriter();
            String encoding = response.getCharacterEncoding();
            writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
            render(writer);
        } catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Object asType(Class type) {
        return null;
    }

}
