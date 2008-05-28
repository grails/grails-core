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
import groovy.lang.GString;
import groovy.lang.GroovyObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.web.converters.AbstractConverter;
import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.json.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

/**
 * A converter that converts domain classes, Maps, Lists, Arrays, POJOs and POGOs to JSON
 *
 * @author Siegfried Puchbauer
 */
public class JSON extends AbstractConverter implements Converter {

    private final static Log log = LogFactory.getLog(JSON.class);

    private Object target;
		private String encoding;
		
    private JSONWriter writer;

    private boolean renderDomainClassRelations = false;
    private static final String CACHED_JSON = "org.codehaus.groovy.grails.CACHED_JSON_REQUEST_CONTENT";
    private static final String DEFAULT_ENCODING = "utf-8";

    /**
     * Returns true if the JSON Converter is configured to convert referenced Domain Class instances as they are
     * or just their id's (false)
     *
     * @return true or false ;-)
     */
    public boolean isRenderDomainClassRelations() {
        return renderDomainClassRelations;
    }

    /**
     * @param renderDomainClassRelations
     * @return the JSON instance for chaining
     */
    public JSON setRenderDomainClassRelations(boolean renderDomainClassRelations) {
        this.renderDomainClassRelations = renderDomainClassRelations;
        return this;
    }

    /**
     * Default Constructor for a JSON Converter
     */
    public JSON() {
        Map config = ConfigurationHolder.getFlatConfig();
        Object enc = config.get("grails.converters.encoding");
        if ((enc != null) && (enc.toString().trim().length() > 0)) {
            this.encoding = enc.toString();
        } else {
            this.encoding = DEFAULT_ENCODING;
        }    	    		
        this.target = null;
    }

    /**
     * Creates a new JSON Converter for the given Object
     *
     * @param target the Object to convert
     */
    public JSON(Object target) {
    		this();
        this.target = target;
    }

    /**
     * Directs the JSON Writer to the given Writer
     *
     * @param out the Writer
     * @throws org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
     *
     */
    public void render(Writer out) throws ConverterException {
        this.writer = new JSONWriter(out);
        try {
            value(this.target);
        } finally {
            try {
                out.flush();
                out.close();
            } catch (Exception e) {
                log.warn("Unexpected exception while closing a writer: " + e.getMessage());
            }
        }
    }

