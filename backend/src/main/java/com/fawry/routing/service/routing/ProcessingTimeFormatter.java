package com.fawry.routing.service.routing;

public final class ProcessingTimeFormatter {

    private ProcessingTimeFormatter() {}

    public static String format(int minutes) {
        if (minutes == 0) {
            return "Instant";
        }
        if (minutes < 60) {
            return minutes + " Minutes";
        }
        if (minutes % (24 * 60) == 0) {
            int days = minutes / (24 * 60);
            return days == 1 ? "24 Hours" : (days * 24) + " Hours";
        }
        if (minutes % 60 == 0) {
            int hours = minutes / 60;
            return hours == 1 ? "1 Hour" : hours + " Hours";
        }
        return minutes + " Minutes";
    }
}
