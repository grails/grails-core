/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.mapping;

import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * A URL mapping that uses a regular expression in combination with TODO: complete javadoc
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 28, 2007
 *        Time: 6:12:52 PM
 */
public class RegexUrlMapping implements UrlMapping {

    private Pattern pattern;
    private ConstrainedProperty[] constraints = new ConstrainedProperty[0];
    private boolean[] optionalTokens;
    private String controllerName;
    private String actionName;

    /*
    /*
     * @see #RegexUrlMapping(String, String, String, java.util.List)
     */

    public RegexUrlMapping(String pattern, String controllerName, ConstrainedProperty[] constraints) {
        this(pattern,controllerName, null, constraints);
    }

    /**
     * Constructs a new RegexUrlMapping for the given pattern, controller name, action name and constraints.
     *
     * @param pattern The regex
     * @param controllerName The name of the controller the URL maps to (required)
     * @param actionName The name of the action the URL maps to
     * @param constraints A list of ConstrainedProperty instances that relate to tokens in the URL
     *
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty
     */
    public RegexUrlMapping(String pattern, String controllerName, String actionName, ConstrainedProperty[] constraints) {
        if(StringUtils.isBlank(pattern)) throw new IllegalArgumentException("Argument [pattern] cannot be null or blank");
        if(StringUtils.isBlank(controllerName)) throw new IllegalArgumentException("Argument [controllerName] cannot be null or blank");

        this.controllerName = controllerName;
        this.actionName = actionName;
        try {
            this.pattern = Pattern.compile(pattern);
        } catch (PatternSyntaxException pse) {
            throw new UrlMappingException("Error evaluating mapping for pattern ["+pattern+"] from Grails URL mappings: " + pse.getMessage(), pse);
        }
        if(constraints != null) {
            this.constraints = constraints;
        }

    }

    /**
     * Matches the given URI and returns a DefaultUrlMappingInfo instance or null
     *
     *
     * @param uri The URI to match
     * @return A UrlMappingInfo instance or null
     *
     * @see org.codehaus.groovy.grails.web.mapping.UrlMappingInfo
     */
    public UrlMappingInfo match(String uri) {
        Matcher m = this.pattern.matcher(uri);
        if(m.find()) {
              UrlMappingInfo urlInfo = null;
              if(constraints.length > 0) {
                  Map params = new HashMap();
                  Errors errors = new MapBindingResult(params, "urlMapping");

                  for (int i = 0; i < constraints.length; i++) {
                      ConstrainedProperty constraint = constraints[i];
                      String name = constraint.getPropertyName();
                      int groupIndex = i+1;
                      // if the there is no matching group for the constraint then
                      // check if it is allowed to be nullable or blank if it is we can break here
                      // otherwise this URL doesn't match
                      if(m.groupCount() < groupIndex) {
                          if(constraint.isNullable() || !constraint.isBlank()) {
                              break;
                          }
                          else {
                            return null;
                          }
                      }
                      else {
                          String value = m.group(groupIndex);
                          constraint.validate(this, value,errors);
                          if(errors.hasErrors()) {
                              return null;
                          }
                          else {
                              params.put(name, value);
                          }
                      }
                  }
                  urlInfo = new DefaultUrlMappingInfo(this.controllerName, this.actionName, params); 
              }
            else {
                urlInfo = new DefaultUrlMappingInfo(this.controllerName, this.actionName, Collections.EMPTY_MAP);
            }

            return urlInfo;
        }
        return null;
    }
}
