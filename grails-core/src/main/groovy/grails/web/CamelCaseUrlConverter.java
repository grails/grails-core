/*
 * Copyright 2011 SpringSource.
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
package grails.web;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;

/**
 * URL converter that allows for camel case URLs
 *
 * @author Jeff Brown
 * @since 2.0
 */
public class CamelCaseUrlConverter implements UrlConverter {

    public String toUrlElement(String propertyOrClassName) {
        if (StringUtils.isBlank(propertyOrClassName)) {
            return propertyOrClassName;
        }
        if (propertyOrClassName.length() > 1 && Character.isUpperCase(propertyOrClassName.charAt(0)) &&
                Character.isUpperCase(propertyOrClassName.charAt(1))) {
            return propertyOrClassName;
        }

        return propertyOrClassName.substring(0,1).toLowerCase(Locale.ENGLISH) + propertyOrClassName.substring(1);
    }
}
