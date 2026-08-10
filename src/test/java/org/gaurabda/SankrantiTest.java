/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2026-08
 */
package org.gaurabda;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surya Sankranti: the moment the Sun enters a rasi, and the day the calendar files it under.
 * <p>
 * The two are not the same thing, and confusing them is what makes the library look wrong. In
 * {@code q=calendar} the enclosing {@code <day>} is the day the event is <b>attributed</b> to,
 * and the default rule carries an event that happens after local noon onto the next day. The
 * {@code time} attribute has always been the true moment, so date and time described
 * different days: Makara 2024 read as "15 Jan ... 23:04:46" when the crossing is on
 * 14 Jan at 23:04:46 local, and every other program says 14 January.
 * <p>
 * The fix was to make the element carry an explicit {@code date}, so date and time always
 * describe the same instant whatever the rule is. The default is left at rule 2 on purpose -
 * the attribution feeds the festival calculations, so it is not free to change - and
 * {@code &san=} selects any of the four rules for a caller that wants a different one.
 *
 * @author Yura Krymlov
 * @version 1.0, 2026-08
 */
public class SankrantiTest extends AGaurabdaTest {

    static final String KYIV = "&lc=Kyiv&la=50.4501&lo=30.5234&lt=2E00&dst=3x0x5x0x10x0x5x0";

    /** the day a sankranti is attributed to */
    static final String SAME_DAY = "&san=0";
    static final String BEFORE_SUNRISE_PREVIOUS = "&san=1";
    static final String AFTER_NOON_NEXT = "&san=2";
    static final String AFTER_SUNSET_NEXT = "&san=3";

    static final Pattern DAY = Pattern.compile("<day date=\"([^\"]+)\"");
    static final Pattern SANK_CAL = Pattern.compile(
            "<sankranti rasi=\"([^\"]+)\" date=\"([^\"]+)\" time=\"([^\"]+)\"");
    static final Pattern SANK_LIST = Pattern.compile(
            "<sank date=\"([^\"]+)\"[^>]*time=\"([^\"]+)\"[^>]*>\\s*<zodiac sans=\"([^\"]+)\"[^>]*id=\"(\\d+)\"");

    static final String[] MON = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    static String calendar(int year, int month, int day, int count, String extra) {
        return GCalManager.nativeGCal4Query("q=calendar" + KYIV + "&ty=" + year + "&tm=" + month
                + "&td=" + day + "&tc=" + count + extra);
    }

    static double toJd(String date, String hms) {
        String[] dp = date.split(" ");
        int d = Integer.parseInt(dp[0]), y = Integer.parseInt(dp[2]), m = 1;
        for (int i = 0; i < 12; i++) if (MON[i].equals(dp[1])) m = i + 1;

        String[] t = hms.split(":");
        double frac = (Integer.parseInt(t[0]) + Integer.parseInt(t[1]) / 60.
                + Integer.parseInt(t[2]) / 3600.) / 24.;

        int a = (14 - m) / 12, yy = y + 4800 - a, mm = m + 12 * a - 3;
        long jdn = d + (153L * mm + 2) / 5 + 365L * yy + yy / 4 - yy / 100 + yy / 400 - 32045;
        return jdn - 0.5 + frac;
    }

    /** the sankranti elements of a calendar, as {enclosingDay, rasi, trueDate, time} */
    static List<String[]> sankrantisOf(String xml) {
        assertNotNull(xml, "the native call returned null");
        List<String[]> out = new ArrayList<>();
        Matcher day = DAY.matcher(xml);
        Matcher s = SANK_CAL.matcher(xml);
        while (s.find()) {
            day.reset();
            String enclosing = "?";
            while (day.find() && day.start() < s.start()) enclosing = day.group(1);
            out.add(new String[]{enclosing, s.group(1), s.group(2), s.group(3)});
        }
        return out;
    }

    // ============================================== the case that started this

    /**
     * Makara Sankranti 2024 at Kyiv. Swiss Ephemeris puts the Sun at sidereal 270 degrees on
     * 14 Jan 2024 at 21:09 UT using gcal's own ayanamsa; gcal says 21:04:46 UT, the four
     * minutes being the difference between its ayanamsa polynomial and Lahiri. So the moment
     * is right - it is 14 January, 23:04:46 local time.
     */
    @Test
    void makara2024HappensOnTheFourteenthOfJanuary() {
        List<String[]> found = sankrantisOf(calendar(2024, 1, 13, 4, ""));
        assertEquals(1, found.size(), "one sankranti in 13-16 Jan 2024");

        String[] s = found.get(0);
        assertEquals("Makara", s[1]);
        assertEquals("14 Jan 2024", s[2], "the true date of the crossing");
        assertEquals("23:04:46", s[3], "local time of the crossing");
    }

    /**
     * The default stays rule 2 - an event after local noon is filed under the next day - and
     * that is deliberate: the attribution feeds the festival calculations, so changing it
     * moves more than the sankranti line. It is also what the reference files in
     * etc/GcalFiles were generated with.
     * <p>
     * The {@code date} attribute is how a caller gets the real day without changing the rule.
     */
    @Test
    void theDefaultRuleFilesMakara2024UnderTheFifteenth() {
        String[] s = sankrantisOf(calendar(2024, 1, 13, 4, "")).get(0);
        assertEquals("15 Jan 2024", s[0], "attributed day");
        assertEquals("14 Jan 2024", s[2], "true day - what other programs report");
    }

