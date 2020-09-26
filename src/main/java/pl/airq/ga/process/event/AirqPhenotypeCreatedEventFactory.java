package pl.airq.ga.process.event;

import java.time.OffsetDateTime;
import pl.airq.common.domain.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.domain.phenotype.AirqPhenotypeCreatedPayload;

class AirqPhenotypeCreatedEventFactory {

    private AirqPhenotypeCreatedEventFactory() {
    }

    static AirqPhenotypeCreatedEvent from(PhenotypeCreated event) {
        final AirqPhenotypeCreatedPayload payload = new AirqPhenotypeCreatedPayload(event.payload.airqPhenotype);
        return new AirqPhenotypeCreatedEvent(OffsetDateTime.now(), payload);
    }
}
