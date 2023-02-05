/*
 * Project   Gaurabda Java Library
 * Author    Yura Krymlov
 * Created   2022
 */

package org.gaurabda;

/**
 * @author Yura Krymlov
 * @version 1.0, 2022
 */
public class GCalManager {
    private static final GCalManager instance = new GCalManager();

    private GCalManager() {
        System.loadLibrary("gcal-11j");
    }

    /**
     * The method is intended to invoke native API to calc Gaurabda calendar for the given query.
     *
     * @return string in xml format which describes the result of the given query
     */
    public static native String nativeGCal4Query(String gcalQuery);
}
