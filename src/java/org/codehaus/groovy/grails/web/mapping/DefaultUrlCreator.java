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

import grails.util.GrailsWebUtil;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.grails.web.pages.FastStringWriter;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * The default implementation of the UrlCreator interface that constructs URLs in Grails
 * default pattern of /controllerName/actionName/id
 *
 * @author Graeme Rocher
 * @since 0.5.5
 *
 *        <p/>
 *        Created: May 30, 2007
 *        Time: 8:37:15 AM
 */
public class DefaultUrlCreator implements UrlCreator {
    private static final char SLASH = '/';
    private final String controllerName;
    private final String actionName;
    public static final String ARGUMENT_ID = "id";
    private static final String ENTITY_AMPERSAND = "&";

    public DefaultUrlCreator(String controller, String action) {
        this.controllerName = controller;
        this.actionName = action;
    }

    public String createURL(Map parameterValues, String encoding) {
        if(parameterValues == null) parameterValues = Collections.EMPTY_MAP;
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        return createURLWithWebRequest(parameterValues, webRequest, true);
    }

    public String createURL(Map parameterValues, String encoding, String fragment) {
        String url = createURL( parameterValues, encoding);
        return createUrlWithFragment(encoding, fragment, url);

    }

    private String createURLWithWebRequest(Map parameterValues, GrailsWebRequest webRequest, boolean includeContextPath) {
        HttpServletRequest request = webRequest.getCurrentRequest();

        String id = null;
        if(parameterValues.containsKey(ARGUMENT_ID)) {
            id = parameterValues.get(ARGUMENT_ID).toString();
        }



        FastStringWriter actualUriBuf = new FastStringWriter();
        if(includeContextPath) {
            actualUriBuf.append(webRequest.getContextPath());
        }
        if(actionName != null) {

            if(actionName.indexOf(SLASH) > -1) {
                  actualUriBuf.append(actionName);
            }
            else {
                if(controllerName != null) {
                    appendUrlToken(actualUriBuf, controllerName, request);
                }
                else {
                    actualUriBuf.append(webRequest.getAttributes().getControllerUri(request));
                }
            }
            appendUrlToken(actualUriBuf, actionName, request);
        }
        if(id != null) {
            appendUrlToken(actualUriBuf, id, request);
        }
        appendRequestParams(actualUriBuf, parameterValues, request);
        return actualUriBuf.toString();
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding) {
        return createURLInternal(controller, action, parameterValues, true );
    }

    private String createURLInternal(String controller, String action, Map parameterValues, boolean includeContextPath) {
        if(parameterValues == null) parameterValues = new HashMap();
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        boolean blankController = StringUtils.isBlank(controller);
        boolean blankAction = StringUtils.isBlank(action);

        if(!blankController) {
            parameterValues.put( GrailsControllerClass.CONTROLLER, controller );
        }
        if(!blankAction)
            parameterValues.put( GrailsControllerClass.ACTION, action );

        try {
            return createURLWithWebRequest(parameterValues, webRequest, includeContextPath);
        }
        finally {
            if(!blankController)
                parameterValues.remove(GrailsControllerClass.CONTROLLER);
            if(!blankAction)
                parameterValues.remove(GrailsControllerClass.ACTION);
        }
    }

    public String createRelativeURL(String controller, String action, Map parameterValues, String encoding) {
         return createURLInternal(controller, action, parameterValues, false );
    }

    public String createRelativeURL(String controller, String action, Map parameterValues, String encoding, String fragment) {
        final String url = createURLInternal(controller, action, parameterValues, false);
        return  createUrlWithFragment(encoding, fragment, url);
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding, String fragment) {
        String url = createURL(controller, action, parameterValues, encoding);
        return createUrlWithFragment(encoding, fragment, url);
    }

    private String createUrlWithFragment(String encoding, String fragment, String url) {
        if(fragment != null) {
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

    /*
     * Appends all the request parameters to the URI buffer
     */
    private void appendRequestParams(FastStringWriter actualUriBuf, Map<Object,Object> params, HttpServletRequest request) {


        boolean querySeparator = false;

        for (Map.Entry<Object, Object> entry : params.entrySet()) {
        	Object name = entry.getKey();
        	if (name.equals(GrailsControllerClass.CONTROLLER) || name.equals(GrailsControllerClass.ACTION) || name.equals(ARGUMENT_ID))
                continue;

            if (!querySeparator) {
                actualUriBuf.append('?');
                querySeparator = true;
            }
            else {
                actualUriBuf.append(ENTITY_AMPERSAND);
            }
            Object value = entry.getValue();
            if (value instanceof Collection) {
                Collection values = (Collection) value;
                Iterator valueIterator = values.iterator();
                while (valueIterator.hasNext()) {
                    Object currentValue = valueIterator.next();
                    appendRequestParam(actualUriBuf, name, currentValue, request);
                    if (valueIterator.hasNext()) {
                        actualUriBuf.append(ENTITY_AMPERSAND);
                    }
                }
            }
            else if (value != null && value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                for (int j = 0; j < array.length; j++) {
                    Object currentValue = array[j];
                    appendRequestParam(actualUriBuf, name, currentValue, request);
                    if (j < (array.length-1)) {
                        actualUriBuf.append(ENTITY_AMPERSAND);
                    }
                }

            }
            else {
                appendRequestParam(actualUriBuf, name, value, request);
            }

        }
    }

    /*
     * Appends a request parameters for the given aname and value
     */
    private void appendRequestParam(FastStringWriter actualUriBuf, Object name, Object value, HttpServletRequest request) {
        if (value==null)
            value = "";

        actualUriBuf.append(urlEncode(name, request))
                 .append('=')
                 .append(urlEncode(value, request));
    }

    private String urlEncode(Object obj, ServletRequest request) {
        try {
            String charset = request.getCharacterEncoding();
            
            return URLEncoder.encode(obj.toString(), (charset != null) ? charset : GrailsWebUtil.DEFAULT_ENCODING );
        } catch (UnsupportedEncodingException ex) {
            throw new ControllerExecutionException("Error creating URL, cannot URLEncode to the client's character encoding: "+ ex.getMessage(),ex);
        }
    }

    /*
     * Appends a URL token to the buffer
     */
    private void appendUrlToken(FastStringWriter actualUriBuf, Object token, ServletRequest request) {
        actualUriBuf.append(SLASH).append(urlEncode(token, request));
    }
}
