package com.fawry.routing.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityWindow {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Cairo");

    @Column(name = "available_24x7", nullable = false)
    private boolean alwaysAvailable;

    @Column(name = "available_days", length = 64)
    private String allowedDaysCsv;

    @Column(name = "available_from_hour")
    private Integer fromHour;

    @Column(name = "available_to_hour")
    private Integer toHour;

    public static AvailabilityWindow alwaysOn() {
        return new AvailabilityWindow(true, null, null, null);
    }

    public boolean isOpenAt(ZonedDateTime moment) {
        if (alwaysAvailable) {
            return true;
        }
        if (fromHour == null || toHour == null) {
            return false;
        }
        ZonedDateTime local = moment.withZoneSameInstant(BUSINESS_ZONE);
        if (!allowedDays().contains(local.getDayOfWeek())) {
            return false;
        }
        int hour = local.getHour();
        return hour >= fromHour && hour < toHour;
    }

    private Set<DayOfWeek> allowedDays() {
        if (allowedDaysCsv == null || allowedDaysCsv.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String token : allowedDaysCsv.split(",")) {
            days.add(parseDay(token.trim().toUpperCase()));
        }
        return days;
    }

    private static DayOfWeek parseDay(String token) {
        return switch (token) {
            case "SUN", "SUNDAY"    -> DayOfWeek.SUNDAY;
            case "MON", "MONDAY"    -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY"   -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY"  -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY"    -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY"  -> DayOfWeek.SATURDAY;
            default -> throw new IllegalArgumentException("Unknown day token: " + token);
        };
    }
}
