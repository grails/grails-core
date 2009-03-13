/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.web.converters.marshaller.json;

import grails.converters.JSON;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONWriter;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class GenericJavaBeanMarshaller implements ObjectMarshaller<JSON> {

    public boolean supports(Object object) {
        return true;
    }

    public void marshalObject(Object o, JSON json) throws ConverterException {
        JSONWriter writer = json.getWriter();
        try {
            writer.object();
            BeanInfo info = Introspector.getBeanInfo(o.getClass());
            PropertyDescriptor[] properties = info.getPropertyDescriptors();
            for (PropertyDescriptor property : properties) {
                String name = property.getName();
                Method readMethod = property.getReadMethod();
                if (readMethod != null) {
                    Object value = readMethod.invoke(o, (Object[]) null);
                    writer.key(name);
                    json.convertAnother(value);
                }
            }
            Field[] fields = o.getClass().getDeclaredFields();
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (field.isAccessible() && Modifier.isPublic(modifiers) && !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                    writer.key(field.getName());
                    json.convertAnother(field.get(o));
                }
            }
            writer.endObject();
        } catch (ConverterException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ConverterException("Error converting Bean with class " + o.getClass().getName(), e);
        }

    }
}
