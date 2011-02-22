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
package org.codehaus.groovy.grails.web.pages.ext.jsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;

import javax.el.*;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.el.ImplicitObjectELResolver;
import javax.servlet.jsp.el.ScopedAttributeELResolver;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: May 1, 2008
 */
public class GroovyPagesJspApplicationContext implements JspApplicationContext{

    private static final Log LOG = LogFactory.getLog(GroovyPagesJspApplicationContext.class);

    private static final ExpressionFactory expressionFactoryImpl = findExpressionFactoryImplementation();

    private final LinkedList listeners = new LinkedList();
    private final CompositeELResolver elResolver = new CompositeELResolver();
    private final CompositeELResolver additionalResolvers = new CompositeELResolver();
    {
        elResolver.add(new ImplicitObjectELResolver());
        elResolver.add(additionalResolvers);
        elResolver.add(new MapELResolver());
        elResolver.add(new ResourceBundleELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new ArrayELResolver());
        elResolver.add(new BeanELResolver());
        elResolver.add(new ScopedAttributeELResolver());
    }

    private static ExpressionFactory findExpressionFactoryImplementation() {
        ExpressionFactory ef = tryExpressionFactoryImplementation("com.sun");
        if(ef == null) {
            ef = tryExpressionFactoryImplementation("org.apache");
            if(ef == null) {
                LOG.warn("Could not find any implementation for " +
                        ExpressionFactory.class.getName());
            }
        }
        return ef;
    }

    private static ExpressionFactory tryExpressionFactoryImplementation(String packagePrefix) {
        String className = packagePrefix + ".el.ExpressionFactoryImpl";
        try {
            Class cl = ClassUtils.forName(className);
            if(ExpressionFactory.class.isAssignableFrom(cl)) {
                LOG.info("Using " + className + " as implementation of " +
                        ExpressionFactory.class.getName());
                return (ExpressionFactory)cl.newInstance();
            }
            LOG.warn("Class " + className + " does not implement " +
                    ExpressionFactory.class.getName());
        }
        catch(ClassNotFoundException e) {
        }
        catch(Exception e) {
            LOG.error("Failed to instantiate " + className, e);
        }
        return null;
    }    
    
    public void addELResolver(ELResolver elResolver) {
        additionalResolvers.add(elResolver);
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactoryImpl;
    }

    public void addELContextListener(ELContextListener elContextListener) {
        synchronized(listeners) {
            listeners.addLast(elContextListener);
        }
    }

    ELContext createELContext(GroovyPagesPageContext pageCtx) {
        ELContext ctx = new GroovyPagesELContext(pageCtx);
        ELContextEvent event = new ELContextEvent(ctx);
        synchronized(listeners) {
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                ELContextListener l = (ELContextListener) iter.next();
                l.contextCreated(event);
            }
        }
        return ctx;
    }

    private class GroovyPagesELContext extends ELContext {
        private GroovyPagesPageContext pageCtx;

        public GroovyPagesELContext(GroovyPagesPageContext pageCtx) {
            this.pageCtx = pageCtx;
        }

        public ELResolver getELResolver() {
            return elResolver;
        }

        public FunctionMapper getFunctionMapper() {
            return null;
        }

        public VariableMapper getVariableMapper() {
            return new VariableMapper() {

                public ValueExpression resolveVariable(String name) {
                    Object o = pageCtx.findAttribute(name);
                    if(o == null)return null;
                    else {
                        return expressionFactoryImpl.createValueExpression(o, o.getClass());
                    }
                }

                public ValueExpression setVariable(String name, ValueExpression valueExpression) {
                    ValueExpression previous = resolveVariable(name);
                    pageCtx.setAttribute(name, valueExpression.getValue(GroovyPagesELContext.this));
                    return previous;
                }
            };
        }
    }
}
