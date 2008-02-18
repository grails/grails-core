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
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Event;
import org.apache.commons.logging.LogFactory

import java.util.ArrayList;
import java.util.List;
import java.util.Map
import org.springframework.webflow.engine.support.ActionTransitionCriteria
import org.springframework.webflow.execution.Action
import org.springframework.webflow.execution.RequestContext;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.springframework.webflow.engine.support.TransitionExecutingFlowExecutionExceptionHandler

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
class FlowBuilder extends AbstractFlowBuilder implements GroovyObject, ApplicationContextAware {
    static final LOG = LogFactory.getLog(FlowBuilder)
    static final DO_CALL_METHOD = "doCall"
    static final FLOW_METHOD = "flow"
    static final CLOSURE_METHODS = ['setDelegate', 'setMetaClass', 'getMetaClass', 'call', 'doCall']
    static final FLOW_INFO_METHODS =  ['on', 'action', 'subflow',"render","redirect"]

    final String flowId
    private MetaClass metaClass
    private Closure flowClosure
    private boolean flowDefiningMode = false
    private String startFlow;
    private boolean initialised = false

    ApplicationContext applicationContext

    public FlowBuilder(String flowId) {
        this.flowId = flowId;
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(FlowBuilder.class);
        super.init(flowId, null);
    }

    public FlowBuilder(String flowId, Closure newFlowClosure) {
       this(flowId)
       this.flowClosure = newFlowClosure
    }

