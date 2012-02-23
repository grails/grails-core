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

import grails.util.GrailsWebUtil;
import groovy.lang.Closure;
import groovy.util.BuilderSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONArray;
import org.codehaus.groovy.grails.web.json.JSONElement;
import org.codehaus.groovy.grails.web.json.JSONException;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.codehaus.groovy.grails.web.json.JSONTokener;
import org.codehaus.groovy.grails.web.json.JSONWriter;
import org.codehaus.groovy.grails.web.json.PathCapturingJSONWriterWrapper;
import org.codehaus.groovy.grails.web.json.PrettyPrintJSONWriter;

/**
 * A converter that converts domain classes, Maps, Lists, Arrays, POJOs and POGOs to JSON.
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 */
public class JSON extends AbstractConverter<JSONWriter> implements Converter<JSONWriter> {

    private final static Log log = LogFactory.getLog(JSON.class);
    private static final String CACHED_JSON = "org.codehaus.groovy.grails.CACHED_JSON_REQUEST_CONTENT";

    private Object target;
    private final String encoding;
    private final ConverterConfiguration<JSON> config;
    private final CircularReferenceBehaviour circularReferenceBehaviour;
    private boolean prettyPrint;

    protected JSONWriter writer;
    protected Stack<Object> referenceStack;

    protected ConverterConfiguration<JSON> initConfig() {
        return ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
    }

    /**
     * Default Constructor for a JSON Converter
     */
    public JSON() {
        config = initConfig();
        encoding = config != null ? config.getEncoding() : "UTF-8";
        circularReferenceBehaviour = config != null ? config.getCircularReferenceBehaviour() : CircularReferenceBehaviour.DEFAULT;
        prettyPrint = config != null && config.isPrettyPrint();
    }

