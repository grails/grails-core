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
package org.codehaus.groovy.grails.web.pages;

import groovy.text.Template;
import groovy.lang.Writable;

import java.util.Map;

/**
 * An instance of the groovy.text.Template interface that knows how to
 * make in instance of GroovyPageWritable
 *
 * @author Graeme Rocher
 * @since 0.5
 *        <p/>
 *        Created: Feb 23, 2007
 *        Time: 11:36:26 AM
 */
public class GroovyPageTemplate implements Template {
    private GroovyPageMetaInfo metaInfo;

    public GroovyPageTemplate(GroovyPageMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public Writable make() {
        return new GroovyPageWritable(metaInfo);
    }

    public Writable make(Map binding) {
        GroovyPageWritable gptw = new GroovyPageWritable(metaInfo);
        gptw.setBinding(binding);
        return gptw;
    }
}