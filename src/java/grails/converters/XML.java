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


import grails.util.GrailsNameUtils;
import groovy.lang.Closure;
import groovy.util.BuilderSupport;
import groovy.util.XmlSlurper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.converters.AbstractConverter;
import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.configuration.ConverterConfiguration;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller;
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.pages.FastStringWriter;
import org.codehaus.groovy.grails.web.xml.PrettyPrintXMLStreamWriter;
import org.codehaus.groovy.grails.web.xml.StreamingMarkupWriter;
import org.codehaus.groovy.grails.web.xml.XMLStreamWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Map;
import java.util.Stack;

/**
 * A converter that converts domain classes to XML
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 */
public class XML extends AbstractConverter<XMLStreamWriter> implements Converter<XMLStreamWriter> {

    public static final Log log = LogFactory.getLog(XML.class);

    private static final String CACHED_XML = "org.codehaus.groovy.grails.CACHED_XML_REQUEST_CONTENT";

    private Object target;

    private StreamingMarkupWriter stream;

    private final ConverterConfiguration<XML> config;

    private final String encoding;

    private final CircularReferenceBehaviour circularReferenceBehaviour;

    private XMLStreamWriter writer;

    private Stack<Object> referenceStack = new Stack<Object>();

    private boolean isRendering = false;

    public XML() {
        this.config = ConvertersConfigurationHolder.getConverterConfiguration(XML.class);
        this.encoding = config.getEncoding();
        this.circularReferenceBehaviour = config.getCircularReferenceBehaviour();
    }

    public XML(Object target) {
        this();
        this.target = target;
    }

