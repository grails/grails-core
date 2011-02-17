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
package org.codehaus.groovy.grails.web.util;

import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;

/**
 * Simple implementation of the HttpListener interface that writes to standard out
 */
public class DebugHttpSessionListener implements HttpSessionListener {

    public void sessionCreated(HttpSessionEvent event) {
        System.out.println("session created = " + event.getSession());
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        System.out.println("session destroyed = " + event.getSession());
    }
}