    /**
     * Directs the JSON Writer to the Outputstream of the HttpServletResponse and sets the Content-Type to application/json
     *
     * @param response a HttpServletResponse
     * @throws ConverterException
     */
    public void render(HttpServletResponse response) throws ConverterException {
        response.setContentType(GrailsWebUtil.getContentType("application/json",this.encoding));
        try {
            render(response.getWriter());
        } catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    /**
     * Reads the value of the primary identifier property of a domain class instance
     *
     * @param domainObject The domain class instance
     * @param idProperty   The GrailsDomainClassProperty
     * @return The primary identifier of the specified domain object
     */
    protected Object extractIdValue(Object domainObject, GrailsDomainClassProperty idProperty) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainObject);
        return beanWrapper.getPropertyValue(idProperty.getName());
    }

    /**
     * Renders any Java object to the JSON Writer by leveraging the Java reflection API
     *
     * @param o The Bean to render
     * @throws ConverterException
     */
    protected void bean(Object o) throws ConverterException {
        try {
            writer.object();
            BeanInfo info = Introspector.getBeanInfo(o.getClass());
            PropertyDescriptor[] properties = info.getPropertyDescriptors();
            for (int i = 0; i < properties.length; i++) {
                PropertyDescriptor property = properties[i];
                String name = property.getName();
                Method readMethod = property.getReadMethod();
                Class propType = property.getPropertyType();
                if (readMethod != null && !(o instanceof GroovyObject && name.equals("metaClass"))) {
                    Object value = readMethod.invoke(o, (Object[]) null);
                    property(name, value);
                }
            }
            Field[] fields = o.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers) && !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                    property(field.getName(), field.get(o));
                }
            }
            writer.endObject();
        } catch (ConverterException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ConverterException("Error converting Bean with class " + o.getClass().getName(), e);
        }
    }

    /**
     * Renders a instance of a Grails Domain Class to the JSON Writer
     *
     * @param o The Domain Class instance
     * @throws ConverterException
     */
    protected void domain(Object o) throws ConverterException {
        try {
            BeanWrapper beanWrapper = createBeanWrapper(o);
            GrailsDomainClass domainClass = ConverterUtil.getDomainClass(o.getClass().getName());
            if(domainClass != null) {

                writer.object();
                GrailsDomainClassProperty id = domainClass.getIdentifier();
                property(id.getName(), beanWrapper.getPropertyValue(id.getName()));
                property("class", domainClass.getName());
                GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();
                for (int i = 0; i < properties.length; i++) {
                    GrailsDomainClassProperty prop = properties[i];
                    if (!prop.isAssociation() || isRenderDomainClassRelations()) {
                        property(prop.getName(), beanWrapper.getPropertyValue(prop.getName()));
                    } else {
                        writer.key(prop.getName());
                        Object refValue = beanWrapper.getPropertyValue(prop.getName());
                        if (refValue == null) {
                            Class propClass = prop.getType();
                            if (Collection.class.isAssignableFrom(propClass)) {
                                writer.array();
                                writer.endArray();
                            } else if (Map.class.isAssignableFrom(propClass)) {
                                writer.object();
                                writer.endObject();
                            } else {
                                writer.value(null);
                            }
                        } else if (prop.isOneToOne() || prop.isManyToOne() || prop.isEmbedded()) {
                            value(extractIdValue(refValue, prop.getReferencedDomainClass().getIdentifier()));
                        } else {
                            //Class referenceClass = prop.getType();
                            GrailsDomainClassProperty refIdProperty = prop.getReferencedDomainClass().getIdentifier();
                            if (Collection.class.isAssignableFrom(prop.getType())) {
                                writer.array();
                                Collection col = (Collection) refValue;
                                for (Iterator it = col.iterator(); it.hasNext();) {
                                    Object val = it.next();
                                    value(extractIdValue(val, refIdProperty));
                                }
                                writer.endArray();
                            } else if (Map.class.isAssignableFrom(prop.getType())) {
                                writer.object();
                                Map map = (Map) refValue;
                                Iterator it = map.keySet().iterator();
                                while (it.hasNext()) {
                                    String key = (String) it.next(); // Key has be a string
                                    property(key, extractIdValue(map.get(key), refIdProperty));
                                }
                                writer.endObject();
                            } else {
                                throw new ConverterException("Unable to convert property \"" + prop.getName() + "\" of Domain Class \""
                                        + domainClass.getName() + "\": The association class [" + prop.getType().getName() + "] is not a Collection or a Map!");
                            }
                        }
                    }
                }
                writer.endObject();
            }
        } catch (ConverterException ce) {
            throw ce;
        } catch (JSONException e) {
            throw new ConverterException(e);
        }
    }

    /**
     * @param o the array object
     * @throws ConverterException
     */
    public void array(Object o) throws ConverterException {
        try {
            int len = Array.getLength(o);
            writer.array();
            for (int i = 0; i < len; i++) {
                value(Array.get(o, i));

            }
            writer.endArray();
        } catch (JSONException e) {
            throw new ConverterException(e);
        }
    }

    /**
     * @param o
     * @throws ConverterException
     */
    public void value(Object o) throws ConverterException {
        try {
            if (o == null) {
                writer.value(null);
            } else if (o instanceof GroovyObject && ConverterUtil.isDomainClass(o.getClass())) {
                domain(o);
            } else if(GrailsClassUtils.isJdk5Enum(o.getClass())) {
                enumeration(o);
            } else if (o instanceof Class) {
                writer.value(((Class) o).getName());
            } else if (o instanceof Map) {
                writer.object();
                Map map = (Map) o;
                for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                    Object key = it.next();
                    property(key.toString(), map.get(key));
                }
                writer.endObject();
            } else if (o instanceof Collection) {
                writer.array();
                for (Iterator it = ((Collection) o).iterator(); it.hasNext();) {
                    value(it.next());
                }
                writer.endArray();
            } else if (o.getClass().isArray()) {
                array(o);
            } else if (o instanceof String) {
                writer.value(o);
            } else if (o instanceof GString) {
                value(o.toString());
            } else if (o instanceof Date) {
                Date d = (Date) o;
                writer.value(d);
            } else if (o instanceof Calendar) {
                value(((Calendar) o).getTime());
            } else if (o.getClass().equals(byte[].class)) {
                // TODO: How to render binary content?
                value(((byte[]) o).length + " Bytes");
            } else if ((o.getClass().isPrimitive() && !o.getClass().equals(byte[].class))
                    || o instanceof Number || o instanceof Boolean) {
                writer.value(o);
            } else if (o instanceof Currency || o instanceof TimeZone || o instanceof Locale || o instanceof URL) {
                value(o.toString());
            } else {
                bean(o);
            }
        } catch (ConverterException ce) {
            throw ce;
        } catch (JSONException e) {
            throw new ConverterException(e);
        }
    }

    private void enumeration(Object en) throws JSONException {
        writer.object();
        Class enumClass = en.getClass();
        property("enumType", enumClass.getName());
        Method nameMethod = BeanUtils.findDeclaredMethod(enumClass, "name", null);
        try {
            property("name",nameMethod.invoke(en,null));
        } catch (Exception e) {
            property("name", "");
        }
        writer.endObject();
    }

    private void property(String key, Object value) throws JSONException, ConverterException {
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
    public String toString(boolean prettyPrint) throws JSONException {
        String json = super.toString();
        if (prettyPrint) {
            Object jsonObject = new JSONTokener(json).nextValue();
            if (jsonObject instanceof JSONObject)
                return ((JSONObject) jsonObject).toString(3);
            else if (jsonObject instanceof JSONArray)
                return ((JSONArray) jsonObject).toString(3);
        }
        return json;
    }

    /**
     * Parses the given JSON String and returns ether a JSONObject or a JSONArry
     *
     * @param source A string containing some JSON
     * @return ether a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static Object parse(String source) throws ConverterException {
        try {
            return new JSONTokener(source).nextValue();
        } catch (JSONException e) {
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
    public static Object parse(InputStream is, String encoding) throws ConverterException {
        try {
            return parse(IOUtils.toString(is, encoding));
        } catch (IOException e) {
            throw new ConverterException("Error parsing JSON", e);
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
        if(json != null) return json;
        String encoding = request.getCharacterEncoding();
        if (encoding == null)
            encoding = Converter.DEFAULT_REQUEST_ENCODING;
        try {
            json = parse(request.getInputStream(), encoding);
            request.setAttribute(CACHED_JSON,json);
            return json;
        } catch (IOException e) {
            throw new ConverterException("Error parsing JSON", e);
        }
    }

    /**
     * Sets the Object which is later converted to JSON
     *
     * @param target the Object
     * @see org.codehaus.groovy.grails.web.converters.Converter
     */
    public void setTarget(Object target) {
        this.target = target;

    }

}
