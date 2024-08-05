/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2021-02
 */
package org.gaurabda;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Parameters:
 *
 * @author Yura Krymlov
 * @version 1.0, 2021-02
 */
public class GCalKyivTest extends AGaurabdaTest {
    private static final Random random = new Random();

    @RepeatedTest(5)
    void testMonths(RepetitionInfo info) throws IOException {
        int i = info.getCurrentRepetition();
        String query = makeGCalQuery(2020 + i, i, 120);
		String gcalXml = GCalManager.nativeGCal4Query(query);
        System.out.println(query + " => " + gcalXml.length() + " bytes");
        //String normalizedFilename = FilenameUtils.normalize(query + ".xml");
        //normalizedFilename = normalizedFilename.replace(':', '_');
        //FileUtils.writeStringToFile(new File(normalizedFilename), gcalXml);
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
        //sb.append("q=calendar&lc=Kyiv&la=50N27&lo=30E31");
        sb.append("q=calendar&lc=Kyiv&la=50.45&lo=30E31");

        sb.append("&ty=").append(year).append("&tm=").append(month);
        sb.append("&td=").append(1).append("&tc=").append(countOfDays);
        sb.append("&lt=2E00").append("&dst=3x0x5x0x10x0x5x0");

        return sb.toString();
    }
}
