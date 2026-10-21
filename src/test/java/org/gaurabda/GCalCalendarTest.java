/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2026-10
 */
package org.gaurabda;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * q=calendar: what must hold for every day of any month, and daylight saving time.
 * <p>
 * The output buffer of the library is 64 KB (XMLOut in astro_w.cpp, a deliberate limit),
 * so a calendar is asked one month at a time.
 *
 * @author Yura Krymlov
 * @version 1.0, 2026-10
 */
public class GCalCalendarTest extends AGaurabdaTest {

    static final String KYIV = "&lc=Kyiv&la=50.4501&lo=30.5234&lt=2E00&dst=3x0x5x0x10x0x5x0";
    static final String MAYAPUR = "&lc=Mayapur&la=23N25&lo=88E24&lt=5E30&dst=0x0x0x0x0x0x0x0";
    static final String NEW_YORK = "&lc=New+York&la=40N43&lo=74W00&lt=5W00&dst=3x0x2x0x11x0x1x0";
    static final String SYDNEY = "&lc=Sydney&la=33S52&lo=151E12&lt=10E00&dst=10x0x1x0x4x0x1x0";
    static final String[] LOCATIONS = {KYIV, MAYAPUR, NEW_YORK, SYDNEY};

    static final int XML_OUT_SIZE = 65536;

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    static final Pattern DAY = Pattern.compile(
            "<day date=\"([^\"]+)\" dayweekid=\"(\\d)\"[^>]*>(.*?)</day>", Pattern.DOTALL);
    static final Pattern MASA = Pattern.compile("<masa name=\"([^\"]+)\" gyear=\"Gaurabda (\\d+)\"");

    /** one {@code <day>} of q=calendar */
    static class Day {
        LocalDate date;
        int weekday;
        LocalTime sunrise, arunodaya, noon, sunset;
        int tithi, dst;
        String masa;
        int gyear;
    }

    static String calendar(String location, int year, int month, int day, int count, String extra) {
        String query = "q=calendar" + location + "&ty=" + year + "&tm=" + month
                + "&td=" + day + "&tc=" + count + extra;
        String xml = GCalManager.nativeGCal4Query(query);
        assertNotNull(xml, query);
        return xml;
    }

    static String attr(String block, String regex) {
        Matcher m = Pattern.compile(regex).matcher(block);
        assertTrue(m.find(), regex + " in " + block);
        return m.group(1);
    }

    static List<Day> days(String xml) {
        List<Day> out = new ArrayList<>();
        Matcher masa = MASA.matcher(xml);
        Matcher m = DAY.matcher(xml);
        String masaName = null;
        int gyear = 0, masaFrom = 0;
        while (m.find()) {
            // the <masa> opened last before this day
            masa.region(masaFrom, m.start());
            while (masa.find()) {
                masaName = masa.group(1);
                gyear = Integer.parseInt(masa.group(2));
                masaFrom = masa.end();
            }
            String b = m.group(3);
            Day d = new Day();
            d.date = LocalDate.parse(m.group(1), DATE);
            d.weekday = Integer.parseInt(m.group(2));
            d.sunrise = LocalTime.parse(attr(b, "<sunrise time=\"([^\"]+)\""));
            d.arunodaya = LocalTime.parse(attr(b, "<arunodaya time=\"([^\"]+)\""));
            d.noon = LocalTime.parse(attr(b, "<noon time=\"([^\"]+)\""));
            d.sunset = LocalTime.parse(attr(b, "<sunset time=\"([^\"]+)\""));
            d.tithi = Integer.parseInt(attr(b, "<tithi name=\"[^\"]+\" elapse=\"[^\"]+\" index=\"(\\d+)\""));
            d.dst = Integer.parseInt(attr(b, "<dst offset=\"(\\d+)\""));
            d.masa = masaName;
            d.gyear = gyear;
            out.add(d);
        }
        return out;
    }

    static Day dayOf(List<Day> days, LocalDate date) {
        for (Day d : days) if (d.date.equals(date)) return d;
        throw new AssertionError("no day " + date);
    }

    static String result(String xml) {
        return xml.substring(xml.indexOf("<result"));
    }