    protected ConverterConfiguration<XML> initConfig() {
        return ConvertersConfigurationHolder.getConverterConfiguration(XML.class);
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    private void finalizeRender(Writer out) {
        try {
            if(out != null) {
                out.flush();
                out.close();
            }
        }
        catch (Exception e) {
            log.warn("Unexpected exception while closing a writer: " + e.getMessage());
        }
    }

    public void render(Writer out) throws ConverterException {
        stream = new StreamingMarkupWriter(out, encoding);
        this.writer = config.isPrettyPrint() ? new PrettyPrintXMLStreamWriter(stream): new XMLStreamWriter(stream);

        try {
            isRendering = true;
            this.writer.startDocument(encoding, "1.0");
            this.writer.startNode(getElementName(target));
            convertAnother(target);
            this.writer.end();
        }
        catch (Exception e) {
            throw new ConverterException(e);
        }
        finally {
            finalizeRender(out);
            isRendering = false;
        }
    }

    private void checkState() {
        if (!isRendering) throw new IllegalStateException("Illegal XML Converter call!");
    }

    public String getElementName(Object o) {
        ObjectMarshaller<XML> om = config.getMarshaller(o);
        if (om instanceof NameAwareMarshaller) {
            return ((NameAwareMarshaller) om).getElementName(o);
        }
        return GrailsNameUtils.getPropertyName(o.getClass());
    }

    public void convertAnother(Object o) throws ConverterException {
        try {
            if (o == null) {
                // noop
            }
            else if (o instanceof CharSequence) {
                writer.characters(o.toString());
            }
            else if (o instanceof Class) {
                writer.characters(((Class) o).getName());
            }
            else if ((o.getClass().isPrimitive() && !o.getClass().equals(byte[].class))
                    || o instanceof Number || o instanceof Boolean) {
                writer.characters(String.valueOf(o));
            }
            else {
                if (referenceStack.contains(o)) {
                    handleCircularRelationship(o);
                }
                else {
                    referenceStack.push(o);
                    ObjectMarshaller<XML> marshaller = config.getMarshaller(o);
                    if (marshaller == null) {
                        throw new ConverterException("Unconvertable Object of class: " + o.getClass().getName());
                    }
                    marshaller.marshalObject(o, this);
                    referenceStack.pop();
                }
            }
        }
        catch (Throwable t) {
            throw ConverterUtil.resolveConverterException(t);
        }
    }

    public ObjectMarshaller<XML> lookupObjectMarshaller(Object target) {
        return config.getMarshaller(target);
    }

    public int getDepth() {
        return referenceStack.size();
    }

    public XML startNode(String tagName) {
        checkState();
        try {
            this.writer.startNode(tagName);
        }
        catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
        return this;
    }

    public XML chars(String chars) {
        checkState();
        try {
            this.writer.characters(chars);
        }
        catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
        return this;
    }


    public XML attribute(String name, String value) {
        checkState();
        try {
            this.writer.attribute(name, value);
        }
        catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
        return this;
    }

    public XML end() {
        checkState();
        try {
            this.writer.end();
        }
        catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
        return this;
    }

    protected void handleCircularRelationship(Object o) throws ConverterException {
        switch (circularReferenceBehaviour) {
            case DEFAULT:
                StringBuilder ref = new StringBuilder();
                int idx = referenceStack.indexOf(o);
                for (int i = referenceStack.size() - 1; i > idx; i--) {
                    ref.append("../");
                }
                attribute("ref", ref.substring(0, ref.length() - 1));
                break;
            case EXCEPTION:
                throw new ConverterException("Circular Reference detected: class " + o.getClass().getName());
            case INSERT_NULL:
                convertAnother(null);
        }
    }

    public void render(HttpServletResponse response) throws ConverterException {
        response.setContentType("text/xml");
        try {
            render(response.getWriter());
        }
        catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    public XMLStreamWriter getWriter() throws ConverterException {
        checkState();
        return writer;
    }

    public StreamingMarkupWriter getStream() {
        checkState();
        return stream;
    }

    public void build(Closure c) throws ConverterException {
//        checkState();
//        chars("");
//        StreamingMarkupBuilder smb = new StreamingMarkupBuilder();
//        Writable writable = (Writable) smb.bind(c);
//        try {
//            writable.writeTo(getStream().unescaped());
//        }
//        catch (IOException e) {
//            throw new ConverterException(e);
//        }

        new Builder(this).execute(c);
    }

    public String toString() {
        FastStringWriter strw = new FastStringWriter();
        render(strw);
        strw.flush();
        return strw.toString();
    }

    /**
     * Parses the given XML
     *
     * @param source a String containing some XML
     * @return a groovy.util.XmlSlurper
     * @throws ConverterException
     */
    public static Object parse(String source) throws ConverterException {
        try {
            return new XmlSlurper().parseText(source);
        }
        catch (Exception e) {
            throw new ConverterException("Error parsing XML", e);
        }
    }

    /**
     * Parses the given XML
     *
     * @param is       an InputStream to read from
     * @param encoding the Character Encoding to use
     * @return a groovy.util.XmlSlurper
     * @throws ConverterException
     */
    public static Object parse(InputStream is, String encoding) throws ConverterException {
        try {
            InputStreamReader reader = new InputStreamReader(is, encoding);
            return new XmlSlurper().parse(reader);
        }
        catch (Exception e) {
            throw new ConverterException("Error parsing XML", e);
        }
    }

    /**
     * Parses the give XML (read from the POST Body of the Request)
     *
     * @param request an HttpServletRequest
     * @return a groovy.util.XmlSlurper
     * @throws ConverterException
     */
    public static Object parse(HttpServletRequest request) throws ConverterException {
        Object xml = request.getAttribute(CACHED_XML);
        if (xml != null) return xml;
        String encoding = request.getCharacterEncoding();
        if (encoding == null)
            encoding = Converter.DEFAULT_REQUEST_ENCODING;
        try {
            if (!request.getMethod().equalsIgnoreCase("GET")) {
                xml = parse(request.getInputStream(), encoding);
                request.setAttribute(CACHED_XML, xml);
            }
            return xml;
        }
        catch (IOException e) {
            throw new ConverterException("Error parsing XML", e);
        }
    }

    public static ConverterConfiguration<XML> getNamedConfig(String configName) throws ConverterException {
        ConverterConfiguration<XML> cfg = ConvertersConfigurationHolder.getNamedConverterConfiguration(configName, XML.class);
        if (cfg == null)
            throw new ConverterException(String.format("Converter Configuration with name '%s' not found!", configName));
        return cfg;
    }

    public static Object use(String configName, Closure callable) throws ConverterException {
        ConverterConfiguration<XML> old = ConvertersConfigurationHolder.getThreadLocalConverterConfiguration(XML.class);
        ConverterConfiguration<XML> cfg = getNamedConfig(configName);
        ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(XML.class, cfg);
        try {
            return callable.call();
        }
        finally {
            ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(XML.class, old);
        }
    }

    public static void use(String cfgName) throws ConverterException {
        if (cfgName == null || "default".equals(cfgName))
            ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(XML.class, null);
        else
            ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(XML.class, getNamedConfig(cfgName));
    }

    public static void registerObjectMarshaller(Class clazz, Closure callable) throws ConverterException {
        registerObjectMarshaller(new ClosureOjectMarshaller<XML>(clazz, callable));
    }

    public static void registerObjectMarshaller(Class clazz, int priority, Closure callable) throws ConverterException {
        registerObjectMarshaller(new ClosureOjectMarshaller<XML>(clazz, callable), priority);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<XML> om) throws ConverterException {
        ConverterConfiguration<XML> cfg = ConvertersConfigurationHolder.getConverterConfiguration(XML.class);
        if (cfg == null)
            throw new ConverterException("Default Configuration not found for class " + XML.class.getName());
        if (!(cfg instanceof DefaultConverterConfiguration)) {
            cfg = new DefaultConverterConfiguration<XML>(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(XML.class, cfg);
        }
        ((DefaultConverterConfiguration<XML>) cfg).registerObjectMarshaller(om);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<XML> om, int priority) throws ConverterException {
        ConverterConfiguration<XML> cfg = ConvertersConfigurationHolder.getConverterConfiguration(XML.class);
        if (cfg == null)
            throw new ConverterException("Default Configuration not found for class " + XML.class.getName());
        if (!(cfg instanceof DefaultConverterConfiguration)) {
            cfg = new DefaultConverterConfiguration<XML>(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(XML.class, cfg);
        }
        ((DefaultConverterConfiguration<XML>) cfg).registerObjectMarshaller(
                om, priority
        );
    }

    public static void createNamedConfig(String name, Closure callable) throws ConverterException {
        DefaultConverterConfiguration<XML> cfg = new DefaultConverterConfiguration<XML>(ConvertersConfigurationHolder.getConverterConfiguration(XML.class));
        try {
            callable.call(cfg);
            ConvertersConfigurationHolder.setNamedConverterConfiguration(XML.class, name, cfg);
        }
        catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
    }

    public static void withDefaultConfiguration(Closure callable) throws ConverterException {
        ConverterConfiguration<XML> cfg = ConvertersConfigurationHolder.getConverterConfiguration(XML.class);
        if (!(cfg instanceof DefaultConverterConfiguration)) {
            cfg = new DefaultConverterConfiguration<XML>(cfg);

        }
        try {
            callable.call(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(XML.class, cfg);
        }
        catch (Throwable t) {
            throw ConverterUtil.resolveConverterException(t);
        }
    }

    public class Builder extends BuilderSupport {

        private XML xml;

        public Builder(XML xml) {
            this.xml = xml;
        }

        public void execute(Closure callable) {
            callable.setDelegate(this);
            callable.call();
        }

        protected Object createNode(Object name) {
            return createNode(name, null, null);
        }

        protected Object createNode(Object name, Object value) {
            return createNode(name, null, value);
        }

        protected Object createNode(Object name, Map attributes) {
            return createNode(name, attributes, null);
        }

        protected Object createNode(Object name, Map attributes, Object value) {
            xml.startNode(name.toString());
            if(attributes != null) {
                for (Object o : attributes.entrySet()) {
                    Map.Entry attribute = (Map.Entry) o;
                    xml.attribute(attribute.getKey().toString(), attribute.getValue().toString());
                }
            }
            if(value != null) {
                xml.convertAnother(value);
            }
            return name;
        }

        @Override
        protected void nodeCompleted(Object o, Object o1) {
            xml.end();
        }

        protected void setParent(Object o, Object o1) {
        }
    }

}
