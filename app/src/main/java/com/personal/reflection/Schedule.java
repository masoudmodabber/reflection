package com.personal.reflection;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

/** Due dates are calendar dates in the device's current timezone. */
public final class Schedule {
    private Schedule() {}
    public static final LocalTime DUE_TIME = LocalTime.of(21, 0);
    public static final String[] CADENCES = {"daily", "weekly", "monthly"};
    public static String label(String cadence) {
        switch (cadence) {
            case "daily": return "Daily";
            case "weekly": return "Weekly";
            case "monthly": return "Monthly";
            default: throw new IllegalArgumentException("Unknown cadence: " + cadence);
        }
    }
    public static LocalDate lastSunday(YearMonth month) {
        return month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }
    public static boolean isDue(String cadence, LocalDate day) {
        switch (cadence) {
            case "daily": return true;
            case "weekly": return day.getDayOfWeek() == DayOfWeek.SUNDAY;
            case "monthly": return day.equals(lastSunday(YearMonth.from(day)));
            default: throw new IllegalArgumentException("Unknown cadence: " + cadence);
        }
    }
    public static LocalDate nextOnOrAfter(String cadence, LocalDate day) {
        switch (cadence) {
            case "daily": return day;
            case "weekly": return day.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case "monthly": {
                YearMonth month = YearMonth.from(day);
                LocalDate due = lastSunday(month);
                return due.isBefore(day) ? lastSunday(month.plusMonths(1)) : due;
            }
            default: throw new IllegalArgumentException("Unknown cadence: " + cadence);
        }
    }
    public static LocalDate previousBefore(String cadence, LocalDate day) {
        LocalDate previous = day.minusDays(1);
        switch (cadence) {
            case "daily": return previous;
            case "weekly": return previous.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            case "monthly": {
                YearMonth month = YearMonth.from(previous);
                LocalDate due = lastSunday(month);
                return due.isAfter(previous) ? lastSunday(month.minusMonths(1)) : due;
            }
            default: throw new IllegalArgumentException("Unknown cadence: " + cadence);
        }
    }
    public static long dueMillis(LocalDate day, ZoneId zone) {
        return day.atTime(DUE_TIME).atZone(zone).toInstant().toEpochMilli();
    }
}
