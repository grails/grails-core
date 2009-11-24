package org.codehaus.groovy.grails.web.util
/**
 * An abstract map that provies methods to converting
 * 
 * @author Graeme Rocher
 * @since 1.2
 */

@Category(Map.class)
abstract class TypeConvertingMap implements Map{
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Byte 'byte'(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).byteValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Byte.parseByte(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Integer 'int'(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return o.intValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Integer.parseInt(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining long value from parameter
     * @param name The name of the parameter
     * @return The long value or null if there isn't one
     */
    Long 'long'(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).longValue()
        }
        else if(o != null) {
            try {
                return Long.parseLong(o.toString())
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining short value from parameter
     * @param name The name of the parameter
     * @return The short value or null if there isn't one
     */
    Short 'short'(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).shortValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Short.parseShort(string)
            }
            catch (NumberFormatException e) {
            }
        }

    }

    /**
     * Helper method for obtaining double value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Double 'double'(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).doubleValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Double.parseDouble(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Float 'float'(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).floatValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Float.parseFloat(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Boolean 'boolean'(String name) {
        def o = get(name)
        if(o instanceof Boolean) {
           return o
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Boolean.parseBoolean(string)
            }
            catch (e) {
            }
        }
    }

  /**
     * Helper method for obtaining a list of values from parameter
     * @param name The name of the parameter
     * @return A list of values
     */
    List list(String name) {
        def paramValues = get(name)
        if(paramValues == null) {
            return []
        }
        else if(paramValues?.getClass().isArray()) {
            return Arrays.asList(paramValues)
        }
        else if(paramValues instanceof Collection) {
            return new ArrayList(paramValues)
        }
        else {
            return [paramValues]
        }
    }
}