/*
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
 */
package grails.events

import groovy.transform.CompileStatic
import org.grails.events.ClosureEventConsumer
import org.springframework.beans.factory.annotation.Autowired
import reactor.bus.Bus
import reactor.bus.Event
import reactor.bus.EventBus
import reactor.bus.registry.Registration
import reactor.bus.selector.Selector
import reactor.bus.selector.Selectors
import reactor.fn.Consumer
import reactor.fn.Supplier


/**
 * A trait that can be applied to any class to enable event sending and receiving
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
trait Events {

    @Autowired
    EventBus eventBus

    /**
     * @see #on(reactor.bus.selector.Selector, reactor.fn.Consumer)
     */
    public <E extends Event<?> > Registration<Object, Consumer<E>> on(Class key, @DelegatesTo(strategy = Closure.DELEGATE_FIRST,
            value = ClosureEventConsumer.ReplyDecorator) Closure consumer) {
        on key, new ClosureEventConsumer<E>(consumer)
    }

    /**
     * @see #on(reactor.bus.selector.Selector, reactor.fn.Consumer)
     */
    public <E extends Event<?> > Registration<Object, Consumer<E>> on(Selector key, @DelegatesTo(strategy = Closure.DELEGATE_FIRST,
            value = ClosureEventConsumer.ReplyDecorator) Closure consumer) {
        on key, new ClosureEventConsumer<E>(consumer)
    }

    /**
     * @see #on(reactor.bus.selector.Selector, reactor.fn.Consumer)
     */
    public <E extends Event<?> > Registration<Object, Consumer<E>> on(key, @DelegatesTo(strategy = Closure.DELEGATE_FIRST,
            value = ClosureEventConsumer.ReplyDecorator) Closure consumer) {
        on key, new ClosureEventConsumer<E>(consumer)
    }

    /**
     * @see #on(reactor.bus.selector.Selector, reactor.fn.Consumer)
     */
    public <E extends Event<?> > Registration<Object, Consumer<E>> on(key, Consumer<E> consumer) {
        if(key instanceof CharSequence) {
            key = key.toString()
        }
        on(Selectors.$((Object)key), consumer)
    }

    /**
     * @see #on(reactor.bus.selector.Selector, reactor.fn.Consumer)
     */
    public <E extends Event<?> > Registration<Object, Consumer<E>> on(Class type, Consumer<E> consumer) {
        on(Selectors.T(type), consumer)
    }

    /**
     * Register a {@link reactor.fn.Consumer} to be triggered when a notification matches the given {@link
     * Selector}.
     *
     * @param sel
     * 		The {@literal Selector} to be used for matching
     * @param consumer
     * 		The {@literal Consumer} to be triggered
     * @param <E>
     * 		The type of the {@link Event}
     *
     * @return A {@link Registration} object that allows the caller to interact with the given mapping
     */
    public <E extends Event<?> > Registration<Object, Consumer<E>> on(Selector sel, Consumer<E> consumer) {
        if(eventBus == null) throw new IllegalStateException("EventBus not present. Event registration attempted outside of application context.")
        eventBus.on sel, consumer
    }

    /**
     * @see reactor.bus.Bus#notify(java.lang.Object, java.lang.Object)
     */
    public Bus notify(Object key, Event<?> ev) {
        if(eventBus == null) throw new IllegalStateException("EventBus not present. Event notification attempted outside of application context.")
        if(ev.replyTo) {
            eventBus.send key, ev
        }
        else {
            eventBus.notify key, ev
        }
    }

    /**
     * @see reactor.bus.Bus#notify(java.lang.Object, reactor.bus.Event)
     */
    public Bus notify(Object key, data) {
        notify key, Event.wrap(data)
    }

    /**
     * @see reactor.bus.Bus#notify(java.lang.Object, reactor.bus.Event)
     */
    public <E extends Event<?>> Bus notify(Object key, Closure<E> supplier) {
        if(eventBus == null) throw new IllegalStateException("EventBus not present. Event notification attempted outside of application context.")
        eventBus.notify key, supplier as Supplier<E>
    }

    /**
     * @see EventBus#sendAndReceive(java.lang.Object, reactor.bus.Event, reactor.fn.Consumer)
     */
    public Bus sendAndReceive(Object key, data, @DelegatesTo(strategy = Closure.DELEGATE_FIRST,
            value = ClosureEventConsumer.ReplyDecorator) Closure reply) {
        eventBus.sendAndReceive key, Event.wrap(data), new ClosureEventConsumer(reply)
    }

    /**
     * @see EventBus#sendAndReceive(java.lang.Object, reactor.fn.Supplier, reactor.fn.Consumer)
     */
    public <E extends Event<?>> Bus sendAndReceive(Object key, Closure<E> supplier, @DelegatesTo(strategy = Closure.DELEGATE_FIRST,
            value = ClosureEventConsumer.ReplyDecorator) Closure reply) {
        eventBus.sendAndReceive key, supplier as Supplier<E>, new ClosureEventConsumer(reply)
    }



    /**
     * Creates an {@link Event} for the given data
     *
     * @param data The data
     * @return The event
     */
    public <T> Event<T> eventFor(T data) {
        return Event.wrap(data)
    }

    /**
     * Creates an {@link Event} for the given headers and data
     *
     * @param headers The headers
     * @param data The data
     * @return The event
     */
    public <T> Event<T> eventFor(Map<String, Object> headers, T data) {
        return new Event(new Event.Headers(headers), data)
    }

    /**
     * Creates an {@link Event} for the given headers, data and error consumer
     *
     * @param headers The headers
     * @param data The data
     * @param errorConsumer The errors consumer
     * @return The event
     */
    public <T> Event<T> eventFor(Map<String, Object> headers, T data, Closure<Throwable> errorConsumer) {
        return new Event(new Event.Headers(headers), data, errorConsumer as Consumer<Throwable>)
    }

    /**
     * Clears event consumers for the given key
     * @param key The key
     * @return True if modifications were made
     */
    boolean clearEventConsumers(key) {
        if(eventBus) {
            return eventBus.consumerRegistry.unregister(key)
        }
        return false
    }

}