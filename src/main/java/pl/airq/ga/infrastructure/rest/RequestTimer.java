package pl.airq.ga.infrastructure.rest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.enterprise.context.RequestScoped;

@RequestScoped
class RequestTimer {

    private final LocalDateTime startedAt;

    RequestTimer() {
        this.startedAt = LocalDateTime.now();
    }

    LocalDateTime startedAt() {
        return startedAt;
    }

    long processTimeInMillis() {
        return startedAt.until(LocalDateTime.now(), ChronoUnit.MILLIS);
    }
}
