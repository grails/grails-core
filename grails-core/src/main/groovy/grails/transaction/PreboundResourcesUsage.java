/*******************************************************************************
 * Copyright 2014 original authors
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
 *******************************************************************************/
package grails.transaction;

/**
 * Setting for determining the use of pre-bound resources like an OpenSessionInView session
 * 
 * With the DONT_USE setting, all pre-bound resources are unbinded before entering the transaction context and 
 * rebinded after exiting the context.
 * 
 * In ADAPTIVE mode, the decision to do the unbinding&rebinding is determined based on the propagation behaviour of 
 * the transaction context and the state of the current transaction context.
 * With PROPAGATION_REQUIRED and active transaction, it will REUSE resources. When propagation is 
 * PROPAGATION_SUPPORTS or PROPAGATION_MANDATORY , it will also REUSE resources.
 * 
 * The REUSE mode just means that no unbinding & rebinding won't happen at all so it will reuse any pre-bound resources 
 * like the OpenSessionInView session.
 * 
 * @since 2.4.5
 */
public enum PreboundResourcesUsage {
    ADAPTIVE,
    REUSE,
    DONT_USE
}
