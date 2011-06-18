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

import grails.util.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Command line parser that parses arguments to the command line. Written as a replacement for Commons CLI
 * because it doesn't support unknown arguments and requires all arguments to be declared up front.
 *
 *  It also doesn't support command options with hyphens. This class gets around those problems.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class CommandLineParser {

    static Map<String, String> ENV_ARGS = new HashMap<String, String>();
    static Map<String, String> DEFAULT_ENVS = new HashMap<String, String>();
    private static final String DEFAULT_PADDING = "        ";

    static {
        ENV_ARGS.put("dev", Environment.DEVELOPMENT.getName());
        ENV_ARGS.put("prod", Environment.PRODUCTION.getName());
        ENV_ARGS.put("test", Environment.TEST.getName());
        DEFAULT_ENVS.put("war", Environment.PRODUCTION.getName());
        DEFAULT_ENVS.put("test-app", Environment.TEST.getName());
    }

    private Map<String, Option> declaredOptions = new HashMap<String, Option> ();
    private int longestOptionNameLength = 0;
    private String usageMessage;

    /**
     * Adds a declared option
     *
     * @param name The name of the option
     * @param description The description
     */
    public void addOption(String name, String description) {
        int length = name.length();
        if(length >longestOptionNameLength) {
            longestOptionNameLength = length;
        }
        declaredOptions.put(name, new Option(name, description));
    }

    /**
     * Parses a string of all the command line options converting them into an array of arguments to pass to #parse(String..args)
     *
     * @param string The string
     * @return The command line
     */
    public CommandLine parseString(String string) {
        // stupid implementation right now that doesn't take into account quoted argument values
        String[] args = string.split(" ");
        return parse(args);
    }

    /**
     * Parses the given list of command line arguments. Arguments starting with -D become system properties,
     * arguments starting with -- or - become either declared or undeclared options. All other arguments are
     * put into a list of remaining arguments
     *
     * @param args The arguments
     * @return The command line state
     */
    public CommandLine parse(String... args) {
        DefaultCommandLine cl = createCommandLine();
        boolean first = true;
        for (String arg : args) {
            if(arg == null) continue;
            String trimmed = arg.trim();
            if(trimmed != null && trimmed.length()>0) {
                if(trimmed.charAt(0) == '-') {
                    processOption(cl, trimmed);
                }
                else {
                   if(ENV_ARGS.containsKey(trimmed)) {
                       cl.setEnvironment(ENV_ARGS.get(trimmed));
                   }
                   else {
                      if(first) {
                          cl.setCommandName(trimmed);
                          first = false;
                      }
                      else {
                          cl.addRemainingArg(trimmed);
                      }
                   }
                }
            }
        }

        return cl;
    }

    public String getHelpMessage() {
        String ls = System.getProperty("line.separator");
        usageMessage = "usage: grails [options] [command]";
        StringBuilder sb = new StringBuilder(usageMessage);
        sb.append(ls);
        for (Option option : declaredOptions.values()) {
            String name = option.getName();
            int extraPadding = longestOptionNameLength - name.length();
            sb.append(" -").append(name);
            for (int i = 0; i < extraPadding; i++) {
                sb.append(' ');
            }
            sb.append(DEFAULT_PADDING).append(option.getDescription()).append(ls);
        }


        return sb.toString();
    }

    protected DefaultCommandLine createCommandLine() {
        return new DefaultCommandLine();
    }

    protected void processOption(DefaultCommandLine cl, String arg) {
        if(arg.length()>1) {
            if(arg.charAt(1) == 'D' && arg.contains("=")) {
                processSystemArg(cl, arg);
            }
            else {
                arg = (arg.charAt(1) == '-' ? arg.substring(2, arg.length()) : arg.substring(1, arg.length())).trim();

                if(arg.contains("=")) {
                    String[] split = arg.split("=");
                    String name = split[0].trim();
                    valideOptionName(name);
                    String value = split[1].trim();
                    if(declaredOptions.containsKey(name)) {
                        cl.addDeclaredOption(name, declaredOptions.get(name), value);
                    }
                    else {
                        cl.addUndeclaredOption(name, value);
                    }
                }
                else {
                    valideOptionName(arg);
                    if(declaredOptions.containsKey(arg)) {
                        cl.addDeclaredOption(arg, declaredOptions.get(arg));
                    }
                    else {
                        cl.addUndeclaredOption(arg);
                    }
                }

            }
        }
    }

    private void valideOptionName(String name) {
        if(name.contains(" ")) throw new ParseException("Invalid argument: " + name);
    }

    protected void processSystemArg(DefaultCommandLine cl, String arg) {
        int i = arg.indexOf("=");
        String name = arg.substring(2, i);
        String value = arg.substring(i+1,arg.length());
        cl.addSystemProperty(name, value);
    }
}
