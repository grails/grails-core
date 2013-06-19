/*
 * Copyright 2013 the original author or authors.
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
package org.grails.databinding;

import groovy.util.slurpersupport.GPathResult;

import java.util.List;

import org.grails.databinding.events.DataBindingListener;

/**
 * @author Jeff Brown
 * @since 2.3
 */
public interface DataBinder {

    void bind(Object obj, DataBindingSource source, String filter, List<String> whiteList,
            List<String> blackList, DataBindingListener listener);

    public abstract void bind(Object obj, GPathResult gpath);

    public abstract void bind(Object obj, DataBindingSource source, List<String> whiteList,
            List<String> blackList);

    public abstract void bind(Object obj, DataBindingSource source, List<String> whiteList);

    public abstract void bind(Object obj, DataBindingSource source, DataBindingListener listener);

    public abstract void bind(Object obj, DataBindingSource source);

}
