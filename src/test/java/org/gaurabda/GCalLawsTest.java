/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2026-11
 */
package org.gaurabda;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.gaurabda.GCalCalendarTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * What must hold for any date and any place, where the neighbouring tests look closely at one year:
 * the calendar is continuous, the Sun moves as it should, and the library's answers to arguments
 * outside the expected range are pinned, including the ones that end the process.
 */
public class GCalLawsTest extends AGaurabdaTest {
    private static final int[] YEARS = {1900, 1999, 2000, 2023, 2100};

    private static String place(String la, String lo, String lt) {
        return "&lc=X&la=" + la + "&lo=" + lo + "&lt=" + lt;
    }

    private static final Map<String, List<Day>> YEARS_ASKED = new HashMap<>();

    /** Remembered, so the sweeps below share one pass through the native library. */
    private static List<Day> yearOf(String location, int year) {
        return YEARS_ASKED.computeIfAbsent(location + year, key -> {
            List<Day> all = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                all.addAll(days(calendar(location, year, month, 1, YearMonth.of(year, month).lengthOfMonth(), "")));
            }
            return all;
        });
    }

    private static Day dayAt(String location, int year, int month, int day) {
        return days(calendar(location, year, month, day, 1, "")).get(0);
    }

    private static long minutes(LocalTime from, LocalTime to) {
        return Duration.between(from, to).toMinutes();
    }

    @Test
    void daysFollowEachOtherAcrossYearsAndWeekdaysAgreeWithTheCalendar() {
        for (String location : LOCATIONS) {
            for (int year : YEARS) {
                List<Day> days = yearOf(location, year);
                assertEquals(LocalDate.of(year, 12, 31).getDayOfYear(), days.size(), location + year);

                LocalDate expected = LocalDate.of(year, 1, 1);
                for (Day day : days) {
                    assertEquals(expected, day.date, location);
                    assertEquals(expected.getDayOfWeek().getValue() % 7, day.weekday, expected + location);
                    expected = expected.plusDays(1);
                }
            }
        }
    }

    @Test
    void theDayRunsFromArunodayaThroughNoonToSunset() {
        for (String location : LOCATIONS) {
            for (int year : YEARS) {
                for (Day day : yearOf(location, year)) {
                    String at = day.date + location;
                    assertEquals(96, minutes(day.arunodaya, day.sunrise), at);
                    assertTrue(day.sunrise.isBefore(day.noon), at);
                    assertTrue(day.noon.isBefore(day.sunset), at);
                }
            }
        }
    }

    /** A tithi can repeat (vriddhi) or be skipped (ksaya), but no day moves further than that. */
    @Test
    void theTithiAtSunriseStepsZeroOneOrTwo() {
        int repeated = 0, skipped = 0;
        for (String location : LOCATIONS) {
            for (int year : YEARS) {
                Day previous = null;
                for (Day day : yearOf(location, year)) {
                    assertTrue(day.tithi >= 1 && day.tithi <= 30, day.date + location + day.tithi);
                    if (null != previous) {
                        int step = (day.tithi - previous.tithi + 30) % 30;
                        assertTrue(step <= 2, day.date + location + " step " + step);
                        if (0 == step) repeated++;
                        if (2 == step) skipped++;
                    }
                    previous = day;
                }
            }
        }
        assertTrue(repeated > 0 && skipped > 0, repeated + " repeated, " + skipped + " skipped");
    }

    @Test
    void theGaurabdaYearKeepsItsOffsetAndNeverRunsBack() {
        for (String location : LOCATIONS) {
            for (int year : YEARS) {
                int previous = Integer.MIN_VALUE;
                for (Day day : yearOf(location, year)) {
                    int offset = year - day.gyear;
                    assertTrue(1486 == offset || 1487 == offset, day.date + " is " + day.gyear);
                    assertTrue(day.gyear >= previous, day.date.toString());
                    previous = day.gyear;
                }
            }
        }
    }

    @Test
    void aDayDoesNotDependOnTheQueryThatAskedForIt() {
        for (String location : LOCATIONS) {
            List<Day> across = days(calendar(location, 2023, 12, 20, 25, ""));
            LocalDate date = LocalDate.of(2023, 12, 20);

            for (Day day : across) {
                Day alone = dayAt(location, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
                assertEquals(date, day.date);
                assertEquals(alone.sunrise, day.sunrise, date + location);
                assertEquals(alone.tithi, day.tithi, date + location);
                assertEquals(alone.masa, day.masa, date + location);
                assertEquals(alone.gyear, day.gyear, date + location);
                date = date.plusDays(1);
            }
        }
    }

    @Test
    void theSameQueryGivesTheSameAnswer() {
        assertEquals(result(calendar(KYIV, 2024, 3, 1, 31, "")), result(calendar(KYIV, 2024, 3, 1, 31, "")));
    }

    @Test
    void movingTheTimeZoneOnlyMovesTheClock() {
        Day two = dayAt(place("50N27", "30E31", "2E00"), 2024, 3, 15);
        Day five = dayAt(place("50N27", "30E31", "5E00"), 2024, 3, 15);

        assertEquals(two.tithi, five.tithi);
        assertEquals(180, minutes(two.sunrise, five.sunrise), 1);
        assertEquals(180, minutes(two.sunset, five.sunset), 1);
    }

    @Test
    void fifteenDegreesEastIsAnHourEarlier() {
        Day west = dayAt(place("50N00", "15E00", "0E00"), 2024, 3, 15);
        Day east = dayAt(place("50N00", "30E00", "0E00"), 2024, 3, 15);

        assertEquals(60, minutes(east.sunrise, west.sunrise), 2);
        assertEquals(60, minutes(east.sunset, west.sunset), 2);
    }

    @Test
    void summerDaysAreLongOnEachSideOfTheEquator() {
        Day kyivJune = dayAt(KYIV, 2024, 6, 21), kyivDecember = dayAt(KYIV, 2024, 12, 21);
        Day sydneyJune = dayAt(SYDNEY, 2024, 6, 21), sydneyDecember = dayAt(SYDNEY, 2024, 12, 21);

        assertTrue(minutes(kyivJune.sunrise, kyivJune.sunset) > minutes(kyivDecember.sunrise, kyivDecember.sunset) + 300);
        assertTrue(minutes(sydneyDecember.sunrise, sydneyDecember.sunset) > minutes(sydneyJune.sunrise, sydneyJune.sunset) + 180);
    }

    @Test
    void atTheEquatorTheDayIsTwelveHoursAllYear() {
        for (Day day : yearOf(place("0N00", "0E00", "0E00"), 2024)) {
            assertEquals(725, minutes(day.sunrise, day.sunset), 10, day.date.toString());
        }
    }

    /** Where the Sun neither rises nor sets the time is midnight UTC, the same for polar day and night. */
    @Test
    void withoutASunriseTheClockShowsMidnightUtc() {
        String tromso = place("69N39", "18E57", "1E00");

        assertEquals(LocalTime.of(1, 0), dayAt(tromso, 2024, 6, 21).sunrise, "polar day");
        assertEquals(LocalTime.of(1, 0), dayAt(tromso, 2024, 12, 21).sunrise, "polar night");
        assertEquals(LocalTime.MIDNIGHT, dayAt(place("90N00", "0E00", "0E00"), 2024, 6, 21).sunrise);
        assertEquals(LocalTime.MIDNIGHT, dayAt(place("90S00", "0E00", "0E00"), 2024, 6, 21).sunrise);
    }

    @Test
    void theEdgesOfTheCenturiesAreOrdinaryDays() {
        assertEquals(LocalDate.of(1599, 1, 2), days(calendar(KYIV, 1599, 1, 1, 2, "")).get(1).date);

        List<Day> turn = days(calendar(KYIV, 2399, 12, 30, 4, ""));
        assertEquals(LocalDate.of(2400, 1, 2), turn.get(3).date);
    }

    /**
     * DEFECT: with no days to show the header of the first month is never written, while its
     * closing tag is, so the XML does not parse.
     */
    @Test
    void noDaysCloseAMonthThatWasNeverOpened() {
        for (int count : new int[]{0, -5}) {
            String answer = result(calendar(KYIV, 2024, 1, 1, count, ""));

            assertTrue(days(answer).isEmpty(), "tc=" + count);
            assertTrue(answer.contains("</masa>"), answer);
            assertFalse(answer.contains("<masa"), answer);
        }
    }

    @Test
    void aMonthOutOfRangeIsNotRefused() {
        assertEquals(LocalDate.of(2025, 1, 1), dayAt(KYIV, 2024, 13, 1).date, "13 rolls over");
        assertEquals(LocalDate.of(2024, 1, 1), dayAt(KYIV, 2024, 0, 1).date, "0 is the first month");
    }

    @Test
    void aDayOutOfRangeIsNotRefused() {
        assertEquals(LocalDate.of(2024, 3, 2), dayAt(KYIV, 2024, 2, 31).date, "rolls over");
        assertEquals(LocalDate.of(2024, 2, 1), dayAt(KYIV, 2024, 2, 0).date, "0 is the first day");
    }

    @Test
    void aYearThatIsNotANumberIsYearZero() {
        String words = result(GCalManager.nativeGCal4Query("q=calendar" + KYIV + "&ty=abc&tm=1&td=1&tc=2"));
        String zero = GCalManager.nativeGCal4Query("q=calendar" + KYIV + "&ty=0&tm=1&td=1&tc=2");

        assertEquals("1 Jan 0", attr(words, "<day date=\"([^\"]+)\""));
        assertEquals(result(zero), words);
    }

    @Test
    void withoutADateItIsToday() {
        LocalDate before = LocalDate.now();
        String xml = GCalManager.nativeGCal4Query("q=calendar" + KYIV);
        LocalDate after = LocalDate.now();

        List<Day> days = days(xml);
        assertEquals(1, days.size());
        assertFalse(days.get(0).date.isBefore(before) || days.get(0).date.isAfter(after), xml);
    }

    @Test
    void emptyPlaceArgumentsMeanTheEquatorAtGreenwich() {
        String empty = GCalManager.nativeGCal4Query("q=calendar&lc=&la=&lo=&lt=&dst=&ty=2024&tm=1&td=1&tc=1");
        String greenwich = calendar(place("0N00", "0E00", "0E00"), 2024, 1, 1, 1, "");

        assertEquals(days(greenwich).get(0).sunrise, days(empty).get(0).sunrise);
        assertEquals(days(greenwich).get(0).tithi, days(empty).get(0).tithi);
    }

    @Test
    void coordinatesOutOfRangeAreNotRefused() {
        assertEquals(1, days(calendar("&lc=X&la=91.0&lo=30&lt=2E00", 2024, 1, 1, 1, "")).size());
        assertEquals(1, days(calendar("&lc=X&la=50&lo=181&lt=2E00", 2024, 1, 1, 1, "")).size());
        assertEquals(1, days(calendar("&lc=X&la=50&lo=30&lt=14E00", 2024, 1, 1, 1, "")).size());
    }

    @Test
    void unknownArgumentsAreIgnored() {
        assertEquals(result(calendar(KYIV, 2024, 1, 1, 2, "")), result(calendar(KYIV, 2024, 1, 1, 2, "&unknown=1")));
    }

    @Test
    void theLastOfARepeatedArgumentWins() {
        String xml = GCalManager.nativeGCal4Query("q=calendar&q=tithi" + KYIV + "&ty=2024&tm=1&td=1&tc=2");

        assertTrue(result(xml).startsWith("<result name=\"Tithi\""), xml);
    }

    @Test
    void aPlaceNameInAnotherAlphabetChangesNothing() {
        assertEquals(result(calendar(KYIV, 2024, 1, 1, 1, "")),
            result(calendar(KYIV, 2024, 1, 1, 1, "&lc=Київ")));
    }

    @Test
    void fourMonthsFitTheBuffer() {
        String xml = calendar(KYIV, 2024, 1, 1, 120, "");

        assertEquals(120, days(xml).size());
        assertTrue(xml.getBytes(StandardCharsets.UTF_8).length < XML_OUT_SIZE);
    }

    // ------------------------------------------------------- what ends the process

    /** Asks on its own JVM: these queries cannot be asked here, the test run would die with them. */
    public static final class OwnProcess {
        public static void main(String[] args) {
            String query = null;
            if ("days".equals(args[0])) {
                query = "q=calendar" + KYIV + "&ty=2024&tm=1&td=1&tc=" + args[1];
            } else if ("letters".equals(args[0])) {
                char[] letters = new char[Integer.parseInt(args[1])];
                Arrays.fill(letters, 'x');
                query = new String(letters);
            }
            GCalManager.nativeGCal4Query(query);
            System.out.println("survived");
        }
    }

    private static boolean survives(String... args) throws IOException, InterruptedException {
        File basedir = new File(System.getProperty("basedir", System.getProperty("user.dir")));
        String classpath = new File(basedir, "target/classes") + File.pathSeparator
            + new File(basedir, "target/test-classes");

        List<String> command = new ArrayList<>(Arrays.asList(
            new File(System.getProperty("java.home"), "bin/java").getPath(),
            "-XX:+SuppressFatalErrorMessage", "-cp", classpath,
            "-Djava.library.path=" + new File(basedir, "libs"),
            OwnProcess.class.getName()));
        Collections.addAll(command, args);

        File output = new File(basedir, "target/own-process.txt");
        Process process = new ProcessBuilder(command).directory(basedir).redirectErrorStream(true)
            .redirectOutput(output).start();
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "still running: " + command);

        String shown = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        boolean survived = shown.contains("survived");
        assertEquals(survived, 0 == process.exitValue(), shown);
        return survived;
    }

    @Test
    void fourMonthsInOneQueryLeaveTheProcessAlive() throws Exception {
        assertTrue(survives("days", "120"));
    }

    /** DEFECT: past the 64 KB of the result buffer the native code writes on, and the JVM dies. */
    @Test
    void aYearOfDaysInOneQueryEndsTheProcess() throws Exception {
        assertFalse(survives("days", "366"));
    }

    @Test
    void aQueryJustBelowTheLimitIsRefusedQuietly() throws Exception {
        assertTrue(survives("letters", "1023"));
    }

    /** DEFECT: from 1024 characters on the process exits at once, without so much as a crash report. */
    @Test
    void aQueryOf1024CharactersEndsTheProcess() throws Exception {
        assertFalse(survives("letters", "1024"));
    }

    /** DEFECT: a null query is handed straight to the JNI layer, and the JVM dies. */
    @Test
    void aNullQueryEndsTheProcess() throws Exception {
        assertFalse(survives("null"));
    }
}
