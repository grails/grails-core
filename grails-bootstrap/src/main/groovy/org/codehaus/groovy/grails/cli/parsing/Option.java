/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.cli.parsing;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a command line option.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class Option {

    private String name;
    private String description;

    public Option(String name, String description) {
        Assert.isTrue(StringUtils.hasLength(name), "Illegal option specified");
        this.name = name;
        this.description = description == null ? "" : description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
