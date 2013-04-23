/*
 * Copyright 2013 the original author or authors.
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
package org.grails.databinding;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation may be applied to a class or to a field to
 * customize the data binding process.  
 * 
 * When the annotation is applied to a field, the value assigned to the
 * annotation should be a Closure which accepts 2 parameters.  The first 
 * parameter is the object that data binding is being applied to.  The second
 * parameter is a Map containing the values being bound to the object.
 * The value returned by the Closure will be bound to the field.  The
 * following code demonstrates using this technique to bind an upper
 * case version of the value in the Map to the field.
 * 
 * 
<pre>
class SomeClass {
    &#064;BindUsing({
        obj, source -> source['name']?.toUpperCase()
    })
    String name
}
</pre>
 * 
 * When the annotation is applied to a class, the value assigned to the
 * annotation should be a class which implements the {@link BindingHelper}
 * interface.  An instance of that class will be used any time a value
 * is bound to a property in the class that this annotation has been
 * applied to.
<pre>
&#064;BindUsing(SomeClassWhichImplementsBindingHelper)
class SomeClass {
    String someProperty
    Integer someOtherProperty
}
</pre>
 *
 * @author Jeff Brown
 * @since 2.3
 * @see BindingHelper
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BindUsing {
    Class<?> value();
}
