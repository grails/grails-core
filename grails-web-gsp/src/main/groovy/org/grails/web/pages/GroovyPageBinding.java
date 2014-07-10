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
package org.grails.web.pages;

import groovy.lang.Binding;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import grails.plugins.GrailsPlugin;

/**
 * Script Binding that is used in GSP evaluation.
 *
 * @author Lari Hotari
 */
public class GroovyPageBinding extends AbstractGroovyPageBinding {
    private static final Log log = LogFactory.getLog(GroovyPageBinding.class);

    private Binding parent;
    private GroovyPage owner;
    private Set<String> cachedParentVariableNames=new HashSet<String>();
    private GroovyPageRequestBinding pageRequestBinding;
    private boolean pageRequestBindingInitialized=false;
    private boolean root;

    public GroovyPageBinding() {
        super();
    }

    public GroovyPageBinding(Binding parent) {
        setParent(parent);
    }

    public GroovyPageBinding(String pluginContextPath) {
        setPluginContextPath(pluginContextPath);
    }

    @SuppressWarnings("rawtypes")
    public GroovyPageBinding(Map variables) {
        super(variables);
    }

    public GroovyPageBinding(String[] args) {
        super(args);
    }

    @Override
    public Object getProperty(String property) {
        return getVariable(property);
    }

    private GroovyPageRequestBinding findPageRequestBinding() {
        if (!pageRequestBindingInitialized && parent != null) {
            Binding nextParent = parent;
            while(nextParent != null && pageRequestBinding==null) {
                if (nextParent instanceof GroovyPageRequestBinding) {
                    pageRequestBinding = (GroovyPageRequestBinding)nextParent;
                }
                if (nextParent instanceof GroovyPageBinding) {
                    nextParent = ((GroovyPageBinding)nextParent).parent;
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
            if (GroovyPage.PAGE_SCOPE.equals(name)) return this;
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
            if (parent instanceof GroovyPageBinding) {
                return ((GroovyPageBinding)parent).findBindingForVariable(name);
            }
            return parent;
        }

        if (getVariablesMap().containsKey(name)) {
            return this;
        }

        if (parent instanceof GroovyPageBinding) {
            return ((GroovyPageBinding)parent).findBindingForVariable(name);
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
        if (!GroovyPage.isReservedName(name)) {
            if (bindingToUse == null) {
                bindingToUse = findBindingForVariable(name);
                if (bindingToUse == null || (bindingToUse instanceof GroovyPageBinding && ((GroovyPageBinding)bindingToUse).shouldUseChildBinding(this))) {
                    bindingToUse = this;
                }
            }
            if (bindingToUse instanceof AbstractGroovyPageBinding) {
                ((AbstractGroovyPageBinding)bindingToUse).getVariablesMap().put(name, value);
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

    private boolean shouldUseChildBinding(GroovyPageBinding childBinding) {
        return isRoot() || hasSameOwnerClass(childBinding);
    }

    private boolean hasSameOwnerClass(GroovyPageBinding otherBinding) {
        // owner class can be same in recursive rendering; in that case, the child binding should be used for setting variable values
        return (getOwner() != null && otherBinding.getOwner() != null && getOwner().getClass()==otherBinding.getOwner().getClass());
    }

    public String getPluginContextPath() {
        return (String)getVariable(GroovyPage.PLUGIN_CONTEXT_PATH);
    }

    @SuppressWarnings("unchecked")
    public void setPluginContextPath(String pluginContextPath) {
        getVariablesMap().put(GroovyPage.PLUGIN_CONTEXT_PATH, pluginContextPath);
    }

    @SuppressWarnings("unchecked")
    public void setPagePlugin(GrailsPlugin plugin) {
        getVariablesMap().put("pagePlugin", plugin);
    }

    public GrailsPlugin getPagePlugin() {
        return (GrailsPlugin)getVariable("pagePlugin");
    }

    public Binding getParent() {
        return parent;
    }

    public void setParent(Binding parent) {
        this.parent = parent;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addMap(Map additionalBinding) {
        for (Iterator<Map.Entry> i = additionalBinding.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = i.next();
            String name = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            internalSetVariable(this, name, value);
        }
    }

    public GroovyPage getOwner() {
        return owner;
    }

    public void setOwner(GroovyPage owner) {
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
            if (parent instanceof AbstractGroovyPageBinding) {
                variableNames.addAll(((AbstractGroovyPageBinding)parent).getVariableNames());
            } else {
                variableNames.addAll(parent.getVariables().keySet());
            }
        }
        variableNames.addAll(getVariablesMap().keySet());
        return variableNames;
    }
}