    /** and rule 0 files it under the day it happens on, for a caller that wants that */
    @Test
    void theSameDayRulePutsMakara2024UnderTheFourteenth() {
        String[] s = sankrantisOf(calendar(2024, 1, 13, 4, SAME_DAY)).get(0);
        assertEquals("14 Jan 2024", s[0], "attributed day");
        assertEquals("14 Jan 2024", s[2], "true day");
    }

    @Test
    void everyAttributionRuleKeepsTheSameMoment() {
        for (String rule : new String[]{"", SAME_DAY, BEFORE_SUNRISE_PREVIOUS,
                AFTER_NOON_NEXT, AFTER_SUNSET_NEXT}) {
            String[] s = sankrantisOf(calendar(2024, 1, 13, 4, rule)).get(0);
            assertEquals("14 Jan 2024", s[2], "true date with rule '" + rule + "'");
            assertEquals("23:04:46", s[3], "time with rule '" + rule + "'");
        }
    }

    /**
     * The rule is global state in the native library, so it has to be reset on every call -
     * otherwise one query would silently change the next.
     */
    @Test
    void theRuleDoesNotLeakIntoTheNextQuery() {
        assertEquals("14 Jan 2024", sankrantisOf(calendar(2024, 1, 13, 4, SAME_DAY)).get(0)[0]);
        assertEquals("15 Jan 2024", sankrantisOf(calendar(2024, 1, 13, 4, "")).get(0)[0],
                "the previous query's san=0 must not carry over into the default");
    }

    // ================================================== the moments themselves

    /**
     * A sankranti is the Sun crossing one 30 degree boundary, so the rasi sequence advances by
     * exactly one and consecutive crossings are between 29.3 and 31.6 days apart - the
     * perihelion and aphelion bounds. Checked on {@code q=sankranti}, where date and time
     * describe the same instant with no attribution rule in the way.
     */
    @Test
    void everySankrantiOfACenturyIsPhysicallyPossible() {
        List<double[]> found = new ArrayList<>();
        List<String> where = new ArrayList<>();

        for (int year = 1950; year <= 2050; year++) {
            String xml = GCalManager.nativeGCal4Query(
                    "q=sankranti" + KYIV + "&ty=" + year + "&tm=1&td=1&tc=370");
            assertNotNull(xml, "year " + year);
            Matcher m = SANK_LIST.matcher(xml);
            while (m.find()) {
                double jd = toJd(m.group(1), m.group(2));
                if (!found.isEmpty() && jd - found.get(found.size() - 1)[0] < 1.) continue;
                found.add(new double[]{jd, Integer.parseInt(m.group(4))});
                where.add(m.group(1) + " " + m.group(2) + " " + m.group(3));
            }
        }

        assertTrue(found.size() > 1200, "expected about 12 a year, got " + found.size());

        for (int i = 1; i < found.size(); i++) {
            double gap = found.get(i)[0] - found.get(i - 1)[0];
            int prev = (int) found.get(i - 1)[1], cur = (int) found.get(i)[1];

            assertEquals((prev + 1) % 12, cur,
                    "rasi sequence broke at " + where.get(i) + " after " + where.get(i - 1));
            assertTrue(gap > 29.3 && gap < 31.6, String.format(Locale.ROOT,
                    "%s after %s: %.4f days, outside the physical bounds",
                    where.get(i), where.get(i - 1), gap));
        }
    }

    /**
     * The two query types describe the same event in different time frames:
     * {@code q=sankranti} in UT, {@code q=calendar} in local time. The gap must be exactly the
     * time zone, so that anything reading both can reconcile them.
     */
    @Test
    void theTwoQueryTypesDifferByExactlyTheTimeZone() {
        String list = GCalManager.nativeGCal4Query(
                "q=sankranti" + KYIV + "&ty=2024&tm=1&td=1&tc=40");
        assertNotNull(list);
        Matcher m = SANK_LIST.matcher(list);
        assertTrue(m.find(), "no sankranti in the list output");

        double ut = toJd(m.group(1), m.group(2));
        String[] cal = sankrantisOf(calendar(2024, 1, 13, 4, "")).get(0);
        double local = toJd(cal[2], cal[3]);

        assertEquals(2. / 24., local - ut, 1. / 86400.,
                "q=sankranti is UT, q=calendar is local time at +2");
    }

    /** the calendar must not lose a sankranti, whichever rule is in force */
    @Test
    void aYearOfMonthlyQueriesFindsTwelveSankrantis() {
        for (String rule : new String[]{"", SAME_DAY, AFTER_SUNSET_NEXT}) {
            int total = 0;
            for (int month = 1; month <= 12; month++) {
                int days = java.time.YearMonth.of(2024, month).lengthOfMonth();
                total += sankrantisOf(calendar(2024, month, 1, days, rule)).size();
            }
            assertEquals(12, total, "12 sankrantis in 2024 with rule '" + rule + "'");
        }
    }
}
