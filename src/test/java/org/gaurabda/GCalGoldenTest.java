/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2026-10
 */
package org.gaurabda;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The calendars of {@link GCalKyivTest} and {@link GCalMinskTest} (2019-2028, month by month)
 * must stay what the released library gives: etc/GcalFiles/gcal-11j/&lt;city&gt;/&lt;year&gt;/
 * month_&lt;m&gt;.fst.xml. A rebuild of the DLL (another compiler, other optimization) that
 * changes a single byte of 240 months fails here.
 *
 * @author Yura Krymlov
 * @version 1.0, 2026-10
 */
public class GCalGoldenTest extends AGaurabdaTest {

    static final File GOLDEN = new File("etc/GcalFiles/gcal-11j");

    static Stream<Arguments> months() {
        Stream.Builder<Arguments> sb = Stream.builder();
        for (String city : new String[]{"Kyiv", "Minsk"})
            for (int year = 2019; year <= 2028; year++)
                for (int month = 1; month <= 12; month++)
                    sb.add(Arguments.of(city, year, month));
        return sb.build();
    }

    @ParameterizedTest(name = "{0} {1}-{2}")
    @MethodSource("months")
    void testMonthAsReleased(String city, int year, int month) throws IOException {
        File file = new File(GOLDEN, city + "/" + year + "/month_" + month + ".fst.xml");
        assertTrue(file.isFile(), "no golden file " + file);

        int days = YearMonth.of(year, month).lengthOfMonth();
        String query = "Kyiv".equals(city)
                ? new GCalKyivTest().makeGCalQuery(year, month, days)
                : new GCalMinskTest().makeGCalQuery(year, month, days);

        String gcalXml = GCalManager.nativeGCal4Query(query);
        assertNotNull(gcalXml, query);

        // a checkout with core.autocrlf may turn the files into CRLF
        String expected = FileUtils.readFileToString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(expected, gcalXml.replace("\r\n", "\n"), query);
    }
}
