/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.AntPathMatcher;

import java.beans.PropertyDescriptor;
import java.util.*;

/**
 * A class that evaluates the conventions contained within controllers to perform auto-configuration
 *
 * @author Graeme Rocher
 * @author Steven Devijver
 *
 * @since 0.1
 *
 * Created: Jul 2, 2005
 */
public class DefaultGrailsControllerClass extends AbstractInjectableGrailsClass
		implements GrailsControllerClass {

    private final static Log log = LogFactory.getLog(DefaultGrailsControllerClass.class);

    public static final String CONTROLLER = "Controller";

    private static final String SLASH = "/";
    private static final String VIEW = "View";
    private static final String DEFAULT_CLOSURE_PROPERTY = "defaultAction";
	private static final String ALLOWED_HTTP_METHODS_PROPERTY = "allowedMethods";

	private static final String EXCEPT = "except";
    private static final String ONLY = "only";
    private static final String FLOW_SUFFIX = "Flow";


    private static final String ACTION = "action";
    private Map uri2viewMap = null;
    private Map uri2closureMap = null;
    private Map viewNames = null;
    private String[] uris = null;
    private String uri;

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    private final Set commandObjectActions = new HashSet();
    private final Set commandObjectClasses = new HashSet();
    private Map flows = new HashMap();

    public void setDefaultActionName(String defaultActionName) {
        this.defaultActionName = defaultActionName;
        configureDefaultActionIfSet();
        configureURIsForCurrentState();        
    }

    private String defaultActionName;
    private String controllerPath;


    public DefaultGrailsControllerClass(Class clazz) {
        super(clazz, CONTROLLER);
        this.uri = SLASH + WordUtils.uncapitalize(getName());
        defaultActionName = (String)getPropertyOrStaticPropertyOrFieldValue(DEFAULT_CLOSURE_PROPERTY, String.class);
        if(defaultActionName == null) {
            defaultActionName = INDEX_ACTION;
        }
        Collection closureNames = new ArrayList();
        this.uri2viewMap = new HashMap();
        this.uri2closureMap = new HashMap();
        this.viewNames = new HashMap();

        this.controllerPath = uri + SLASH;


        PropertyDescriptor[] propertyDescriptors = getReference().getPropertyDescriptors();
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
            Closure closure = (Closure)getPropertyOrStaticPropertyOrFieldValue(propertyDescriptor.getName(), Closure.class);
            if (closure != null) {

                String closureName = propertyDescriptor.getName();
                if(closureName.endsWith(FLOW_SUFFIX)) {
                    String flowId = closureName.substring(0, closureName.length()-FLOW_SUFFIX.length());
                    flows.put(flowId, closure);
                    closureName = flowId;
                }
                else {
                    configureCommandObjectIfPresent(closure, closureName);
                }

                closureNames.add(closureName);

                configureMappingForClosureProperty(controllerPath, closureName);
            } else if (ALLOWED_HTTP_METHODS_PROPERTY.equals(propertyDescriptor.getName())) {
                if (!GrailsClassUtils.isStaticProperty(clazz, ALLOWED_HTTP_METHODS_PROPERTY)) {
                    log.error("The allowedMethods property in " + clazz.getName() + " should be declared static.  " +
                            "The non static version is supported for now but has been deprecated and may not work in " +
                            "future versions of Grails.");
                }
            }
        }

        if (!getReference().isReadableProperty(defaultActionName) && closureNames.size() == 1) {
            defaultActionName = ((String)closureNames.iterator().next());
        }
        configureDefaultActionIfSet();
        configureURIsForCurrentState();
    }

    private void configureURIsForCurrentState() {
        this.uris  = (String[])this.uri2closureMap.keySet().toArray(new String[this.uri2closureMap.keySet().size()]);
    }

    private void configureDefaultActionIfSet() {
        if(defaultActionName!=null) {
            this.uri2closureMap.put(uri, defaultActionName);
            this.uri2closureMap.put(controllerPath, defaultActionName);
            this.uri2viewMap.put(controllerPath, controllerPath + defaultActionName);
            this.uri2viewMap.put(uri, controllerPath +  defaultActionName);
            this.viewNames.put( defaultActionName, controllerPath + defaultActionName );
        }
    }

    private void configureCommandObjectIfPresent(Closure closure, String closureName) {
        Class[] parameterTypes = closure.getParameterTypes();
        if(parameterTypes != null && parameterTypes.length > 0) {
            for(int j = 0; j < parameterTypes.length; j++) {
                Class parameterType = parameterTypes[j];
                if(GroovyObject.class.isAssignableFrom(parameterType)) {
                    commandObjectActions.add(closureName);
                    commandObjectClasses.add(parameterType);
                }
            }
        }
    }

    private void configureMappingForClosureProperty(String controllerPath, String closureName) {
        String tmpUri = controllerPath + closureName;
        uri2closureMap.put(tmpUri,closureName);
        uri2closureMap.put(tmpUri + SLASH + "**",closureName);
        this.uri2viewMap.put(tmpUri, tmpUri);
        this.viewNames.put( closureName, tmpUri );
    }

    public String[] getURIs() {
		return this.uris;
	}

	public boolean mapsToURI(String uri) {
		for (int i = 0; i < uris.length; i++) {
			if (pathMatcher.match( uris[i], uri)) {
				return true;
			}
		}
		return false;
	}
	
	public String getViewByURI(String uri) {
		return (String)this.uri2viewMap.get(uri);
	}
	
	public String getClosurePropertyName(String uri) {
		return (String)this.uri2closureMap.get(uri);
	}

	public String getViewByName(String viewName) {
        if(this.viewNames.containsKey(viewName)) {
            return (String)this.viewNames.get(viewName);
        }
        else {
             return this.uri + SLASH + viewName;
        }
    }

	public boolean isInterceptedBefore(GroovyObject controller, String action) {
        return controller.getMetaClass().hasProperty(controller, BEFORE_INTERCEPTOR) != null && isIntercepted(controller.getProperty(BEFORE_INTERCEPTOR), action);
    }

	private boolean isIntercepted(Object bip, String action) {
		if(bip instanceof Map) {
			Map bipMap = (Map)bip;
			if(bipMap.containsKey(EXCEPT)) {
				Object excepts = bipMap.get(EXCEPT);
				if(excepts instanceof String) {
					if(!excepts.equals(action))
						return true;							
				}
				else if(excepts instanceof List) {
					if(!((List)excepts).contains(action))
						return true;
				}
			}
			else if(bipMap.containsKey(ONLY)) {
				Object onlys = bipMap.get(ONLY);
				if(onlys instanceof String) {
					if(onlys.equals(action))
						return true;
				}
				else if(onlys instanceof List) {
					if(((List)onlys).contains(action))
						return true;
				}
			}else{
                return true;
            }
		}
		else if(bip instanceof Closure) {
			return true;
		}
		return false;
	}

	public boolean isHttpMethodAllowedForAction(GroovyObject controller, String httpMethod, String actionName) {
		boolean isAllowed = true;
		Object methodRestrictionsProperty = null;
        if(controller.getMetaClass().hasProperty(controller, ALLOWED_HTTP_METHODS_PROPERTY) != null) {
            methodRestrictionsProperty = controller.getProperty(ALLOWED_HTTP_METHODS_PROPERTY);
        }
		if(methodRestrictionsProperty instanceof Map) {
			Map map = (Map)methodRestrictionsProperty;
			if(map.containsKey(actionName)) {
				Object value = map.get(actionName);
				if(value instanceof List) {
					List listOfMethods = (List) value;
					isAllowed = listOfMethods.contains(httpMethod);
				} else if(value instanceof String) {
					isAllowed = value.equals(httpMethod);
				}
			}
		}
		return isAllowed;
	}

	public boolean isInterceptedAfter(GroovyObject controller, String action) {
        return controller.getMetaClass().hasProperty(controller, AFTER_INTERCEPTOR) != null && isIntercepted(controller.getProperty(AFTER_INTERCEPTOR), action);
	}

	public Closure getBeforeInterceptor(GroovyObject controller) {
        if(getReference().isReadableProperty(BEFORE_INTERCEPTOR)) {
            return getInterceptor(controller.getProperty(BEFORE_INTERCEPTOR));
        }
        return null;
	}

	public Closure getAfterInterceptor(GroovyObject controller) {
        if(getReference().isReadableProperty(AFTER_INTERCEPTOR)) {
            return getInterceptor(controller.getProperty(AFTER_INTERCEPTOR));
        }
        return null;
    }

	private Closure getInterceptor(Object ip) {
		if(ip instanceof Map) {
			Map ipMap = (Map)ip;
			if(ipMap.containsKey(ACTION)) {
				return (Closure)ipMap.get(ACTION);
			}
		}
		else if(ip instanceof Closure) {
			return (Closure)ip;
		}
		return null;
	}

    public Set getCommandObjectActions() {
        return commandObjectActions;
    }

    public Set getCommandObjectClasses() {
        return Collections.unmodifiableSet(commandObjectClasses);
    }

    public Map getFlows() {
        return this.flows;
    }

    public boolean isFlowAction(String actionName) {
        return this.flows.containsKey(actionName);
    }

    public String getDefaultAction() {
        return this.defaultActionName;
    }

    public void registerMapping(String actionName) {
        configureMappingForClosureProperty(this.controllerPath, actionName);
        configureURIsForCurrentState();
    }
}
