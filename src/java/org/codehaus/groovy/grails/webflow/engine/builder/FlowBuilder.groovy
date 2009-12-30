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
package org.codehaus.groovy.grails.webflow.engine.builder

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.metaclass.PropertyExpression
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.binding.expression.support.StaticExpression
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.util.Assert
import org.springframework.webflow.action.ExternalRedirectAction
import org.springframework.webflow.action.ViewFactoryActionAdapter
import org.springframework.webflow.core.collection.LocalAttributeMap
import org.springframework.webflow.definition.registry.FlowDefinitionLocator
import org.springframework.webflow.engine.*
import org.springframework.webflow.engine.builder.FlowArtifactFactory
import org.springframework.webflow.engine.builder.FlowBuilderContext
import org.springframework.webflow.engine.builder.FlowBuilderException
import org.springframework.webflow.engine.builder.support.AbstractFlowBuilder
import org.springframework.webflow.engine.builder.support.FlowBuilderContextImpl
import org.springframework.webflow.engine.builder.support.FlowBuilderServices
import org.springframework.webflow.engine.support.ActionTransitionCriteria
import org.springframework.webflow.engine.support.DefaultTargetStateResolver
import org.springframework.webflow.engine.support.DefaultTransitionCriteria
import org.springframework.webflow.engine.support.TransitionExecutingFlowExecutionExceptionHandler
import org.springframework.webflow.execution.Action
import org.springframework.webflow.execution.Event
import org.springframework.webflow.execution.ViewFactory
import org.springframework.beans.BeanUtils
import java.beans.PropertyDescriptor

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
     static final FLOW_INFO_METHODS =  ['on', 'action', 'subflow',"render","redirect", "onRender", "onEntry", "onExit"]

     final String flowId
     private MetaClass metaClass
     private Closure flowClosure
     private boolean flowDefiningMode = false
     private String startFlow;
     private boolean initialised = false

     ApplicationContext applicationContext
     String viewPath = "/"

     protected FlowBuilderServices flowBuilderServices

     public FlowBuilder(String flowId, FlowBuilderServices flowBuilderServices, FlowDefinitionLocator definitionLocator) {
         this.flowId = flowId;
         Assert.notNull flowBuilderServices, "Argument [flowBuilderServices] is required!"
         this.flowBuilderServices = flowBuilderServices
         this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(FlowBuilder.class);

         def context = new FlowBuilderContextImpl(flowId,null, definitionLocator, flowBuilderServices)
         super.init(context);
     }

     public FlowBuilder(String flowId, Closure newFlowClosure,FlowBuilderServices flowBuilderServices, FlowDefinitionLocator definitionLocator) {
        this(flowId, flowBuilderServices, definitionLocator)
        this.flowClosure = newFlowClosure
     }

     FlowBuilderContext getFlowBuilderContext() {
         super.getContext()
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
             flow.attributes.put("persistenceContext", "true")
             return flow;
         }
         else if(flowDefiningMode) {
             FlowArtifactFactory flowFactory = getContext().getFlowArtifactFactory()
             if(isFirstArgumentClosure(args)) {
                 if("onStart" == name) {
                    flow.startActionList.add(new ClosureInvokingAction(args[0]))
                 }
                 else if("onEnd" == name) {
                    flow.endActionList.add(new ClosureInvokingAction(args[0]))
                 }
                 else {
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
                     c.resolveStrategy = Closure.DELEGATE_FIRST
                     c.call()

                     Transition[] trans = flowInfo.transitions


                     Closure action = flowInfo.action
                     State state
                     if(flowInfo.redirectUrl) {
                         if(flowInfo.redirectUrl instanceof RuntimeRedirectAction) {
                             state = flowFactory.createEndState(name, getFlow(),null,flowInfo.redirectUrl,null,null,null)
                         }
                         else {
                             String url = flowInfo.redirectUrl
                             state = createRedirectEndState(name, url, flowFactory, flowInfo.entryAction)

                         }
                         state.attributes.put("commit", true)
                     }
                     else if(trans.length == 0 && flowInfo.subflow == null) {
                         String view = createViewPath(flowInfo, name)

                         state = createEndState(name,view, flowFactory, flowInfo.entryAction)
                         state.attributes.put("commit", true)
                     }
                     else if(action) {
                        // add action state
                         state = createActionState(name, action, trans, flowFactory, flowInfo.entryAction, flowInfo.exitAction)
                     }
                     else if(flowInfo.subflow) {
                         def i = flowId.indexOf('/')
                         def subflowId = i>-1 ? "${flowId[0..i-1]}/$name" : "${flowId}/$name"
                         FlowBuilder subFlowBuilder = new FlowBuilder(subflowId, flowBuilderServices, getContext().getFlowDefinitionLocator())
                         subFlowBuilder.viewPath = this.viewPath
                         Flow subflow = subFlowBuilder.flow(flowInfo.subflow)
                         state = createSubFlow(name, subflow, trans, flowFactory)
                     }
                     else {

                         String view = createViewPath(flowInfo, name)

                         state = createViewState(name, view,trans,flowFactory, flowInfo.renderAction, flowInfo.entryAction, flowInfo.exitAction);
                     }


                     for(eh in flowInfo.exceptionHandlers) {
                         state.getExceptionHandlerSet().add(eh)
                     }

                     // add start state
                     if(startFlow == null) {
                         startFlow = name;
                         getFlow().setStartState(startFlow);
                     }

                     return state;
                 }


             }
             else {
                String view = createViewPath([viewName:name], name)
                State state = createEndState(name, view, flowFactory)
                state.attributes.put("commit", true)
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

    private String createViewPath(flowInfo, String name) {
        String view
        if (flowInfo.viewName) {
            def path = flowInfo.viewName
            if (path.startsWith("/")) view = path
            else view = "$viewPath/$flowId/${path}"
        }
        else {
            view = "$viewPath/$flowId/${name}"
        }
        return view
    }

    private State createViewState(String stateId, String viewName, Transition[] transitions, FlowArtifactFactory flowFactory, Closure customRenderAction = null, Closure customEntryAction = null, Closure customExitAction=null) {
         def renderAction = new ClosureInvokingAction( {
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
         } )


        ViewFactory viewFactory = createViewFactory(viewName)

        List renderActions = [renderAction]
        if(customRenderAction) renderActions << new ClosureInvokingAction(customRenderAction)
        return flowFactory.createViewState( stateId,
                                            getFlow(),
                                            [] as ViewVariable[],
                                            getActionArrayOrNull(customEntryAction),
                                            viewFactory,
                                            null,
                                            false,
                                            renderActions as Action[],
                                            transitions,
                                            null,
                                            getActionArrayOrNull(customExitAction),
                                            null)

    }

    private State createSubFlow(String stateId, Flow subflow, Transition[] trans, FlowArtifactFactory flowFactory) {

        return flowFactory.createSubflowState(stateId,
                getFlow(),
                null,
                new StaticExpression(subflow),
                null,
                trans,
                null,
                null,
                null)
    }

    private State createActionState(String stateId, Closure action, Transition[] transitions, FlowArtifactFactory flowFactory,Closure customEntryAction = null, Closure customExitAction=null) {
        return flowFactory.createActionState(stateId,
                getFlow(),
                getActionArrayOrNull(customEntryAction),
                [new ClosureInvokingAction(action)] as Action[],
                transitions,
                null,
                getActionArrayOrNull(customExitAction),
                null)
    }

    protected State createRedirectEndState(String stateId, String url, FlowArtifactFactory flowFactory, Closure customEntryAction=null) {
        return flowFactory.createEndState(stateId, getFlow(), getActionArrayOrNull(customEntryAction), new ExternalRedirectAction(new StaticExpression(url)), null, null, null)
    }

    protected State createEndState( String stateId, String viewId, FlowArtifactFactory flowFactory, Closure customEntryAction=null) {
        ViewFactory viewFactory = createViewFactory(viewId)

        return flowFactory.createEndState(stateId,
                                            getFlow(),
                                            getActionArrayOrNull(customEntryAction),
                                            new ViewFactoryActionAdapter(viewFactory),
                                            null,
                                            null,
                                            null)
    }

    private Action[] getActionArrayOrNull(Closure customAction) {
        if (customAction) {
            return [new ClosureInvokingAction(customAction)] as Action[]
        }
    }

    protected ViewFactory createViewFactory(String viewId) {
        ViewFactory viewFactory = flowBuilderServices.getViewFactoryCreator().createViewFactory(
                new StaticExpression(viewId),
                flowBuilderServices.getExpressionParser(),
                flowBuilderServices.getConversionService(),
                null)
        return viewFactory
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
     List exceptionHandlers = []
     private Closure action
     private Closure renderAction
     private Closure entryAction
     private Closure exitAction
     private Closure subflow
     private String viewName
     private applicationContext
     private redirectUrl
     def propertyDescriptors = BeanUtils.getPropertyDescriptors(ExpressionDelegate)

     FlowInfoCapturer(FlowBuilder builder,ApplicationContext applicationContext) {
         this.builder = builder;
         this.applicationContext = applicationContext
     }

     Transition[] getTransitions() {
         return transitions.toArray(new Transition[transitions.size()])
     }

     String getViewName() { this.viewName }
     Closure getAction() { this.action }
     Closure getRenderAction() { this.renderAction}
     Closure getEntryAction() { this.entryAction}
     Closure getExitAction() { this.exitAction}
     Closure getSubflow() { this.subflow }
     def getRedirectUrl() { this.redirectUrl }

     TransitionTo on(String name) {
         return new TransitionTo(name, builder, transitions,exceptionHandlers);
     }
     TransitionTo on(String name, Closure transitionCriteria) {
         def transitionCriteriaAction = new ClosureInvokingAction(transitionCriteria)

         return new TransitionTo(name, builder, transitions,exceptionHandlers, transitionCriteriaAction)
     }
     TransitionTo on(Class exception) {
         if(!Throwable.class.isAssignableFrom(exception)) {
             throw new FlowDefinitionException("Event handler in flow ["+getFlowId()+"] passed a class which is not a instance of java.lang.Throwable");
         }
         return new TransitionTo(exception, builder, transitions,exceptionHandlers);
     }
     void onRender(Closure callable) {
         this.renderAction = callable
     }
     void onEntry(Closure callable) {
         this.entryAction = callable
     }
     void onExit(Closure callable) {
         this.exitAction = callable
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
                 def urlMapper =  applicationContext?.getBean(UrlMappingsHolder.BEAN_ID)
                 def params = args.params ?: [:]
                 if(args.id) params.id = args.id

                 redirectUrl = new RuntimeRedirectAction(controller:args.controller,
                                                             action:args.action,
                                                             params:params,
                                                             urlMapper:urlMapper)

             }
         }
     }

     def propertyMissing(String name) {

         if(propertyDescriptors.find { PropertyDescriptor pd -> pd.name == name}) {
             return new PropertyExpression(name)
         }
         else {
             throw new MissingPropertyException(name,FlowInfoCapturer)
         }
     }
 }

 class TransitionTo {
     private String on;
     private Class error;
     private FlowBuilder builder
     List transitions
     List exceptionHandlers
     private criteria


     public TransitionTo(String newOn, builder, newTransitions, newExceptionHandlers) {
         this.on = newOn;
         this.builder = builder
         this.transitions = newTransitions
         this.exceptionHandlers = newExceptionHandlers
     }

     public TransitionTo(String newOn, builder, newTransitions, newExceptionHandlers, org.codehaus.groovy.grails.webflow.engine.builder.ClosureInvokingAction newCriteria) {
         this.on = newOn;
         this.builder = builder
         this.transitions = newTransitions
         this.criteria = newCriteria
         this.exceptionHandlers = newExceptionHandlers
     }

     public TransitionTo(Class exception, builder, newTransitions, newExceptionHandlers) {
         this.error = exception;
         this.builder = builder
         this.transitions = newTransitions
         this.exceptionHandlers = newExceptionHandlers
     }

     public Object to(Closure resolver) {
         def closureResolver = new DefaultTargetStateResolver(new ClosureExpression(resolver))
         Transition t
         if(this.criteria) {
             t = transition(on, closureResolver,new ActionTransitionCriteria(this.criteria))
         }
         else {
             t = transition(on, closureResolver)
         }
         transitions.add(t);
         return t
     }
     public Object to(String newTo) {
         if(error != null) {
             TransitionExecutingFlowExecutionExceptionHandler handler = new TransitionExecutingFlowExecutionExceptionHandler();
             handler.add(error, newTo);
             exceptionHandlers << handler
             return handler;
         }
         else {
             Transition t
             if(this.criteria) {                 
                t = transition(on, newTo, new ActionTransitionCriteria(this.criteria))
             }
             else {
                t = transition(on, newTo)
             }
             transitions.add(t);
             return t;
         }
     }

     private Transition transition(String from, to) {
         return transition(from, to,null)
     }

     private Transition transition(String on, to, TransitionCriteria criteria) {
         def f = new DefaultTransitionCriteria(new StaticExpression(on))
         TargetStateResolver t = to instanceof TargetStateResolver ? to : new DefaultTargetStateResolver(to)
         Transition transition = new Transition(f, t)
         if(criteria)
            transition.executionCriteria = criteria
         return transition
     }
 }
