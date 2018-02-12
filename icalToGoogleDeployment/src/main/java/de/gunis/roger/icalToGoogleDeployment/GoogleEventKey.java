package de.gunis.roger.icalToGoogleDeployment;

import com.google.api.services.calendar.model.EventDateTime;

import java.util.Objects;

class GoogleEventKey {
    private final EventDateTime start;
    private final EventDateTime end;
    private final String summary;

    private GoogleEventKey(EventDateTime start, EventDateTime end, String summary) {
        this.start = start;
        this.end = end;
        this.summary = summary;
    }

    public static GoogleEventKey of(EventDateTime start, EventDateTime end, String summary) {
        return new GoogleEventKey(start, end, summary);
    }

    @Override
    public String toString() {
        return "GoogleEventKey{" +
                "start=" + start +
                ", end=" + end +
                ", summary='" + summary + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleEventKey that = (GoogleEventKey) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end) &&
                Objects.equals(summary, that.summary);
    }

    @Override
    public int hashCode() {

        return Objects.hash(start, end, summary);
    }
}
