/*
 * Copyright 2010 the original author or authors.
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
package org.codehaus.groovy.grails.cli;

/**
 * Exception thrown when Grails is asked to execute a script that it can't find.
 */
public class ScriptNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 2794605786839371674L;
    private String scriptName;

    public ScriptNotFoundException(String scriptName) {
        this.scriptName = scriptName;
    }

    public ScriptNotFoundException(String scriptName, String message, Throwable cause) {
        super(message, cause);
        this.scriptName = scriptName;
    }

    public ScriptNotFoundException(String scriptName, Throwable cause) {
        super(cause);
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }
}
