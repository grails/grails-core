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
import grails.build.logging.GrailsConsole;

import java.util.Vector;

/**
 * Custom input handler mechanism for Ant that ignores case of input.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class CommandLineInputHandler implements InputHandler {

    public CommandLineInputHandler() {
    }

    public void handleInput(InputRequest inputRequest) throws BuildException {
       String[] validInputs = null;
       if (inputRequest instanceof MultipleChoiceInputRequest) {
           @SuppressWarnings("unchecked")
           Vector<String> choices = ((MultipleChoiceInputRequest) inputRequest).getChoices();
           validInputs = choices.toArray(new String[choices.size()]);
       }
       String result = GrailsConsole.getInstance().userInput(inputRequest.getPrompt(), validInputs);
       if(result == null || result.length() == 0) {
           result = inputRequest.getDefaultValue();
       }
       inputRequest.setInput(result);
    }
}
