/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2021-02
 */
package org.gaurabda;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/**
 * @author Yura Krymlov
 * @version 1.0, 2021-02
 */
public class GCalKyiv365Test extends AGaurabdaTest {

    @RepeatedTest(365)
    void testYears(RepetitionInfo info) {
        String query = "q=calendar&lc=Kyiv&la=50.4501&lo=30.5234&tm=1&td=1&tc=366&lt=2E00&dst=3x0x5x0x10x0x5x0&ty="
                + (1800 + info.getCurrentRepetition());

        String xml = GCalManager.nativeGCal4Query(query);
    }
}
