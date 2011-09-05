package grails.web;

import org.apache.commons.lang.StringUtils;


public class HyphenatedUrlConverter implements UrlConverter {

    public String toUrlElement(String propertyOrClassName) {
        if(StringUtils.isBlank(propertyOrClassName)) {
            return propertyOrClassName;
        }
        StringBuffer buffer = new StringBuffer();

        char[] charArray = propertyOrClassName.toCharArray();
        for(char c : charArray) {
            if(Character.isUpperCase(c)) {
                if(buffer.length() > 0) {
                    buffer.append("-");
                }
                buffer.append(Character.toLowerCase(c));
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

}