    @Test
    void testEveryDayOf2024() {
        for (String location : LOCATIONS) {
            for (int month = 1; month <= 12; month++) {
                YearMonth ym = YearMonth.of(2024, month);
                String xml = calendar(location, 2024, month, 1, ym.lengthOfMonth(), "");
                String where = location + " " + ym;
                assertTrue(xml.getBytes(StandardCharsets.UTF_8).length < XML_OUT_SIZE, where);

                List<Day> days = days(xml);
                assertEquals(ym.lengthOfMonth(), days.size(), where);
                for (int i = 0; i < days.size(); i++) {
                    Day d = days.get(i);
                    String at = location + " " + d.date;
                    assertEquals(ym.atDay(i + 1), d.date, at);
                    assertEquals(d.date.getDayOfWeek().getValue() % 7, d.weekday, at);
                    assertTrue(d.tithi >= 1 && d.tithi <= 30, at);
                    assertEquals(d.sunrise.minusMinutes(96), d.arunodaya, at);
                    assertTrue(d.sunrise.isBefore(d.noon) && d.noon.isBefore(d.sunset), at);
                    assertTrue(d.dst == 0 || d.dst == 1, at);
                    assertNotNull(d.masa, at);
                    if (i > 0) {
                        // the same tithi at two sunrises (vriddhi), the next one or one skipped (ksaya)
                        int step = (d.tithi - days.get(i - 1).tithi + 30) % 30;
                        assertTrue(step <= 2, at + " tithi step " + step);
                    }
                }
            }
        }
    }

    @Test
    void testTithiAtSunriseMatchesTithiQuery() {
        // q=tithi: Navami (id 23, Gaura) from 14 Jun 20:35:55 to 15 Jun 23:04:30;
        // in q=calendar the index of a Gaura tithi is id + 1
        Day d = days(calendar(KYIV, 2024, 6, 15, 1, "")).get(0);
        assertEquals(24, d.tithi);
    }

    @Test
    void testGaurabdaYearStartsOnFirstDay() {
        // q=firstday for 2024 is 26 Mar 2024, the first day of Visnu masa of 538
        List<Day> days = days(calendar(KYIV, 2024, 3, 24, 4, ""));
        Day before = dayOf(days, LocalDate.of(2024, 3, 25));
        Day first = dayOf(days, LocalDate.of(2024, 3, 26));
        assertEquals(537, before.gyear);
        assertEquals("Govinda Masa", before.masa);
        assertEquals(538, first.gyear);
        assertEquals("Visnu Masa", first.masa);
    }

    @Test
    void testDstKyiv() {
        // last Sunday of March and of October
        List<Day> march = days(calendar(KYIV, 2024, 3, 30, 2, ""));
        assertEquals(0, march.get(0).dst);
        assertEquals(1, march.get(1).dst);
        assertEquals(LocalTime.of(5, 37, 56), march.get(0).sunrise);
        assertEquals(LocalTime.of(6, 35, 44), march.get(1).sunrise);

        List<Day> october = days(calendar(KYIV, 2024, 10, 26, 2, ""));
        assertEquals(1, october.get(0).dst);
        assertEquals(0, october.get(1).dst);
    }

    @Test
    void testDstSydney() {
        // the southern hemisphere: from the first Sunday of October to the first Sunday of April
        assertEquals(1, days(calendar(SYDNEY, 2024, 1, 10, 1, "")).get(0).dst);
        assertEquals(0, days(calendar(SYDNEY, 2024, 7, 10, 1, "")).get(0).dst);

        List<Day> april = days(calendar(SYDNEY, 2024, 4, 6, 2, ""));
        assertEquals(1, april.get(0).dst);
        assertEquals(0, april.get(1).dst);

        List<Day> october = days(calendar(SYDNEY, 2024, 10, 5, 2, ""));
        assertEquals(0, october.get(0).dst);
        assertEquals(1, october.get(1).dst);
    }

    @Test
    void testNoDstArgumentMeansNoDst() {
        String location = "&lc=Kyiv&la=50.4501&lo=30.5234&lt=2E00";
        String none = calendar(location, 2024, 7, 1, 31, "");
        for (Day d : days(none)) assertEquals(0, d.dst, d.date.toString());
        assertEquals(result(calendar(location + "&dst=0x0x0x0x0x0x0x0", 2024, 7, 1, 31, "")), result(none));
    }

    /**
     * Rules of type 1 ("from the n-th day of the month") do not work: gcal.cpp packs the
     * &amp;dst= fields with the widths a1:6 a2:2 a5:6 a6:2, ExpandVal in ttimezone.cpp unpacks
     * them as a1:2 a2:6 a5:2 a6:6. Type 0 rules ("the n-th weekday") survive by chance; this
     * one switches on 31 Mar and 29 Sep (the last Sundays) instead of 22 Mar and 22 Sep.
     */
    @Test
    @Disabled("defect of gcal.cpp: dst rules of type 1 are unpacked with other bit widths")
    void testDstFromDayOfMonth() {
        String location = "&lc=X&la=50.4501&lo=30.5234&lt=2E00&dst=3x1x22x0x9x1x22x0";
        List<Day> march = days(calendar(location, 2024, 3, 21, 2, ""));
        assertEquals(0, march.get(0).dst);
        assertEquals(1, march.get(1).dst);

        List<Day> september = days(calendar(location, 2024, 9, 21, 2, ""));
        assertEquals(1, september.get(0).dst);
        assertEquals(0, september.get(1).dst);
    }
}
