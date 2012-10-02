package grails.web;

import org.apache.commons.lang.StringUtils;


public class HyphenatedUrlConverter implements UrlConverter {

    public String toUrlElement(String propertyOrClassName) {
        if (StringUtils.isBlank(propertyOrClassName)) {
            return propertyOrClassName;
        }

        StringBuilder builder = new StringBuilder();
        char[] charArray = propertyOrClassName.toCharArray();
        for (char c : charArray) {
            if (Character.isUpperCase(c)) {
                if (builder.length() > 0) {
                    builder.append("-");
                }
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
