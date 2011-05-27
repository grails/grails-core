/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.codehaus.groovy.grails.resolve;

import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

/**
 * Correctly handles last modified in a resolved Ivy resource.
 *
 * @since 1.4
 * @author Luke Daley
 * @author Graeme Rocher
 */
public class LastModifiedResolvedResource extends ResolvedResource {

    private long lastModified;

    public LastModifiedResolvedResource(Resource res, String rev) {
        this(res, rev, -1);
    }

    public LastModifiedResolvedResource(Resource res, String rev, long lastModified) {
        super(res, rev);
        this.lastModified = lastModified;
    }

    @Override
    public long getLastModified() {
        if (lastModified < 0) {
            return getResource().getLastModified();
        }
        return lastModified;
    }
}
