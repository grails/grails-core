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

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <p>A UrlMapping implementation that takes a Grails URL pattern and turns it into a regex matcher so that
 * URLs can be matched and information captured from the match.</p>
 *
 * <p>A Grails URL pattern is not a regex, but is an extension to the form defined by Apache Ant and used by
 * Spring AntPathMatcher. Unlike regular Ant paths Grails URL patterns allow for capturing groups in the form:</p>
 *
 * <code>/blog/(*)/**</code>
 *
 * <p>The parenthesis define a capturing group. This implementation transforms regular Ant paths into regular expressions
 * that are able to use capturing groups</p>
 * 
 *
 * @see org.springframework.util.AntPathMatcher
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 28, 2007
 *        Time: 6:12:52 PM
 */
public class RegexUrlMapping extends AbstractUrlMapping implements UrlMapping {

    private Pattern[] patterns;
    private UrlMappingData urlData;
    
    private static final String WILDCARD = "*";
    private static final String CAPTURED_WILDCARD = "(*)";
    private static final String SLASH = "/";
    private static final char QUESTION_MARK = '?';
    private static final String ENTITY_AMPERSAND = "&amp;";
    private static final char AMPERSAND = '&';
    private static final String DOUBLE_WILDCARD = "**";
    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final String CAPTURED_DOUBLE_WILDCARD = "(**)";
    private static final Log LOG = LogFactory.getLog(RegexUrlMapping.class);


    /**
     * Constructs a new RegexUrlMapping for the given pattern, controller name, action name and constraints.
     *
     * @param data An instance of the UrlMappingData class that holds necessary information of the URL mapping
     * @param controllerName The name of the controller the URL maps to (required)
     * @param actionName The name of the action the URL maps to
     * @param viewName The name of the view as an alternative to the name of the action. If the action is specified it takes precedence over the view name during mapping
     * @param constraints A list of ConstrainedProperty instances that relate to tokens in the URL
     *
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty
     */
    public RegexUrlMapping(UrlMappingData data, Object controllerName, Object actionName, Object viewName, ConstrainedProperty[] constraints) {
        super(controllerName, actionName, viewName, constraints != null ? constraints : new ConstrainedProperty[0]);
        parse(data, constraints);
    }

    private void parse(UrlMappingData data, ConstrainedProperty[] constraints) {
        if(data == null) throw new IllegalArgumentException("Argument [pattern] cannot be null");

        String[] urls = data.getLogicalUrls();
        this.urlData = data;
        this.patterns = new Pattern[urls.length];

        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];

