/*
 * Copyright 2008 the original author or authors.
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
package org.codehaus.groovy.grails.cli.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * Aids in binding a mock JNDI context from Grails' JNDI configuration format
 *
 * @author Graeme Rocher
 * @since 1.2.3
 */
public class JndiBindingSupport {

    private static Map<String, JndiBindingHandler> jndiBinders = new HashMap<String, JndiBindingHandler>();
    static {
        DataSourceBinder dsBinder = new DataSourceBinder();
        jndiBinders.put(dsBinder.getType(), dsBinder);
    }

    private static final String DATA_SOURCE = "javax.sql.DataSource";
    private static final Object TYPE = "type";
    private Map<String, Object> jndiConfig;

    public JndiBindingSupport(Map<String, Object> jndiConfig) {
        this.jndiConfig = jndiConfig;
    }

    /**
     * Used to register a new JNDI binding handler
     *
     * @param handler The binding handler
     */
    public static void registerJndiBindingHandler(JndiBindingHandler handler) {
        if (handler != null && handler.getType() != null) {
            jndiBinders.put(handler.getType(), handler);
        }
    }

    /**
     * Bindings a JNDI context.
     *
     * @return The bound JNDI context
     */
    @SuppressWarnings("unchecked")
    Object bind() {
        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();

        if (jndiConfig != null) {
            for (Object o : jndiConfig.entrySet()) {
                Map.Entry entry = (Map.Entry)o;

                Object propsObj = entry.getValue();

                final String entryName = entry.getKey().toString();
                if (propsObj instanceof Map) {
                    Map<String, Object> props = (Map)propsObj;
                    Object typeObj = props.get(TYPE);

                    if (typeObj != null) {
                        props.remove(TYPE);
                        String type = typeObj.toString();

                        JndiBindingHandler handler = jndiBinders.get(type);
                        if (handler != null) {
                            handler.handleBinding(builder, entryName, props);
                        }
                        else {
                            try {
                                Class<?> c = Class.forName(type, true, Thread.currentThread().getContextClassLoader());
                                Object beanObj = BeanUtils.instantiate(c);
                                bindProperties(beanObj, props);
                                builder.bind(entryName, beanObj);
                            }
                            catch (BeanInstantiationException e) {
                                // ignore
                            }
                            catch (ClassNotFoundException e) {
                                // ignore
                            }
                        }
                    }
                }
                else {
                    builder.bind(entryName, propsObj);
                }
            }
        }

        try {
            builder.activate();
            return builder.createInitialContextFactory(null).getInitialContext(null);
        }
        catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void bindProperties(Object obj, Map entryProperties) {
        BeanWrapper dsBean = new BeanWrapperImpl(obj);
        for (Object o : entryProperties.entrySet()) {
            Map.Entry entry2 = (Map.Entry)o;
            final String propertyName = entry2.getKey().toString();
            if (dsBean.isWritableProperty(propertyName)) {
                dsBean.setPropertyValue(propertyName, entry2.getValue());
            }
        }
    }

    static class DataSourceBinder implements JndiBindingHandler {

        public String getType() {
            return DATA_SOURCE;
        }

        @SuppressWarnings("unchecked")
        public void handleBinding(SimpleNamingContextBuilder builder,
                String entryName, Map entryProperties) {
            try {
                Object ds = BeanUtils.instantiate(Class.forName("org.apache.commons.dbcp.BasicDataSource",true, Thread.currentThread().getContextClassLoader()));
                bindProperties(ds, entryProperties);
                builder.bind(entryName, ds);
            }
            catch (Exception e) {
                // ignore
            }
        }
    }
}
