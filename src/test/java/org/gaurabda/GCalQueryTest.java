/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2026-10
 */
package org.gaurabda;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The query kinds other than q=calendar and q=sankranti (see {@link GCalCalendarTest} and
 * {@link SankrantiTest}), the query string itself and the state between calls. The values are
 * those of Kyiv 2024.
 * <p>
 * Not asked here: q=gcalendar (a whole Gaurabda year, over the 64 KB of the output buffer -
 * a deliberate limit) and q=appday (returns the buffer of a TString already destroyed).
 * <p>
 * The numbering of masas differs between the queries: q=gtithi takes gm from 0 for
 * Madhusudana (7 = Kesava), q=next and q=gnaksatra take it from 1 (3 = Vamana), q=masastart
 * prints masa from 0 for Visnu (5 = Hrsikesa). The tithi of q=gtithi and q=next is 1..15
 * within the paksa (gp 0 = Krsna, 1 = Gaura).
 *
 * @author Yura Krymlov
 * @version 1.0, 2026-10
 */
public class GCalQueryTest extends AGaurabdaTest {

    static final String KYIV = GCalCalendarTest.KYIV;

    static String query(String query) {
        String xml = GCalManager.nativeGCal4Query(query);
        assertNotNull(xml, query);
        return xml;
    }

    static String attr(String xml, String regex) {
        Matcher m = Pattern.compile(regex).matcher(xml);
        assertTrue(m.find(), regex);
        return m.group(1);
    }

    static List<String> all(String xml, String regex) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(xml);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    static String result(String xml) {
        return xml.substring(xml.indexOf("<result"));
    }

    @Test
    void testFirstDay() {
        String xml = query("q=firstday" + KYIV + "&ty=2024");
        assertEquals("26 Mar 2024", attr(xml, "<firstday date=\"([^\"]+)\""));
        assertEquals("2", attr(xml, "dayweekid = \"(\\d)\""));
        assertEquals("Tuesday", attr(xml, "dayweek=\"([^\"]+)\""));
    }

    @Test
    void testTithi() {
        String xml = query("q=tithi" + KYIV + "&ty=2024&tm=6&td=15&th=6&tmin=30");
        assertEquals("23", attr(xml, "id=\"(\\d+)\""));
        assertEquals("Navami", attr(xml, "name=\"([^\"]+)\"\\s+startdate"));
        assertEquals("14 Jun 2024", attr(xml, "startdate=\"([^\"]+)\""));
        assertEquals("20:35:55", attr(xml, "starttime=\"([^\"]+)\""));
        assertEquals("15 Jun 2024", attr(xml, "enddate=\"([^\"]+)\""));
        assertEquals("23:04:30", attr(xml, "endtime=\"([^\"]+)\""));
    }

    @Test
    void testTithiIsTheTithiOfTheDay() {
        // the library as it is: th and tmin are not used, the answer is the tithi of the
        // day (at its sunrise) - at 23:30 of 15 Jun still Navami, which ended at 23:04:30
        String morning = query("q=tithi" + KYIV + "&ty=2024&tm=6&td=15&th=0&tmin=0");
        assertEquals(result(morning), result(query("q=tithi" + KYIV + "&ty=2024&tm=6&td=15&th=23&tmin=30")));

        // the end of one tithi is the start of the next one
        String next = query("q=tithi" + KYIV + "&ty=2024&tm=6&td=16&th=6&tmin=0");
        assertEquals("24", attr(next, "id=\"(\\d+)\""));
        assertEquals(attr(morning, "enddate=\"([^\"]+)\""), attr(next, "startdate=\"([^\"]+)\""));
        assertEquals(attr(morning, "endtime=\"([^\"]+)\""), attr(next, "starttime=\"([^\"]+)\""));
    }

    @Test
    void testMasaStart() {
        String xml = query("q=masastart" + KYIV + "&ty=2024&tm=9&td=10");
        assertEquals("20 Aug 2024", attr(xml, "<masabegin date=\"([^\"]+)\""));
        assertEquals("5", attr(xml, "<masa>(\\d+)</masa>"));
        assertEquals("Hrsikesa", attr(xml, "<masaname>([^<]+)</masaname>"));
        assertEquals("538", attr(xml, "<gyear>(\\d+)</gyear>"));
    }

    @Test
    void testPaksaStart() {
        // the request is named "FirstMasaDay" here too
        String xml = query("q=paksastart" + KYIV + "&ty=2024&tm=9&td=10");
        assertEquals("3 Sep 2024", attr(xml, "<masabegin date=\"([^\"]+)\""));
        assertEquals("Hrsikesa", attr(xml, "<masaname>([^<]+)</masaname>"));
    }

    @Test
    void testGTithi() {
        // Kesava Gaura Purnima in the Gaurabda years 538 and 539
        String xml = query("q=gtithi" + KYIV + "&ty=2024&tye=2025&gm=7&gp=1&gt=15");
        assertEquals(Arrays.asList("15 Dec 2024", "4 Dec 2025"), all(xml, "date=\"([^\"]+)\""));
        assertEquals(Arrays.asList("Kesava", "Kesava"), all(xml, "rmasa=\"([^\"]+)\""));
        assertEquals(Arrays.asList("Purnima", "Purnima"), all(xml, "rtithi=\"([^\"]+)\""));
        assertEquals(Arrays.asList("Gaura", "Gaura"), all(xml, "rpaksa=\"([^\"]+)\""));
    }

