/*
 * Copyright 2014 Pivotal
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
package grails.test.mixin;

import grails.test.runtime.SharedRuntimeConfigurer;
import grails.test.runtime.TestPlugin;
import grails.test.runtime.TestRuntime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for registering custom {@link TestPlugin} classes to the {@link TestRuntime} of the current test class.
 * 
 * This annotation can be also used on custom {@link SharedRuntimeConfigurer} classes to register test plugin classes.
 *
 * @author Lari Hotari
 * @since 2.4.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface UseTestPlugin {
    /**
     * @return TestPlugin classes to register in the runtime
     */
    Class<? extends TestPlugin>[] value();
    boolean exclude() default false;
}
