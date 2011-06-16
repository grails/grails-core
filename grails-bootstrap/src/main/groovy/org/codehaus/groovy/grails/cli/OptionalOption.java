/*
 * Copyright 2011 SpringSource.
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

import org.apache.commons.cli.Option;

/**
 * Represents an optional commandline option that wasn't specified beforehand.
 *
 * @author Burt Beckwith
 * @since 1.4
 */
@SuppressWarnings("serial")
public class OptionalOption extends Option {

    private static final String HYPHEN = "__HYPHEN__";

    public OptionalOption(String opt) throws IllegalArgumentException {
        super(opt.replaceAll("\\-", HYPHEN), "");
    }

    @Override
    public String getOpt() {
        return super.getOpt().replaceAll(HYPHEN, "-");
    }

    @Override
    public String toString() {
        if (getArgs() == 1) {
            return getOpt() + '=' + getValue();
        }
        return getOpt();
    }
}
