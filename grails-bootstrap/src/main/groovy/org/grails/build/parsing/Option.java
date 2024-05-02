/*
 * Copyright 2024 original authors
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
package org.grails.build.parsing;

/**
 * Represents a command line option.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class Option {

    private String name;
    private String description;

    public Option(String name, String description) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("illegal option specified");
        }

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
