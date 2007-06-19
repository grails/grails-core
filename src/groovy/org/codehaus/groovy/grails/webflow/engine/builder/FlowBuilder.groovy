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
import org.springframework.webflow.engine.State;
import org.springframework.webflow.engine.Transition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.AbstractFlowBuilder;
import org.springframework.webflow.engine.builder.FlowBuilderException;
import org.springframework.webflow.engine.support.TransitionExecutingStateExceptionHandler;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Event;
import org.codehaus.groovy.grails.commons.metaclass.ExpandoMetaClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>A builder implementation used to construct Spring Webflows. This is a DSL specifically
 *   designed to allow the construction of complex flows and is integrated into Grails'
 *   controller mechanism</p>
 *
 * <p>An example flow can be seen below:
 *
 * <pre><code>
            displaySearchForm {
                on("submit").to "executeSearch"
            }
            executeSearch {
                action {
                    [results:searchService.executeSearch(params.q)]
                }
                on("success").to "displayResults"
                on(Exception).to "displaySearchForm"
            }
            displayResults()
 * </code></pre>
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jun 8, 2007
 *        Time: 9:09:05 AM
 */
class  FlowBuilder extends AbstractFlowBuilder implements GroovyObject {
    static final DO_CALL_METHOD = "doCall"
    static final FLOW_METHOD = "flow"
    static final CLOSURE_METHODS = ['setDelegate', 'setMetaClass', 'getMetaClass', 'call', 'doCall']
    static final FLOW_INFO_METHODS =  ['on', 'action', 'subflow']

    final String flowId
    private MetaClass metaClass
    private Closure flowClosure
    private boolean flowDefiningMode = false
    private String startFlow;
    private boolean initialised = false

    public FlowBuilder(String flowId) {
        this.flowId = flowId;
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(FlowBuilder.class);
        super.init(flowId, null);
    }

    public invokeMethod(String name, args) {
        if(isRootFlowDefinition(name, args) && !initialised) {
            this.flowClosure = args[0];
            flowClosure.setDelegate(this);
            flowDefiningMode = true;
            buildStates();
            flowDefiningMode = false;
            initialised = true;
            return super.getFlow();
        }
        else if(flowDefiningMode) {
            if(isFirstArgumentClosure(args)) {
                FlowInfoCapturer flowInfo = new FlowInfoCapturer(this);

                Closure c = args[0];
                def builder = this
                // root methods invoked on the closure to the builder and not to enclosing scope to avoid confusing bugs
                MetaClass closureMetaClass = c.class.metaClass
                closureMetaClass.invokeMethod = {String methodName, methodArgs ->
                    if(CLOSURE_METHODS.contains(methodName)) {
                        def metaMethod = closureMetaClass.getMetaMethod(methodName, methodArgs)
                        return metaMethod.invoke(delegate, methodArgs)                                                
                    }
                    else if(FLOW_INFO_METHODS.contains(methodName)) {
                        return flowInfo.invokeMethod(methodName, methodArgs)
                    }
                    else {
                        return builder.invokeMethod(methodName, methodArgs)
                    }
                }
                c.metaClass = closureMetaClass
                c.setDelegate(flowInfo);
                c.call();

                Transition[] trans = flowInfo.getTransitions();

                Closure action = flowInfo.getAction();
                State state
                if(trans.length == 0 && flowInfo.getSubflow() == null) {
                    state = addEndState(name, name);
                }
                else if(action!=null) {
                   // add action state
                    state = addActionState(name, new ClosureInvokingAction(action),trans);
                }
                else if(flowInfo.getSubflow()!= null) {
                    Flow flow = new FlowBuilder(name).flow(flowInfo.getSubflow());
                    state = addSubflowState(name, flow,null, trans );
                }
                else {
                    state = addViewState(name, name, trans);
                }

                // add start state
                if(startFlow == null) {
                    startFlow = name;
                    getFlow().setStartState(startFlow);
                }

                return state;
            }
            else {
               return addEndState(name, name);
            }
        }
        else {
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
    }

    private Flow flow(Closure flow) {
        return invokeMethod(FLOW_METHOD, [flow] as Object[]);
    }

    private Object invokeMethodAsEvent(String name, Object args) {
        Object[] argArray = (Object[])args;
        if(argArray.length == 0)
            return new Event(getFlow(),name);
        else if(argArray[0] instanceof Map) {
            return new Event(getFlow(),name,new LocalAttributeMap((Map)argArray[0]));
        }
        else {
            return new Event(getFlow(),name);
        }
    }

    private boolean isFirstArgumentClosure(Object[] argArray) {
        return argArray.length > 0 && argArray[0] instanceof Closure;
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

    private boolean isRootFlowDefinition(String name, Object[] argArray) {
        return (name.equals(FLOW_METHOD) && argArray.length == 1 && argArray[0] instanceof Closure)
    }

    public void buildStates() throws FlowBuilderException {

        this.flowClosure.call();
    }
}
/**
 *  Used to capture details of the flow
 */
private class FlowInfoCapturer {
    private FlowBuilder builder;
    private List transitions = new ArrayList();
    private Closure action;
    private Closure subflow;

    public Transition[] getTransitions() {
        return (Transition[])transitions.toArray(new Transition[transitions.size()]);
    }
    public FlowInfoCapturer(FlowBuilder builder) {
        this.builder = builder;
    }

    public Closure getAction() {
        return action;
    }

    public Closure getSubflow() {
        return subflow;
    }
    public TransitionTo on(String name) {
        return new TransitionTo(name, builder, transitions);
    }
    public TransitionTo on(Class exception) {
        if(!Throwable.class.isAssignableFrom(exception)) {
            throw new FlowDefinitionException("Event handler in flow ["+getFlowId()+"] passed a class which is not a instance of java.lang.Throwable");
        }
        return new TransitionTo(exception, builder, transitions);
    }
    public void action(Closure callable) {
        this.action = callable;
    }
    public void subflow(Closure callable) {
        this.subflow = callable;
    }
}
class TransitionTo {
    private String on;
    private Class error;
    private builder
    List transitions


    public TransitionTo(String newOn, builder, newTransitions) {
        this.on = newOn;
        this.builder = builder
        this.transitions = newTransitions
    }

    public TransitionTo(Class exception, builder, newTransitions) {
        this.error = exception;
        this.builder = builder
        this.transitions = newTransitions
    }

    public Object to(String newTo) {
        if(error != null) {
            TransitionExecutingStateExceptionHandler handler = new TransitionExecutingStateExceptionHandler();
            handler.add(error, newTo);
            builder.getFlow().getExceptionHandlerSet().add(handler);
            return handler;
        }
        else {
            Transition t = builder.transition(builder.on(on), builder.to(newTo));
            transitions.add(t);
            return t;
        }
    }
}
