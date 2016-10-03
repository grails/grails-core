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
package grails.internal

/**
 * A class to determine the runtime Java version
 *
 * @author James Kleeh
 * @since 3.2.1
 */
class JavaVersion {

    static Boolean isAtLeast(int major, int minor) {
        String version = System.getProperty("java.version");
        int firstPos = version.indexOf('.');
        int currMajor = Integer.parseInt(version.substring(0, firstPos));
        int secondPos = version.indexOf('.', firstPos+1);
        int currMinor = Integer.parseInt(version.substring(firstPos+1, secondPos));
        currMajor >= major && currMinor >= minor
    }
}
