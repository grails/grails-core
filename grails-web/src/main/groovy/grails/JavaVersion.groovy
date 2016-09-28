package grails

/**
 * Created by jameskleeh on 9/28/16.
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