    @Test
    void testNext() {
        // the next Vamana Krsna Ekadasi after 1 Jan 2024
        String xml = query("q=next" + KYIV + "&ty=2024&tm=1&td=1&gm=3&gp=0&gt=11");
        assertEquals(Arrays.asList("2 Jul 2024"), all(xml, "date=\"([^\"]+)\""));
        assertEquals("Vamana", attr(xml, "rmasa=\"([^\"]+)\""));
        assertEquals("Ekadasi", attr(xml, "rtithi=\"([^\"]+)\""));
        assertEquals("Krsna", attr(xml, "rpaksa=\"([^\"]+)\""));
    }

    @Test
    void testMasaNumbering() {
        // the same masa: gm=2 for q=gtithi, gm=3 for q=next
        String gtithi = query("q=gtithi" + KYIV + "&ty=2024&tye=2024&gm=2&gp=0&gt=11");
        String next = query("q=next" + KYIV + "&ty=2024&tm=1&td=1&gm=3&gp=0&gt=11");
        assertEquals("Vamana", attr(gtithi, "rmasa=\"([^\"]+)\""));
        assertEquals(attr(next, "date=\"([^\"]+)\""), attr(gtithi, "date=\"([^\"]+)\""));
    }

    @Test
    void testGNaksatraStartsWithMasa() {
        // q=gnaksatra starts on the first day of the masa that q=masastart gives
        String xml = query("q=gnaksatra" + KYIV + "&gy=538&gm=5&tc=3");
        assertEquals("20 Aug 2024", attr(xml, "<arg name=\"startdate\" val=\"([^\"]+)\""));
        assertEquals("20 Aug 2024", attr(xml, "<day date=\"([^\"]+)\""));
    }

    @Test
    void testNaksatraIdsAreConsecutive() {
        for (String xml : new String[]{
                query("q=naksatra" + KYIV + "&ty=2024&tm=3&td=1&tc=30"),
                query("q=gnaksatra" + KYIV + "&gy=538&gm=5&tc=30")}) {
            List<String> ids = all(xml, "<naksatra id=\"(\\d+)\"");
            assertTrue(ids.size() > 25, "naksatras " + ids.size());
            for (int i = 1; i < ids.size(); i++)
                assertEquals((Integer.parseInt(ids.get(i - 1)) + 1) % 27, Integer.parseInt(ids.get(i)));
        }
        String xml = query("q=naksatra" + KYIV + "&ty=2024&tm=3&td=1&tc=3");
        assertEquals("Visakha", attr(xml, "<naksatra id=\"15\" name=\"([^\"]+)\""));
        assertEquals("09:19:24", attr(xml, "starttime=\"([^\"]+)\""));
    }

    @Test
    void testNaksatraIgnoresDayCount() {
        // the library as it is: tc is printed among the arguments, the list is always the same
        String one = query("q=naksatra" + KYIV + "&ty=2024&tm=3&td=1&tc=1");
        String many = query("q=naksatra" + KYIV + "&ty=2024&tm=3&td=1&tc=100");
        assertEquals(result(one), result(many));
    }

    @Test
    void testInvalidQueries() {
        assertNull(GCalManager.nativeGCal4Query(""));
        assertNull(GCalManager.nativeGCal4Query("q=c"));
        assertNull(GCalManager.nativeGCal4Query("q=zzz" + KYIV));
    }

    @Test
    void testCoordinateFormats() {
        String day = "q=calendar&lc=X&ty=2024&tm=6&td=21&tc=3&dst=0x0x0x0x0x0x0x0";
        String[][] same = {
                {"&la=50N27&lo=30E31&lt=2E00", "&la=50.45&lo=30.516667&lt=2E00"},
                {"&la=50N27&lo=30E31&lt=2E00", "&la=50N27&lo=30E31&lt=2.0"},
                {"&la=50N27&lo=30E31&lt=2E00", "&la=50N27&lo=30E31&lt=2"},
                {"&la=40N43&lo=74W00&lt=5W00", "&la=40.716667&lo=-74.0&lt=-5.0"},
                {"&la=28N36&lo=77E12&lt=5E30", "&la=28N36&lo=77E12&lt=5.5"},
                {"&la=33S52&lo=151E12&lt=10E00", "&la=-33.866667&lo=151.2&lt=10"},
        };
        for (String[] pair : same)
            assertEquals(result(query(day + pair[0])), result(query(day + pair[1])), pair[0] + " vs " + pair[1]);
    }

    @Test
    void testCallsAreIndependent() {
        // the library keeps global state: a call in between, for another place and kind,
        // must not change the answer
        String kyiv = "q=calendar" + KYIV + "&ty=2024&tm=3&td=29&tc=4";
        String first = query(kyiv);
        query("q=calendar" + GCalCalendarTest.SYDNEY + "&ty=1950&tm=10&td=1&tc=31&san=3");
        query("q=gtithi" + GCalCalendarTest.NEW_YORK + "&ty=2099&tye=2100&gm=0&gp=0&gt=1");
        query("q=sankranti" + GCalCalendarTest.MAYAPUR + "&ty=1980&tm=1&td=1&tc=366");
        assertEquals(first, query(kyiv));
        assertNotEquals(first, query("q=calendar" + GCalCalendarTest.SYDNEY + "&ty=2024&tm=3&td=29&tc=4"));
    }
}
