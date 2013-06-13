/*
 * Copyright 2012 the original author or authors.
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
package org.grails.wrapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * RemoteFileHelper class used by {@link GrailsWrapper} for wrapper
 *
 * @author Jeff Brown
 * @author Hans Dockter
 * @since 2.1
 */
public class RemoteFileHelper {
    private static final int CHUNK_SIZE = 20000;
    private static final int BUFFER_SIZE = 10000;

    public void retrieve(final URI address, final File destination) throws Exception {
        if (destination.exists()) {
            return;
        }
        destination.getParentFile().mkdirs();

        System.out.println("Downloading " + address + " to " + destination.getAbsolutePath());
        OutputStream out = null;
        InputStream in = null;
        try {
            final URL url = address.toURL();
            out = new BufferedOutputStream(
                    new FileOutputStream(destination));
            final URLConnection conn = url.openConnection();
            in = conn.getInputStream();
            final byte[] buffer = new byte[BUFFER_SIZE];
            int numRead;
            long progressCounter = 0;
            while ((numRead = in.read(buffer)) != -1) {
                progressCounter += numRead;
                if (progressCounter / CHUNK_SIZE > 0) {
                    System.out.print(".");
                    progressCounter = progressCounter - CHUNK_SIZE;
                }
                out.write(buffer, 0, numRead);
            }
        } finally {
            System.out.println("");
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
