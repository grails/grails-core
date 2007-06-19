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
package org.codehaus.groovy.grails.webflow.engine.builder;

import groovy.lang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.grails.commons.metaclass.ExpandoMetaClass;

import java.util.Map;

/**
 * Invokes a closure as a Webflow action placing the returned model within the flow scope
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jun 12, 2007
 *        Time: 12:00:23 PM
 */
public class ClosureInvokingAction extends AbstractAction implements GroovyObject {
    private Closure callable;
    private static final Log LOG = LogFactory.getLog(ClosureInvokingAction.class);
    private transient MetaClass metaClass;
    private static final String RESULT = "result";

    public ClosureInvokingAction(Closure callable) {
        this.callable = callable;
        this.metaClass = InvokerHelper.getMetaClass(this);
    }

    protected Event doExecute(RequestContext context) throws Exception {
        Closure cloned = (Closure)callable.clone();
        cloned.setDelegate(this);
        Object result = cloned.call(context);
        if(result instanceof Map) {
            context.getFlowScope().putAll(new LocalAttributeMap((Map)result));
            return super.success(result);
        }
        else if(result instanceof Event) {
           Event e = (Event)result;

            Map model = (Map) e.getAttributes().get(RESULT);
            if(model != null)
                context.getFlowScope().putAll(new LocalAttributeMap(model));
           return e;
        }
        else {
            return super.success(result);
        }
    }

    public Object invokeMethod(String name, Object args) {
        if(metaClass instanceof ExpandoMetaClass) {
            ExpandoMetaClass emc = (ExpandoMetaClass)metaClass;
            MetaMethod metaMethod = emc.getMetaMethod(name, args);
            if(metaMethod!=null) return metaMethod.invoke(this, (Object[]) args);
            else {
                return invokeMethodAsEvent(name, args);
            }
        }
        return invokeMethodAsEvent(name, args);
    }

    private Object invokeMethodAsEvent(String name, Object args) {
        Object[] argArray = (Object[])args;
        if(argArray.length == 0)
            return result(name);
        else if(argArray[0] instanceof Map) {
            return result(name,new LocalAttributeMap((Map)argArray[0]));
        }
        else {
            return result(name,name, args);
        }
    }

    public Object getProperty(String property) {
        return metaClass.getProperty(this, property);
    }

    public void setProperty(String property, Object newValue) {
         metaClass.setProperty(this, property, newValue);
    }

    public MetaClass getMetaClass() {
        return metaClass;
    }

    public void setMetaClass(MetaClass metaClass) {
        this.metaClass = metaClass;
    }
}