    public invokeMethod(String name, args) {
        if(isRootFlowDefinition(name, args) && !initialised) {
            this.flowClosure = args[0];
            flowClosure.setDelegate(this);
            flowDefiningMode = true;
            this.flowClosure.delegate = this
            this.flowClosure.call();
            flowDefiningMode = false;
            initialised = true;
            Flow flow = super.getFlow()
            flow.attributeMap.put("persistenceContext", true)
            return flow;
        }
        else if(flowDefiningMode) {
            if(isFirstArgumentClosure(args)) {
                FlowInfoCapturer flowInfo = new FlowInfoCapturer(this, applicationContext)

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
                c.delegate = flowInfo
                c.call()

                Transition[] trans = flowInfo.transitions

                Closure action = flowInfo.action
                State state
                if(flowInfo.redirectUrl) {
                    state = addEndState(name, flowInfo.redirectUrl );
                    state.attributeMap.put("commit", true)
                }
                else if(trans.length == 0 && flowInfo.subflow == null) {

                    state = addEndState(name, flowInfo.viewName ?: name);
                    state.attributeMap.put("commit", true)
                }
                else if(action) {
                   // add action state
                    state = addActionState(name, new ClosureInvokingAction(action),trans);
                }
                else if(flowInfo.subflow) {
                    Flow flow = new FlowBuilder(name).flow(flowInfo.subflow)
                    state = addSubflowState(name, flow,null, trans );
                }
                else {
                    def renderAction = new ClosureInvokingAction() { 
                        for(entry in flash.asMap()) {
                            def key = entry.key
                             if(key.startsWith(GrailsApplicationAttributes.ERRORS)) {
                                  key = key.substring(GrailsApplicationAttributes.ERRORS.length()+1)
                                  def formObject = flow[key]
                                  if(formObject) {
                                      try {
                                          formObject.errors = entry.value
                                      } catch (MissingPropertyException mpe) {
                                            // ignore
                                      }
                                  }
                                  else {
                                      formObject = conversation[key]
                                      if(formObject) {
                                          try {
                                              formObject.errors = entry.value
                                          } catch (MissingPropertyException mpe) {
                                                // ignore
                                          }
                                      }
                                  }
                             }
                        }
                    }

                    if(flowInfo.viewName) {
                        state = addViewState(name, flowInfo.viewName,(Action) renderAction,trans);
                    }
                    else
                        state = addViewState(name, name,(Action) renderAction,trans);
                }

                // add start state
                if(startFlow == null) {
                    startFlow = name;
                    getFlow().setStartState(startFlow);
                }

                return state;
            }
            else {
               State state = addEndState(name, name)
               state.attributeMap.put("commit", true)
               return state;
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
            return new Event(name,name);
        else if(argArray[0] instanceof Map) {
            return new Event(name,name,new LocalAttributeMap((Map)argArray[0]));
        }
        else {
            return new Event(name,name);
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
         flow(this.flowClosure)
    }
}
/**
 *  Used to capture details of the flow
 */
class FlowInfoCapturer {
    private FlowBuilder builder
    private List transitions = []
    private Closure action
    private Closure subflow
    private String viewName
    private applicationContext
    private redirectUrl

    FlowInfoCapturer(FlowBuilder builder,ApplicationContext applicationContext) {
        this.builder = builder;
        this.applicationContext = applicationContext
    }

    Transition[] getTransitions() {
        return transitions.toArray(new Transition[transitions.size()])
    }

    String getViewName() { this.viewName }
    Closure getAction() { this.action }
    Closure getSubflow() { this.subflow }
    String getRedirectUrl() { this.redirectUrl }

    TransitionTo on(String name) {
        return new TransitionTo(name, builder, transitions);
    }
    TransitionTo on(String name, Closure transitionCriteria) {
        def transitionCriteriaAction = new ClosureInvokingAction(transitionCriteria)

        return new TransitionTo(name, builder, transitions, transitionCriteriaAction)
    }
    TransitionTo on(Class exception) {
        if(!Throwable.class.isAssignableFrom(exception)) {
            throw new FlowDefinitionException("Event handler in flow ["+getFlowId()+"] passed a class which is not a instance of java.lang.Throwable");
        }
        return new TransitionTo(exception, builder, transitions);
    }
    void action(Closure callable) {
        this.action = callable;
    }
    void subflow(Closure callable) {
        this.subflow = callable;
    }
    void render(Map args) {
        if(args.view) {
            this.viewName = args.view
        }
    }
    void redirect(Map args) {
        if(args.url) redirectUrl = "externalRedirect:${args.url}"
        else if(args.uri) redirectUrl = "externalRedirect:${args.uri}"
        else {
            if(args.controller) {
                def controller = args.controller
                def params = args.params ? args.params : [:]
                def urlMapper =  applicationContext?.getBean(UrlMappingsHolder.BEAN_ID)
                if(!urlMapper) throw new IllegalStateException("Cannot redirect without an instance of [org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder] within the ApplicationContext")
                if(args.id) params.id = args.id

                UrlCreator urlCreator = urlMapper.getReverseMapping( controller, args.action, params );
                def url = urlCreator.createRelativeURL(controller, args.action, params, 'utf-8')
                redirectUrl = "externalRedirect:$url"                
            }
        }   
    }
}
class TransitionTo {
    private String on;
    private Class error;
    private builder
    List transitions
    private criteria


    public TransitionTo(String newOn, builder, newTransitions) {
        this.on = newOn;
        this.builder = builder
        this.transitions = newTransitions
    }

    public TransitionTo(String newOn, builder, newTransitions, org.codehaus.groovy.grails.webflow.engine.builder.ClosureInvokingAction newCriteria) {
        this.on = newOn;
        this.builder = builder
        this.transitions = newTransitions
        this.criteria = newCriteria
    }

    public TransitionTo(Class exception, builder, newTransitions) {
        this.error = exception;
        this.builder = builder
        this.transitions = newTransitions
    }

    public Object to(Closure resolver) {
        def closureResolver = new org.springframework.webflow.engine.support.DefaultTargetStateResolver(new ClosureExpression(resolver))
        Transition t
        if(this.criteria) {
             t = builder.transition(builder.on(on), closureResolver, new ActionTransitionCriteria(this.criteria));
        }
        else {
            t = builder.transition(builder.on(on), closureResolver);
        }
        transitions.add(t);
        return t
    }
    public Object to(String newTo) {
        if(error != null) {
            TransitionExecutingFlowExecutionExceptionHandler handler = new TransitionExecutingFlowExecutionExceptionHandler();
            handler.add(error, newTo);
            builder.getFlow().getExceptionHandlerSet().add(handler);
            return handler;
        }
        else {
            Transition t
            if(this.criteria) {
               t = builder.transition(builder.on(on), builder.to(newTo), new ActionTransitionCriteria(this.criteria));                 
            }
            else {
               t = builder.transition(builder.on(on), builder.to(newTo));
            }
            transitions.add(t);
            return t;
        }
    }
}
