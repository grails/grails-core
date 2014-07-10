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
package org.grails.web.pages;

import grails.util.Environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import grails.core.GrailsApplication;
import grails.web.util.GrailsApplicationAttributes;
import org.grails.web.servlet.mvc.GrailsWebRequest;

/**
 * Script binding to be used as the top-level binding in GSP evaluation.
 *
 * @author Lari Hotari
 */
public class GroovyPageRequestBinding extends AbstractGroovyPageBinding {
    private static Log log = LogFactory.getLog(GroovyPageRequestBinding.class);
    private GrailsWebRequest webRequest;
    private Map<String, Class<?>> cachedDomainsWithoutPackage;
    private boolean developmentMode = Environment.isDevelopmentMode();
    private Set<String> requestAttributeVariables=new HashSet<String>();

    private static Map<String, LazyRequestBasedValue> lazyRequestBasedValuesMap = new HashMap<String, LazyRequestBasedValue>();
    static {
        Map<String, LazyRequestBasedValue> m = lazyRequestBasedValuesMap;
        m.put(GroovyPage.WEB_REQUEST, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest;
            }
        });
        m.put(GroovyPage.REQUEST, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getCurrentRequest();
            }
        });
        m.put(GroovyPage.RESPONSE, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getCurrentResponse();
            }
        });
        m.put(GroovyPage.FLASH, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getFlashScope();
            }
        });
        m.put(GroovyPage.SERVLET_CONTEXT, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getServletContext();
            }
        });
        m.put(GroovyPage.APPLICATION_CONTEXT, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getAttributes().getApplicationContext();
            }
        });
        m.put(GrailsApplication.APPLICATION_ID, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getAttributes().getGrailsApplication();
            }
        });
        m.put(GroovyPage.SESSION, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getSession();
            }
        });
        m.put(GroovyPage.PARAMS, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getParams();
            }
        });
        m.put(GroovyPage.ACTION_NAME, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getActionName();
            }
        });
        m.put(GroovyPage.CONTROLLER_NAME, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getControllerName();
            }
        });
        m.put(GrailsApplicationAttributes.CONTROLLER, new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getAttributes().getController(webRequest.getCurrentRequest());
            }
        });
    }

    public GroovyPageRequestBinding(GrailsWebRequest webRequest) {
        this.webRequest = webRequest;
        if (webRequest != null) {
            GroovyPagesTemplateEngine templateEngine=(GroovyPagesTemplateEngine)webRequest.getAttributes().getPagesTemplateEngine();
            if (templateEngine != null) {
                this.setCachedDomainsWithoutPackage(templateEngine.getDomainClassMap());
            }
        }
    }

    public boolean isRequestAttributeVariable(String name) {
        return requestAttributeVariables.contains(name);
    }

    @Override
    public Object getVariable(String name) {
        Object val = getVariablesMap().get(name);
        if (val == null && !getVariablesMap().containsKey(name) && webRequest != null) {
            val = webRequest.getCurrentRequest().getAttribute(name);
            if (val != null) {
                requestAttributeVariables.add(name);
            } else {
                LazyRequestBasedValue lazyValue = lazyRequestBasedValuesMap.get(name);
                if (lazyValue != null) {
                    val = lazyValue.evaluate(webRequest);
                }

                if (val == null && cachedDomainsWithoutPackage != null) {
                    val = cachedDomainsWithoutPackage.get(name);
                }

                // warn about missing variables in development mode
                if (val == null && developmentMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Variable '" + name + "' not found in binding or the value is null.");
                    }
                }
            }
        }
        return val;
    }

    public void setCachedDomainsWithoutPackage(Map<String, Class<?>> cachedDomainsWithoutPackage) {
        this.cachedDomainsWithoutPackage = cachedDomainsWithoutPackage;
    }

    private static interface LazyRequestBasedValue {
        public Object evaluate(GrailsWebRequest webRequest);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getVariableNames() {
        if (getVariablesMap().isEmpty()) {
            return lazyRequestBasedValuesMap.keySet();
        }

        Set<String> variableNames = new HashSet<String>(lazyRequestBasedValuesMap.keySet());
        variableNames.addAll(getVariablesMap().keySet());
        return variableNames;
    }
}
