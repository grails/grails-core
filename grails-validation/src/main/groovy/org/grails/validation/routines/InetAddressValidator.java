/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.validation.routines;

import java.io.Serializable;

/**
 * <p><b>InetAddress</b> validation and conversion routines (<code>java.net.InetAddress</code>).</p>
 *
 * <p>This class provides methods to validate a candidate IP address.
 *
 * <p>
 * This class is a Singleton; you can retrieve the instance via the {@link #getInstance()} method.
 * </p>
 *
 * @since Validator 1.4
 * @deprecated Replaced by newer version of commons-validation
 */
@Deprecated
public class InetAddressValidator implements Serializable {

    private static final long serialVersionUID = -3986248595858163100L;

    private static final String IPV4_REGEX = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";

    /**
     * Singleton instance of this class.
     */
    private static final InetAddressValidator VALIDATOR = new InetAddressValidator();

    /** IPv4 RegexValidator */
    private final RegexValidator ipv4Validator = new RegexValidator(IPV4_REGEX);

    /**
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     */
    public static InetAddressValidator getInstance() {
        return VALIDATOR;
    }

    /**
     * Checks if the specified string is a valid IP address.
     * @param inetAddress the string to validate
     * @return true if the string validates as an IP address
     */
    public boolean isValid(String inetAddress) {
        return isValidInet4Address(inetAddress);
    }

    /**
     * Validates an IPv4 address. Returns true if valid.
     * @param inet4Address the IPv4 address to validate
     * @return true if the argument contains a valid IPv4 address
     */
    public boolean isValidInet4Address(String inet4Address) {
        // verify that address conforms to generic IPv4 format
        String[] groups = ipv4Validator.match(inet4Address);

        if (groups == null) return false;

        // verify that address subgroups are legal
        for (int i = 0; i <= 3; i++) {
            String ipSegment = groups[i];
            if (ipSegment == null || ipSegment.length() <= 0) {
                return false;
            }

            int iIpSegment = 0;
            try {
                iIpSegment = Integer.parseInt(ipSegment);
            }
            catch(NumberFormatException e) {
                return false;
            }

            if (iIpSegment > 255) {
                return false;
            }
        }

        return true;
    }
}
