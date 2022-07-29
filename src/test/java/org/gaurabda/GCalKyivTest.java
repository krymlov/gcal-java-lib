/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2021-02
 */
package org.gaurabda;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import java.util.Random;

/**
 * Parameters:
 *
 * @author Yura Krymlov
 * @version 1.0, 2021-02
 */
public class GCalKyivTest extends AGaurabdaTest {
    private static final Random random = new Random();

    @RepeatedTest(50)
    void testMonths(RepetitionInfo info) {
        String query = makeGCalQuery(2000 + info.getCurrentRepetition(),
                random.nextInt(12) + 1, random.nextInt(100) + 1);
        System.out.println(query + " => " + GCalManager.nativeGCal4Query(query).length() + " bytes");
    }

    /**
     * lc - location name like Mayapur
     * la - latitude like 12N30 or 15S06
     * lo - longitude like 56E13 or 10W45
     * lt - timezone like 5E30 or 7W00
     * ty - year like 2008, 1965 from 1500 up to 3000
     * tm - month [1...12]
     * td - day [1...31]
     * tc - count of days like 10 or 300
     * dst - daylight saving time data, like 3x0x5x0x10x0x5x0
     *
     * @return gcal query
     */
    String makeGCalQuery(int year, int month, int countOfDays) {
        final StringBuilder sb = new StringBuilder(255);
        sb.append("q=calendar&lc=Kyiv&la=50N27&lo=30E31");

        sb.append("&ty=").append(year).append("&tm=").append(month);
        sb.append("&td=").append(1).append("&tc=").append(countOfDays);
        sb.append("&lt=2E00").append("&dst=3x0x5x0x10x0x5x0");

        return sb.toString();
    }
}
