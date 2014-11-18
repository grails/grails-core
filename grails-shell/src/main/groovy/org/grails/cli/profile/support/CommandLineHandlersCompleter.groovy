/*
 * Copyright 2014 the original author or authors.
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
package org.grails.cli.profile.support

import groovy.transform.CompileStatic
import jline.console.completer.Completer

import org.grails.cli.interactive.completors.StringsCompleter
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.DefaultProfile
import org.grails.cli.profile.ProjectContext


@CompileStatic
class CommandLineHandlersCompleter implements Completer {
    ProjectContext context
    DefaultProfile defaultProfile

    CommandLineHandlersCompleter(ProjectContext context, DefaultProfile defaultProfile) {
        this.context = context
        this.defaultProfile = defaultProfile
    }

    /**
     * Perform a completion operation across all aggregated completers.
     *
     * @see Completer#complete(String, int, java.util.List)
     * @return the highest completion return value from all completers
     */
    public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
        List<Completion> completions = new ArrayList<Completion>()
        int max = -1
        
        def completerHandler = { Completer completer ->
            Completion completion = new Completion(candidates)
            completion.complete(completer, buffer, cursor)
            max = Math.max(max, completion.cursor)
            completions.add(completion)
        }
        
        defaultProfile.getCommandLineHandlers(context).each { CommandLineHandler commandLineHandler ->
            def allCommands = commandLineHandler.listCommands(context)
            if (allCommands) {
                Completer completer = new StringsCompleter(allCommands*.name)
                completerHandler(completer)
            }
            if(commandLineHandler instanceof Completer) {
                completerHandler((Completer)commandLineHandler)
            }
        }

        Set<CharSequence> newCompletions = new TreeSet<CharSequence>()
        for (Completion completion : completions) {
            if (completion.cursor == max) {
                newCompletions.addAll(completion.candidates)
            }
        }
        candidates.addAll(newCompletions)

        return max
    }

    private static class Completion
    {
        final List<CharSequence> candidates
        int cursor

        public Completion(final List<CharSequence> candidates) {
            this.candidates = new ArrayList<CharSequence>(candidates);
        }

        public void complete(final Completer completer, final String buffer, final int cursor) {
            this.cursor = completer.complete(buffer, cursor, candidates);
        }
    }
}
