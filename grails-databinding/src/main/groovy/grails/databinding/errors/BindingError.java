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
package grails.databinding.errors;

/**
 * Represents a problem which occurred during data binding.
 * 
 * @author Jeff Brown
 * @since 3.0
 */
public interface BindingError {
    /**
     * 
     * @return the object that data binding was being imposed upon
     */
    Object getObject();

    /**
     * 
     * @return the name of the property that the data binding error occurred on
     */
    String getPropertyName();

    /**
     * 
     * @return The value which could not be bound to the property
     */
    Object getRejectedValue();

    /**
     * 
     * @return an exception thrown during the data binding process
     */
    Throwable getCause();
}
