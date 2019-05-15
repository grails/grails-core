package org.grails.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class DotNotatedKeyParser {

    /**
     * Resolves dot notated getProperty calls on a config object so that injected environmental variables
     * are properly resolved the same as Groovy's dot notation.
     *
     * @param configMap NavigableMap
     * @param key       identifies NavigableMap value to retrieve
     * @return the property value associated with the key
     */
    public static Object getValueWithDotNotatedKeySupport(NavigableMap configMap, String key) {
        if (key == null || configMap == null) {
            return null;
        }

        List<String> keys = convertTokensIntoArrayList(new StringTokenizer(key, "."));
        if (keys.size() == 0) {
            return null;
        }

        Object value = null;
        for (int i = 0; i < keys.size(); i++) {
            if (i == 0) {
                value = configMap.get(keys.get(i));
            } else if (value instanceof Map) {
                value = ((Map) value).get(keys.get(i));
            }
        }
        return value;
    }

    private static List<String> convertTokensIntoArrayList(StringTokenizer st) {
        List<String> elements = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            elements.add(st.nextToken());
        }
        return elements;
    }
}
