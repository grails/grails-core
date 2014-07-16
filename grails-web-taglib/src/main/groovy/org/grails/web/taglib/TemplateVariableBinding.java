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
package org.grails.web.taglib;

import groovy.lang.Binding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Script Binding that is used in GSP evaluation.
 *
 * @author Lari Hotari
 */
public class TemplateVariableBinding extends AbstractTemplateVariableBinding {
    private static final Log log = LogFactory.getLog(TemplateVariableBinding.class);

    private Binding parent;
    private Object owner;
    private Set<String> cachedParentVariableNames=new HashSet<String>();
    private WebRequestTemplateVariableBinding pageRequestBinding;
    private boolean pageRequestBindingInitialized=false;
    private boolean root;

    public TemplateVariableBinding() {
        super();
    }

    public TemplateVariableBinding(Binding parent) {
        setParent(parent);
    }

    @SuppressWarnings("rawtypes")
    public TemplateVariableBinding(Map variables) {
        super(variables);
    }

    public TemplateVariableBinding(String[] args) {
        super(args);
    }

    @Override
    public Object getProperty(String property) {
        return getVariable(property);
    }

    private WebRequestTemplateVariableBinding findPageRequestBinding() {
        if (!pageRequestBindingInitialized && parent != null) {
            Binding nextParent = parent;
            while(nextParent != null && pageRequestBinding==null) {
                if (nextParent instanceof WebRequestTemplateVariableBinding) {
                    pageRequestBinding = (WebRequestTemplateVariableBinding)nextParent;
                }
                if (nextParent instanceof TemplateVariableBinding) {
                    nextParent = ((TemplateVariableBinding)nextParent).parent;
                } else {
                    nextParent = null;
                }
            }
            pageRequestBindingInitialized=true;
        }
        return pageRequestBinding;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getVariable(String name) {
        Object val = getVariablesMap().get(name);
        if (val == null && !getVariablesMap().containsKey(name)) {
            if ("variables".equals(name)) return getVariables();
            if ("metaClass".equals(name)) return getMetaClass();
            if (parent != null) {
                val = parent.getVariable(name);
                if (val != null) {
                    if (findPageRequestBinding() == null || !findPageRequestBinding().isRequestAttributeVariable(name)) {
                        // cache variable in this context since parent context cannot change during usage of this context
                        getVariablesMap().put(name, val);
                        cachedParentVariableNames.add(name);
                    }
                }
            }
        }
        return val;
    }

    @Override
    public void setProperty(String property, Object newValue) {
        setVariable(property, newValue);
    }

    /**
     * ModifyOurScopeWithBodyTagTests breaks if variable isn't changed in the binding it exists in.
     *
     * @param name
     * @return The binding
     */
    private Binding findBindingForVariable(String name) {
        if (cachedParentVariableNames.contains(name)) {
            if (parent instanceof TemplateVariableBinding) {
                return ((TemplateVariableBinding)parent).findBindingForVariable(name);
            }
            return parent;
        }

        if (getVariablesMap().containsKey(name)) {
            return this;
        }

        if (parent instanceof TemplateVariableBinding) {
            return ((TemplateVariableBinding)parent).findBindingForVariable(name);
        }

        if (parent != null && parent.getVariables().containsKey(name)) {
            return parent;
        }

        return null;
    }

    @Override
    public void setVariable(String name, Object value) {
        internalSetVariable(null, name, value);
    }

    @SuppressWarnings("unchecked")
    private void internalSetVariable(Binding bindingToUse, String name, Object value) {
        if (!isReservedName(name)) {
            if (bindingToUse == null) {
                bindingToUse = findBindingForVariable(name);
                if (bindingToUse == null || (bindingToUse instanceof TemplateVariableBinding && ((TemplateVariableBinding)bindingToUse).shouldUseChildBinding(this))) {
                    bindingToUse = this;
                }
            }
            if (bindingToUse instanceof AbstractTemplateVariableBinding) {
                ((AbstractTemplateVariableBinding)bindingToUse).getVariablesMap().put(name, value);
            } else {
                bindingToUse.getVariables().put(name, value);
            }

            if (bindingToUse != this && cachedParentVariableNames.contains(name)) {
                // maintain cached value
                getVariablesMap().put(name, value);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Cannot override reserved variable '" + name + "'");
            }
        }
    }

    protected boolean isReservedName(String name) {
        return false;
    }

    protected boolean shouldUseChildBinding(TemplateVariableBinding childBinding) {
        return isRoot();
    }

    public Binding getParent() {
        return parent;
    }

    public void setParent(Binding parent) {
        this.parent = parent;
    }

    protected void internalSetVariable(String name, Object value) {
        internalSetVariable(this, name, value);
    }

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getVariableNames() {
        Set<String> variableNames=new HashSet<String>();
        if (parent != null) {
            if (parent instanceof AbstractTemplateVariableBinding) {
                variableNames.addAll(((AbstractTemplateVariableBinding)parent).getVariableNames());
            } else {
                variableNames.addAll(parent.getVariables().keySet());
            }
        }
        variableNames.addAll(getVariablesMap().keySet());
        return variableNames;
    }
}