            Pattern pattern = convertToRegex(url);
            if(pattern == null) throw new IllegalStateException("Cannot use null pattern in regular expression mapping for url ["+data.getUrlPattern()+"]");
            this.patterns[i] = pattern;

        }
        if(constraints != null) {
            for (int i = 0; i < constraints.length; i++) {

                ConstrainedProperty constraint = constraints[i];
                if(data.isOptional(i)) {
                    constraint.setNullable(true);
                }
            }
        }
    }



    /**
     * Converst a Grails URL provides via the UrlMappingData interface to a regular expression
     *
     * @param url The URL to convert
     * @return A regex Pattern objet
     */
    protected Pattern convertToRegex(String url) {
        Pattern regex;
        String pattern = null;
        try {
            pattern = "^" + url.replaceAll("([^\\*])\\*([^\\*])", "$1[^/]+$2").replaceAll("([^\\*])\\*$", "$1[^/]+").replaceAll("\\*\\*", ".*");
            pattern += "/??$";
            regex = Pattern.compile(pattern);
        } catch (PatternSyntaxException pse) {
            throw new UrlMappingException("Error evaluating mapping for pattern ["+pattern+"] from Grails URL mappings: " + pse.getMessage(), pse);
        }

        return regex;
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
    	for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = patterns[i];
            Matcher m = pattern.matcher(uri);
            if(m.matches()) {
                  UrlMappingInfo urlInfo = createUrlMappingInfo(uri,m);
                  if(urlInfo!=null) {
                      return urlInfo;
                  }
            }

        }
        return null;
    }

    /**
     * @see org.codehaus.groovy.grails.web.mapping.UrlMapping
     */
    public String createURL(Map parameterValues, String encoding) {
        return createURLInternal(parameterValues, encoding, true);
    }

    private String createURLInternal(Map parameterValues, String encoding, boolean includeContextPath) {

        if(encoding == null) encoding = "utf-8";

        String contextPath = "";
        if(includeContextPath) {
            GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
            if(webRequest != null) {
                contextPath = webRequest.getAttributes().getApplicationUri(webRequest.getCurrentRequest());
            }
        }
        if(parameterValues==null)parameterValues= Collections.EMPTY_MAP;
        StringBuffer uri = new StringBuffer(contextPath);
        Set usedParams = new HashSet();

        String[] tokens = urlData.getTokens();
        int paramIndex = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if(CAPTURED_WILDCARD.equals(token) || CAPTURED_DOUBLE_WILDCARD.equals(token)) {
                ConstrainedProperty prop = this.constraints[paramIndex++];
                String propName = prop.getPropertyName();
                Object value = parameterValues.get(propName);
                usedParams.add(propName);
                if(value == null && !prop.isNullable())
                    throw new UrlMappingException("Unable to create URL for mapping ["+this+"] and parameters ["+parameterValues+"]. Parameter ["+prop.getPropertyName()+"] is required, but was not specified!");
                else if(value == null)
                    break;

                try {
                    uri.append(SLASH).append(URLEncoder.encode(value.toString(), encoding));
                } catch (UnsupportedEncodingException e) {
                    throw new ControllerExecutionException("Error creating URL for parameters ["+parameterValues +"], problem encoding URL part ["+value +"]: " + e.getMessage(),e);
                }
            }
            else {
                uri.append(SLASH).append(token);
            }

        }
        populateParameterList(parameterValues, encoding, uri, usedParams);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Created reverse URL mapping ["+uri.toString()+"] for parameters ["+parameterValues+"]");
        }
        return  uri.toString();
    }

    public String createURL(Map parameterValues, String encoding, String fragment) {
        String url = createURL(parameterValues, encoding);
        return createUrlWithFragment(url, fragment, encoding);
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding) {
        return createURLInternal(controller, action, parameterValues, encoding, true);
    }

    private String createURLInternal(String controller, String action, Map parameterValues, String encoding, boolean includeContextPath) {
        if(parameterValues == null) parameterValues = new HashMap();

        boolean hasController = !StringUtils.isBlank(controller);
        boolean hasAction = !StringUtils.isBlank(action);

        try {

            if(hasController)
                parameterValues.put(CONTROLLER, controller);
            if(hasAction)
                parameterValues.put(ACTION, action);

            return createURLInternal(parameterValues, encoding, includeContextPath);
        } finally {
            if(hasController)
                parameterValues.remove(CONTROLLER);
            if(hasAction)
                parameterValues.remove(ACTION);

        }
    }

    public String createRelativeURL(String controller, String action, Map parameterValues, String encoding) {
        return createURLInternal(controller, action, parameterValues, encoding, false);
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding, String fragment) {
        String url = createURL(controller, action, parameterValues, encoding);
        return createUrlWithFragment(url, fragment, encoding);
    }

    private String createUrlWithFragment(String url, String fragment, String encoding) {
        if(fragment != null) {
            // A 'null' encoding will cause an exception, so default to 'UTF-8'.
            if (encoding == null) {
                encoding = DEFAULT_ENCODING;
            }

            try {
                return url + '#' + URLEncoder.encode(fragment, encoding);
            } catch (UnsupportedEncodingException ex) {
                throw new ControllerExecutionException("Error creating URL  ["+url +"], problem encoding URL fragment ["+fragment +"]: " + ex.getMessage(),ex);
            }
        }
        else {
            return url;
        }
    }


    private void populateParameterList(Map parameterValues, String encoding, StringBuffer uri, Set usedParams) {
        boolean addedParams = false;
        usedParams.add( "controller" );
        usedParams.add( "action" );

        // A 'null' encoding will cause an exception, so default to 'UTF-8'.
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }

        for (Iterator i = parameterValues.keySet().iterator(); i.hasNext();) {
            String name = i.next().toString();
            if(!usedParams.contains(name)) {
                if(!addedParams) {
                    uri.append(QUESTION_MARK);
                    addedParams = true;
                }
                else {
                    uri.append(AMPERSAND);
                }
                Object value = parameterValues.get(name);
                if(value != null && value instanceof Collection) {
                    Collection multiValues = (Collection)value;
                    for (Iterator j = multiValues.iterator(); j.hasNext();) {
                        Object o = j.next();
                        appendValueToURI(encoding, uri, name, o);
                        if(j.hasNext()) {
                            uri.append(AMPERSAND);
                        }
                    }
                }
                else if(value!= null && value.getClass().isArray()) {
                    Object[] multiValues = (Object[])value;
                    for (int j = 0; j < multiValues.length; j++) {
                        Object o = multiValues[j];
                        appendValueToURI(encoding, uri, name, o);
                        if(j+1 < multiValues.length) {
                            uri.append(AMPERSAND);
                        }
                    }
                }
                else {
                    appendValueToURI(encoding, uri, name, value);
                }
            }

        }
    }

    private void appendValueToURI(String encoding, StringBuffer uri, String name, Object value) {
        try {
            uri.append(URLEncoder.encode(name,encoding)).append('=')
                    .append(URLEncoder.encode(value != null ? value.toString() : "",encoding));
        } catch (UnsupportedEncodingException e) {
            throw new ControllerExecutionException("Error redirecting request for url ["+name+":"+value +"]: " + e.getMessage(),e);
        }
    }

    public UrlMappingData getUrlData() {
        return this.urlData;
    }

    private UrlMappingInfo createUrlMappingInfo(String uri, Matcher m) {
        Map params = new HashMap();
        Errors errors = new MapBindingResult(params, "urlMapping");
        String lastGroup = null;
        for (int i = 0; i < m.groupCount(); i++) {
            lastGroup = m.group(i+1);
            int j = lastGroup.indexOf('?');
            if(j >-1) {
                lastGroup = lastGroup.substring(0,j);
            }
            if(constraints.length > i) {
                ConstrainedProperty cp = constraints[i];
                cp.validate(this,lastGroup, errors);

                if(errors.hasErrors()) return null;
                else {
                    params.put(cp.getPropertyName(), lastGroup);
                }
            }
        }

        if(lastGroup!= null) {
            String remainingUri = uri.substring(uri.lastIndexOf(lastGroup)+lastGroup.length());
            if(remainingUri.length() > 0) {
                if(remainingUri.startsWith(SLASH))remainingUri = remainingUri.substring(1);
                String[] tokens = remainingUri.split(SLASH);
                for (int i = 0; i < tokens.length; i=i+2) {
                    String token = tokens[i];
                    if((i+1) < tokens.length) {
                        params.put(token, tokens[i+1]);
                    }

                }
            }
        }

        for (Iterator i = this.parameterValues.keySet().iterator(); i.hasNext();) {
            Object key =  i.next();
            params.put(key, this.parameterValues.get(key));            
        }

        if(controllerName == null) {
            this.controllerName = createRuntimeConstraintEvaluator(GrailsControllerClass.CONTROLLER, this.constraints);
        }

        if(actionName == null) {
            this.actionName = createRuntimeConstraintEvaluator(GrailsControllerClass.ACTION, this.constraints);
        }

        if(viewName == null) {
            this.viewName = createRuntimeConstraintEvaluator(GrailsControllerClass.VIEW, this.constraints);
        }

        if (viewName != null && this.controllerName == null) {
           return new DefaultUrlMappingInfo(viewName, params,this.urlData);
        }
        else {
            return new DefaultUrlMappingInfo(this.controllerName, this.actionName,getViewName(), params,this.urlData);
        }
    }

    /**
     * This method will look for a constraint for the given name and return a closure that when executed will
     * attempt to evaluate its value from the bound request parameters at runtime.
     *
     * @param name The name of the constrained property
     * @param constraints The array of current ConstrainedProperty instances
     * @return Either a Closure or null
     */
    private Object createRuntimeConstraintEvaluator(final String name, ConstrainedProperty[] constraints) {
        if(constraints == null)return null;

        for (int i = 0; i < constraints.length; i++) {
            ConstrainedProperty constraint = constraints[i];
            if(constraint.getPropertyName().equals(name)) {
                return new Closure(this) {
                    public Object call(Object[] objects) {
                        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
                        return webRequest.getParams().get(name);
                    }
                };
            }
        }
        return null;  
    }


    public String[] getLogicalMappings() {
        return this.urlData.getLogicalUrls(); 
    }

    /**
     * Compares this UrlMapping instance with the specified UrlMapping instance and deals with URL mapping precedence rules.
     *
     * @param o An instance of the UrlMapping interface
     * @return 1 if this UrlMapping should match before the specified UrlMapping. 0 if they are equal or -1 if this UrlMapping should match after the given UrlMapping
     */
    public int compareTo(Object o) {
        if(!(o instanceof UrlMapping)) throw new IllegalArgumentException("Cannot compare with Object ["+o+"]. It is not an instance of UrlMapping!");

        UrlMapping other = (UrlMapping)o;

        String[] otherTokens = other
                                   .getUrlData()
                                   .getTokens();


        String[] tokens = getUrlData().getTokens();

        if( isWildcard(this) && !isWildcard( other ) ) return -1;
        if( !isWildcard(this) && isWildcard( other ) ) return 1;
        
        if(tokens.length < otherTokens.length) {
            return -1;
        }
        else if(tokens.length > otherTokens.length) {
            return 1;
        }

        int result = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if(otherTokens.length > i) {
                String otherToken = otherTokens[i];
                if(isWildcard(token) && !isWildcard(otherToken)) {
                    result = -1;
                    break;
                }
                else if(!isWildcard(token) && isWildcard(otherToken)) {
                    result = 1;
                    break;
                }                
            }
            else {
                break;
            }
        }
        return result;
    }

    private boolean isWildcard(UrlMapping mapping) {
        String[] tokens = mapping.getUrlData().getTokens();
        for( int i = 0; i < tokens.length; i++ ) {
            if( isWildcard( tokens[i] )) return true;
        }
        return false;
    }

    private boolean isWildcard(String token) {
        return WILDCARD.equals(token) || CAPTURED_WILDCARD.equals(token) || DOUBLE_WILDCARD.equals(token) || CAPTURED_DOUBLE_WILDCARD.equals(token);
    }


    public String toString() {
        return this.urlData.getUrlPattern();
    }
}