    /**
     * Creates a new JSON Converter for the given Object
     *
     * @param target the Object to convert
     */
    public JSON(Object target) {
        this();
        setTarget(target);
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    private void prepareRender(Writer out) {
        writer = prettyPrint ? new PrettyPrintJSONWriter(out) : new JSONWriter(out);
        if (circularReferenceBehaviour == CircularReferenceBehaviour.PATH) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Using experimental CircularReferenceBehaviour.PATH for %s", getClass().getName()));
            }
            writer = new PathCapturingJSONWriterWrapper(writer);
        }
        referenceStack = new Stack<Object>();
    }

    private void finalizeRender(Writer out) {
        try {
            out.flush();
            out.close();
        }
        catch (Exception e) {
            log.warn("Unexpected exception while closing a writer: " + e.getMessage());
        }
    }

    /**
     * Directs the JSON Writer to the given Writer
     *
     * @param out the Writer
     * @throws org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
     *
     */
    public void render(Writer out) throws ConverterException {
        prepareRender(out);
        try {
            value(target);
        }
        finally {
            finalizeRender(out);
        }
    }

    /**
     * Directs the JSON Writer to the Outputstream of the HttpServletResponse and sets the Content-Type to application/json
     *
     * @param response a HttpServletResponse
     * @throws ConverterException
     */
    public void render(HttpServletResponse response) throws ConverterException {
        response.setContentType(GrailsWebUtil.getContentType("application/json", encoding));
        try {
            render(response.getWriter());
        }
        catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    public JSONWriter getWriter() throws ConverterException {
        return writer;
    }

    public void convertAnother(Object o) throws ConverterException {
        value(o);
    }

    public void build(@SuppressWarnings("rawtypes") Closure c) throws ConverterException {
        new Builder(this).execute(c);
    }

    /**
     * @param o
     * @throws ConverterException
     */
    public void value(Object o) throws ConverterException {
        o = config.getProxyHandler().unwrapIfProxy(o);
        try {
            if (o == null || o.equals(JSONObject.NULL)) {
                writer.value(null);
            }
            else if (o instanceof CharSequence) {
                writer.value(o);
            }
            else if (o instanceof Class<?>) {
                writer.value(((Class<?>)o).getName());
            }
            else if ((o.getClass().isPrimitive() && !o.getClass().equals(byte[].class)) ||
                    o instanceof Number || o instanceof Boolean) {
                writer.value(o);
            }
            else {
                if (referenceStack.contains(o)) {
                    handleCircularRelationship(o);
                }
                else {
                    referenceStack.push(o);
                    ObjectMarshaller<JSON> marshaller = config.getMarshaller(o);
                    if (marshaller == null) {
                        throw new ConverterException("Unconvertable Object of class: " + o.getClass().getName());
                    }
                    marshaller.marshalObject(o, this);
                    referenceStack.pop();
                }
            }
        }
        catch (ConverterException ce) {
            throw ce;
        }
        catch (JSONException e) {
            throw new ConverterException(e);
        }
    }

    public ObjectMarshaller<JSON> lookupObjectMarshaller(@SuppressWarnings("hiding") Object target) {
        return config.getMarshaller(target);
    }

    public int getDepth() {
        return referenceStack.size();
    }

    public void property(String key, Object value) throws JSONException, ConverterException {
        writer.key(key);
        value(value);
    }

    /**
     * Performs the conversion and returns the resulting JSON as String
     *
     * @param prettyPrint true, if the output should be indented, otherwise false
     * @return a JSON String
     * @throws JSONException
     */
    public String toString(@SuppressWarnings("hiding") boolean prettyPrint) throws JSONException {
        String json = super.toString();
        if (prettyPrint) {
            Object jsonObject = new JSONTokener(json).nextValue();
            if (jsonObject instanceof JSONObject) {
                return ((JSONObject) jsonObject).toString(3);
            }
            if (jsonObject instanceof JSONArray) {
                return ((JSONArray) jsonObject).toString(3);
            }
        }
        return json;
    }

    /**
     * Parses the given JSON String and returns ether a JSONObject or a JSONArry
     *
     * @param reader JSON source
     * @return ether a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static JSONElement parse(Reader reader) throws ConverterException {
//        TODO: Migrate to new javacc based parser
//        JSONParser parser = new JSONParser(reader);
//        try {
//            return parser.parseJSON();
//        }
//        catch (ParseException e) {
//            throw new ConverterException("Error parsing JSON: " + e.getMessage(), e);
//        }

        try {
            return parse(IOUtils.toString(reader));
        }
        catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    /**
     * Parses the given JSON String and returns ether a JSONObject or a JSONArry
     *
     * @param source A string containing some JSON
     * @return ether a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static JSONElement parse(String source) throws ConverterException {
        // TODO: Migrate to new javacc based parser
        try {
            final Object value = new JSONTokener(source).nextValue();
            if (value instanceof JSONElement) {
                return (JSONElement) value;
            }

            // return empty object
            return new JSONObject();
        }
        catch (JSONException e) {
            throw new ConverterException("Error parsing JSON", e);
        }
    }

    /**
     * Parses the given JSON and returns ether a JSONObject or a JSONArry
     *
     * @param is       An InputStream which delivers some JSON
     * @param encoding the Character Encoding to use
     * @return ether a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static JSONElement parse(InputStream is, String encoding) throws ConverterException {
//        TODO: Migrate to new javacc based parser
//        JSONParser parser = new JSONParser(is, encoding);
//        try {
//            return parser.parseJSON();
//        }
//        catch (ParseException e) {
//            throw new ConverterException("Error parsing JSON: " + e.getMessage(), e);
//        }
        try {
            return parse(IOUtils.toString(is, encoding));
        }
        catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    /**
     * Parses the given request's InputStream and returns ether a JSONObject or a JSONArry
     *
     * @param request the JSON Request
     * @return ether a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static Object parse(HttpServletRequest request) throws ConverterException {
        Object json = request.getAttribute(CACHED_JSON);
        if (json != null) return json;

        String encoding = request.getCharacterEncoding();
        if (encoding == null) {
            encoding = Converter.DEFAULT_REQUEST_ENCODING;
        }
        try {
            PushbackInputStream pushbackInputStream = null;
            int firstByte = -1;
            try {
                pushbackInputStream = new PushbackInputStream(request.getInputStream());
                firstByte = pushbackInputStream.read();
            } catch (IOException ioe) {}
            
            if(firstByte == -1) {
                return new JSONObject();
            }
            
            pushbackInputStream.unread(firstByte);
            json = parse(pushbackInputStream, encoding);
            request.setAttribute(CACHED_JSON, json);
            return json;
        }
        catch (IOException e) {
            throw new ConverterException("Error parsing JSON", e);
        }
    }

    /**
     * Sets the Object which is later converted to JSON
     *
     * @param target the Object
     * @see org.codehaus.groovy.grails.web.converters.Converter
     */
    @Override
    public void setTarget(Object target) {
        this.target = target;
    }

    protected void handleCircularRelationship(Object o) throws ConverterException {
        switch (circularReferenceBehaviour) {
            case DEFAULT:
                if (!(Map.class.isAssignableFrom(o.getClass()) || Collection.class.isAssignableFrom(o.getClass()))) {
                    Map<String, Object> props = new HashMap<String, Object>();
                    props.put("class", o.getClass());
                    StringBuilder ref = new StringBuilder();
                    int idx = referenceStack.indexOf(o);
                    for (int i = referenceStack.size() - 1; i > idx; i--) {
                        ref.append("../");
                    }
                    props.put("_ref", ref.substring(0, ref.length() - 1));
                    value(props);
                }
                break;
            case EXCEPTION:
                throw new ConverterException("Circular Reference detected: class " + o.getClass().getName());
            case INSERT_NULL:
                value(null);
                break;
            case PATH:
                Map<String, Object> props = new HashMap<String, Object>();
                props.put("class", o.getClass());
                int idx = referenceStack.indexOf(o);
                PathCapturingJSONWriterWrapper pcWriter = (PathCapturingJSONWriterWrapper) writer;
                props.put("ref", String.format("root%s", pcWriter.getStackReference(idx)));
                value(props);
                break;
            case IGNORE:
                break;
        }
    }

    public static ConverterConfiguration<JSON> getNamedConfig(String configName) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getNamedConverterConfiguration(configName, JSON.class);
        if (cfg == null) {
            throw new ConverterException(String.format("Converter Configuration with name '%s' not found!", configName));
        }
        return cfg;
    }

    public static Object use(String configName, Closure<?> callable) throws ConverterException {
        ConverterConfiguration<JSON> old = ConvertersConfigurationHolder.getThreadLocalConverterConfiguration(JSON.class);
        ConverterConfiguration<JSON> cfg = getNamedConfig(configName);
        ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(JSON.class, cfg);
        try {
            return callable.call();
        }
        finally {
            ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(JSON.class, old);
        }
    }

    public static void use(String cfgName) throws ConverterException {
        if (cfgName == null || "default".equals(cfgName)) {
            ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(JSON.class, null);
        }
        else {
            ConvertersConfigurationHolder.setTheadLocalConverterConfiguration(JSON.class, getNamedConfig(cfgName));
        }
    }

    public static void registerObjectMarshaller(Class<?> clazz, Closure<?> callable) throws ConverterException {
        registerObjectMarshaller(new ClosureOjectMarshaller<JSON>(clazz, callable));
    }

    public static void registerObjectMarshaller(Class<?> clazz, int priority, Closure<?> callable) throws ConverterException {
        registerObjectMarshaller(new ClosureOjectMarshaller<JSON>(clazz, callable), priority);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<JSON> om) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
        if (cfg == null) {
            throw new ConverterException("Default Configuration not found for class " + JSON.class.getName());
        }
        if (!(cfg instanceof DefaultConverterConfiguration<?>)) {
            cfg = new DefaultConverterConfiguration<JSON>(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
        }
        ((DefaultConverterConfiguration<JSON>) cfg).registerObjectMarshaller(om);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<JSON> om, int priority) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
        if (cfg == null) {
            throw new ConverterException("Default Configuration not found for class " + JSON.class.getName());
        }
        if (!(cfg instanceof DefaultConverterConfiguration<?>)) {
            cfg = new DefaultConverterConfiguration<JSON>(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
        }
        ((DefaultConverterConfiguration<JSON>) cfg).registerObjectMarshaller(om, priority);
    }

    public static void createNamedConfig(String name, Closure<?> callable) throws ConverterException {
        DefaultConverterConfiguration<JSON> cfg = new DefaultConverterConfiguration<JSON>(ConvertersConfigurationHolder.getConverterConfiguration(JSON.class));
        try {
            callable.call(cfg);
            ConvertersConfigurationHolder.setNamedConverterConfiguration(JSON.class, name, cfg);
        }
        catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
    }

    public static void withDefaultConfiguration(Closure<?> callable) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
        if (!(cfg instanceof DefaultConverterConfiguration<?>)) {
            cfg = new DefaultConverterConfiguration<JSON>(cfg);
        }
        try {
            callable.call(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
        }
        catch (Throwable t) {
            throw ConverterUtil.resolveConverterException(t);
        }
    }

    public class Builder extends BuilderSupport {

        private JSON json;

        public Builder(JSON json) {
            this.json = json;
            writer = json.writer;
        }

        public void execute(Closure<?> callable) {
            callable.setDelegate(this);
//            callable.setDelegate(Closure.DELEGATE_FIRST);
            invokeMethod("json", new Object[] { callable });
        }

        private Stack<BuilderMode> stack = new Stack<BuilderMode>();
        private boolean start = true;
        @SuppressWarnings("hiding")
        private JSONWriter writer;

        @Override
        protected Object createNode(Object name) {
            int retVal = 1;
            try {
                if (start) {
                    start = false;
                    writeObject();
                }
                else {
                    if (getCurrent() == null && stack.peek() == BuilderMode.OBJECT) {
                        throw new IllegalArgumentException("only call to [element { }] is allowed when creating array");
                    }
                    if (stack.peek() == BuilderMode.ARRAY) {
                        writeObject();
                        retVal = 2;
                    }
                    writer.key(String.valueOf(name)).array();
                    stack.push(BuilderMode.ARRAY);
                }
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element");
            }

            return retVal;
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected Object createNode(Object key, Map valueMap) {
            try {
                if (stack.peek().equals(BuilderMode.OBJECT)) writer.key(String.valueOf(key));
                writer.object();
                for (Object o : valueMap.entrySet()) {
                    Map.Entry element = (Map.Entry) o;
                    writer.key(String.valueOf(element.getKey()));//.value(element.getValue());
                    json.convertAnother(element.getValue());
                }
                writer.endObject();
                return null;
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element");
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected Object createNode(Object arg0, Map arg1, Object arg2) {
            throw new IllegalArgumentException("not implemented");
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected Object createNode(Object key, Object value) {
            if (getCurrent() == null && stack.peek()== BuilderMode.OBJECT) {
                throw new IllegalArgumentException("only call to [element { }] is allowed when creating array");
            }

            try {
                int retVal = 0;
                if (stack.peek().equals(BuilderMode.ARRAY)) {
                    writeObject();
                    retVal = 1;
                }
                if (value instanceof Collection) {
                    Collection c = (Collection)value;
                    writer.key(String.valueOf(key));
                    handleCollectionRecurse(c);
                }
                else {
                    writer.key(String.valueOf(key));
                    json.convertAnother(value); //.value(value);
                }
                return retVal != 0 ? retVal : null;
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element");
            }
        }

        @SuppressWarnings("rawtypes")
        private void handleCollectionRecurse(Collection c) throws JSONException {
            writer.array();
            for (Object element : c) {
                if (element instanceof Collection) {
                    handleCollectionRecurse((Collection) element);
                }
                else {
                    json.convertAnother(element);
                }
            }
            writer.endArray();
        }

        @Override
        protected void nodeCompleted(Object parent, Object node) {
            Object last = null;

            if (node == null) {
                return;
            }

            try {
                int i = ((Integer)node);
                while (i-- > 0) {
                    last = stack.pop();
                    if (BuilderMode.ARRAY == last) writer.endArray();
                    if (BuilderMode.OBJECT == last) writer.endObject();
                }
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element on the stack");
            }
        }

        @Override
        protected void setParent(Object arg0, Object arg1) {
            /* do nothing */
        }

        private void writeObject() throws JSONException {
            writer.object();
            stack.push(BuilderMode.OBJECT);
        }
    }

    private enum BuilderMode {
        ARRAY,
        OBJECT
    }
}
