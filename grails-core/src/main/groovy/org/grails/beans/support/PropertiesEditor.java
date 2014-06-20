package org.grails.beans.support;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Fixed version of Spring's PropertiesEditor that converts all keys and values to String values
 * 
 * java.util.Properties should only contain String keys and values
 * 
 * @author Lari Hotari
 * @since 2.3.6
 *
 */
public class PropertiesEditor extends org.springframework.beans.propertyeditors.PropertiesEditor {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setValue(Object value) {
        if (!(value instanceof Properties) && value instanceof Map) {
            Properties props = new Properties();
            for(Map.Entry entry : (Set<Map.Entry>)((Map)value).entrySet()) {
                props.put(String.valueOf(entry.getKey()), entry.getValue() != null ? String.valueOf(entry.getValue()) : null);
            }
            super.setValue(props);
        }
        else {
            super.setValue(value);
        }
    }
}