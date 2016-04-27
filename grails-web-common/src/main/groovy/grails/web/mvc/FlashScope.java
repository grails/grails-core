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
package grails.web.mvc;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents flash scope and allows a flash scope map to be set to the next
 * state. Flash scope essentially allows variables to be maintained for the next state and the next
 * state only, what this means is if objects are stored inside FlashScope on the first call to
 * "next()" they will be still there, but on the subsequent call they will be cleared unless they
 * have been set again since the last call to "next()"
 *
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public interface FlashScope extends Map<String, Object>, Serializable {

    /**
     * The attribute containing the chain model
     */
    String CHAIN_MODEL = "chainModel";

    /**
     * Sets the flash scope to the next state upon a new request
     */
    void next();

    /**
     * Returns the current state of flash scope, to be used if you don't
     * want to include variables in the next request
     *
     * @return A map
     */
    Map getNow();
}
