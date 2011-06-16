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
package org.codehaus.groovy.grails.cli

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.GnuParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.commons.cli.Util

/**
 * Custom parser that handles optional arguments.
 *
 * @author Burt Beckwith
 * @since 1.4
 */
class GrailsCliParser extends GnuParser {

    @Override
    protected void processOption(String arg, ListIterator iter) throws ParseException {
        boolean hasOption = getOptions().hasOption(arg)

        // get the option represented by arg
        Option opt

        if (hasOption) {
            opt = getOptions().getOption(arg).clone()
        }
        else {
            String value
            if (arg.indexOf('=') != -1) {
                String[] parts = arg.split('=')
                arg = parts[0].trim()
                value = parts[1].trim()
            }
            opt = new OptionalOption(stripLeadingHyphens(arg))
            if (value) {
                opt.setArgs(1)
                opt.addValueForProcessing(Util.stripLeadingAndTrailingQuotes(value))
            }
            getOptions().addOption(opt)
        }

        // if the option is a required option remove the option from the requiredOptions list
        if (opt.isRequired()) {
            getRequiredOptions().remove(opt.getKey())
        }

        // if the option is in an OptionGroup make that option the selected option of the group
        if (getOptions().getOptionGroup(opt) != null) {
            OptionGroup group = getOptions().getOptionGroup(opt)

            if (group.isRequired()) {
                getRequiredOptions().remove(group)
            }

            group.setSelected(opt)
        }

        // if the option takes an argument value
        if (opt.hasArg()) {
            processArgs(opt, iter)
        }

        // set the option on the command line
        cmd.addOption(opt)
    }

    private String stripLeadingHyphens(String str) {
        if (str == null) {
            return null
        }

        if (str.startsWith("--")) {
            return str.substring(2)
        }

        if (str.startsWith("-")) {
            return str.substring(1)
        }

        return str
   }
}
