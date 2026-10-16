/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.web.converters.jackson;

import java.util.function.Supplier;

import tools.jackson.databind.SerializationContext;

/**
 * Finds the {@link DomainClassRendering} of a write: the one the writer supplied for it, such as the JSON converter does
 * for each domain class instance it renders, or else the one of the mapper's {@link DomainClassJacksonModule}. The
 * writer carries the write out on its own thread, so the rendering it supplies is the thread's for the duration.
 *
 * <p>The module's rendering is kept once it knows the application's domain classes, which it may not when the mapper
 * is built ahead of the application.</p>
 *
 * @since 9.0
 */
final class DomainRendering implements Supplier<DomainClassRendering> {

    /** The rendering a writer supplied for the write that is under way on this thread. */
    private static final ThreadLocal<DomainClassRendering> SUPPLIED = new ThreadLocal<>();

    private final Supplier<DomainClassRendering> source;

    private volatile DomainClassRendering kept;

    DomainRendering(Supplier<DomainClassRendering> source) {
        this.source = source;
    }

    @Override
    public DomainClassRendering get() {
        DomainClassRendering current = kept;
        if (current == null) {
            current = source.get();
            if (current.hasApplication()) {
                kept = current;
            }
        }
        return current;
    }

    /**
     * @param context the context of a write
     * @return the rendering to write with
     */
    DomainClassRendering of(SerializationContext context) {
        DomainClassRendering supplied = SUPPLIED.get();
        return supplied != null ? supplied : get();
    }

    /**
     * Runs a write, which the current thread carries out, with a rendering of its own.
     */
    static void with(DomainClassRendering rendering, Runnable write) {
        DomainClassRendering previous = SUPPLIED.get();
        SUPPLIED.set(rendering);
        try {
            write.run();
        }
        finally {
            if (previous != null) {
                SUPPLIED.set(previous);
            }
            else {
                SUPPLIED.remove();
            }
        }
    }
}
