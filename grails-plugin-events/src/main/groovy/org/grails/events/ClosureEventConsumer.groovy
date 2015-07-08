/*
 * Copyright 2015 original authors
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
package org.grails.events

import groovy.transform.CompileStatic
import reactor.bus.Bus
import reactor.bus.Event
import reactor.bus.EventBus
import reactor.core.processor.CancelException
import reactor.fn.Consumer



/**
 * Had to fork this class as Reactor 2.0 M2 Groovy support is compiled with Java 8
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 * @author Graeme Rocher
 */
@CompileStatic
class ClosureEventConsumer<T> implements Consumer<Event<T>> {

    final Closure callback
    final boolean eventArg

    ClosureEventConsumer(Closure cl) {
        callback = cl
        callback.delegate = this
        def argTypes = callback.parameterTypes
        this.eventArg = Event.isAssignableFrom(argTypes[0])
    }

    void cancel() {
        throw CancelException.get()
    }

    @Override
    void accept(Event<T> arg) {
        def callback = this.callback
        if (EventBus.ReplyToEvent.class.isAssignableFrom(arg.class)) {
            callback = (Closure) callback.clone()
            callback.delegate = new ReplyDecorator(arg.replyTo, (((EventBus.ReplyToEvent) arg).replyToObservable))
        }
        if (eventArg) {
            callback arg
        } else {
            callback arg?.data
        }
    }

    class ReplyDecorator {

        final replyTo
        final Bus observable

        ReplyDecorator(replyTo, Bus observable) {
            this.replyTo = replyTo
            this.observable = observable
        }


        void reply() {
            observable.notify(replyTo, new Event<Void>(Void))
        }

        void reply(data) {
            observable.notify(replyTo, Event.wrap(data))
        }
    }
}
