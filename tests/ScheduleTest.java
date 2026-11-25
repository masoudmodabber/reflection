package com.personal.reflection;
import java.time.*;
public final class ScheduleTest {
    private static int passed=0;
    private static void check(Object actual,Object expected) {
        if(!actual.equals(expected))throw new AssertionError("Expected "+expected+", got "+actual);
        passed++;
    }
    public static void main(String[] args) {
        LocalDate t=LocalDate.parse("2026-11-24");
        check(Schedule.nextOnOrAfter("daily",t),t);
        check(Schedule.nextOnOrAfter("weekly",t),LocalDate.parse("2026-11-29"));
        check(Schedule.nextOnOrAfter("monthly",t),LocalDate.parse("2026-11-29"));
        check(Schedule.isDue("weekly",t),false);
        check(Schedule.isDue("monthly",t),false);
        LocalDate sun=LocalDate.parse("2026-11-29");
        check(Schedule.isDue("weekly",sun),true);
        check(Schedule.isDue("monthly",sun),true);
        check(Schedule.nextOnOrAfter("monthly",sun.plusDays(1)),LocalDate.parse("2026-12-27"));
        check(Schedule.previousBefore("monthly",sun),LocalDate.parse("2026-10-25"));
        check(Schedule.previousBefore("monthly",sun.plusDays(1)),sun);
        check(Schedule.previousBefore("weekly",sun),LocalDate.parse("2026-11-22"));
        check(Schedule.previousBefore("daily",t),t.minusDays(1));
        check(Schedule.lastSunday(YearMonth.of(2024,2)),LocalDate.parse("2024-02-25"));
        check(Schedule.lastSunday(YearMonth.of(2025,8)),LocalDate.parse("2025-08-31"));
        check(Schedule.nextOnOrAfter("monthly",LocalDate.parse("2026-12-31")),LocalDate.parse("2027-01-31"));
        for(int year=2024;year<=2030;year++) for(int month=1;month<=12;month++) {
            YearMonth ym=YearMonth.of(year,month); LocalDate last=Schedule.lastSunday(ym);
            check(last.getDayOfWeek(),DayOfWeek.SUNDAY);
            check(YearMonth.from(last),ym);
            check(last.plusWeeks(1).isAfter(ym.atEndOfMonth()),true);
            check(Schedule.isDue("monthly",last),true);
            check(Schedule.previousBefore("monthly",last.plusDays(1)),last);
            check(Schedule.nextOnOrAfter("monthly",last),last);
        }
        ZoneId brisbane=ZoneId.of("Australia/Brisbane"),sydney=ZoneId.of("Australia/Sydney");
        check(Instant.ofEpochMilli(Schedule.dueMillis(t,brisbane)),Instant.parse("2026-11-24T11:00:00Z"));
        check(Instant.ofEpochMilli(Schedule.dueMillis(t,sydney)),Instant.parse("2026-11-24T10:00:00Z"));
        check(Instant.ofEpochMilli(Schedule.dueMillis(LocalDate.parse("2026-07-01"),sydney)),Instant.parse("2026-07-01T11:00:00Z"));
        System.out.println("PASS: "+passed+" calendar assertions");
    }
}
