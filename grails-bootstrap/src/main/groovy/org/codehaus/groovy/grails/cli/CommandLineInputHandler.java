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

package org.codehaus.groovy.grails.cli;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.input.MultipleChoiceInputRequest;

import java.util.Vector;

/**
 * Custom input handler mechanism for Ant that ignores case of input
 *
 * @author Graeme Rocher
 * @since 1.4
 *
 */
public class CommandLineInputHandler implements InputHandler {
    private CommandLineHelper commandLineHelper;

    public CommandLineInputHandler() {
        this.commandLineHelper = new CommandLineHelper(System.out);
    }

    public CommandLineInputHandler(CommandLineHelper helper) {
        this.commandLineHelper = helper;
    }

    public void handleInput(InputRequest inputRequest) throws BuildException {
       String[] validInputs = null;
       if(inputRequest instanceof MultipleChoiceInputRequest) {
           Vector choices = ((MultipleChoiceInputRequest) inputRequest).getChoices();
           validInputs = (String[]) choices.toArray(new String[choices.size()]);
       }
        String result = commandLineHelper.userInput(inputRequest.getPrompt(), validInputs);
        inputRequest.setInput(result);
    }
}
