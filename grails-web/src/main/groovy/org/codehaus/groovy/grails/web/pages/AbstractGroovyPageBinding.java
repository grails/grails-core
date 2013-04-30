/*
 * Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Binding;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.AbstractMapEntry;
import org.apache.commons.collections.set.UnmodifiableSet;

@SuppressWarnings("rawtypes")
public abstract class AbstractGroovyPageBinding extends Binding {
    public AbstractGroovyPageBinding() {
        super();
    }

    public AbstractGroovyPageBinding(Map variables) {
        super(variables);
    }

    public AbstractGroovyPageBinding(String[] args) {
        super(args);
    }

    public Map getVariablesMap() {
        return super.getVariables();
    }

    @SuppressWarnings("unchecked")
    public void setVariableDirectly(String name, Object value) {
        getVariablesMap().put(name, value);
    }

    public abstract Set<String> getVariableNames();

    @Override
    public Map getVariables() {
        return new GroovyPageBindingMap(this);
    }

    protected static final class GroovyPageBindingMap implements Map {
        AbstractGroovyPageBinding binding;

        public GroovyPageBindingMap(AbstractGroovyPageBinding binding) {
            this.binding=binding;
        }

        public int size() {
            return binding.getVariableNames().size();
        }

        public boolean isEmpty() {
            return binding.getVariableNames().isEmpty();
        }

        public boolean containsKey(Object key) {
            return binding.getVariableNames().contains(key);
        }

        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        public Object get(Object key) {
            return binding.getVariable(String.valueOf(key));
        }

        public Object put(Object key, Object value) {
            binding.setVariable(String.valueOf(key), value);
            return null;
        }

        public Object remove(Object key) {
            binding.setVariable(String.valueOf(key), null);
            return null;
        }

        public void putAll(Map m) {
            for (Object entryObj : m.entrySet()) {
                Map.Entry entry=(Map.Entry)entryObj;
                binding.setVariable(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        public void clear() {
            throw new UnsupportedOperationException("clear() not supported");
        }

        public Set keySet() {
            return binding.getVariableNames();
        }

        @SuppressWarnings("unchecked")
        public Collection values() {
            Set<String> variableNames = binding.getVariableNames();
            Collection values = new ArrayList(variableNames.size());
            for (String variable : variableNames) {
                values.add(binding.getVariable(variable));
            }
            return values;
        }

        public Set entrySet() {
            return UnmodifiableSet.decorate(new AbstractSet() {
                @Override
                public Iterator iterator() {
                    return entryIterator();
                }
                @Override
                public int size() {
                    return binding.getVariableNames().size();
                }
            });
        }

        private Iterator entryIterator() {
            final Iterator iter = keySet().iterator();
            return new Iterator() {
                public boolean hasNext() {
                    return iter.hasNext();
                }
                public Object next() {
                    Object key = iter.next();
                    Object value = get(key);
                    return new BindingMapEntry(binding, key, value);
                }
                public void remove() {
                    throw new UnsupportedOperationException("remove() not supported");
                }
            };
        }
    }

    protected static class BindingMapEntry extends AbstractMapEntry {
        private AbstractGroovyPageBinding binding;

        protected BindingMapEntry(AbstractGroovyPageBinding binding, Object key, Object value) {
            super(key, value);
            this.binding = binding;
        }

        @Override
        public Object setValue(Object value) {
            String key = String.valueOf(getKey());
            Object oldValue = binding.getVariable(key);
            binding.setVariable(key, value);
            super.setValue(value);
            return oldValue;
        }
    }
}
