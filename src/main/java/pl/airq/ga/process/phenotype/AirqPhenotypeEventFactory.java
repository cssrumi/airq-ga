package pl.airq.ga.process.phenotype;

import java.time.OffsetDateTime;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedPayload;
import pl.airq.ga.domain.phenotype.event.PhenotypeCreated;

class AirqPhenotypeEventFactory {

    private AirqPhenotypeEventFactory() {
    }

    static AirqPhenotypeCreatedEvent from(PhenotypeCreated event) {
        final AirqPhenotypeCreatedPayload payload = new AirqPhenotypeCreatedPayload(event.payload.airqPhenotype);
        return new AirqPhenotypeCreatedEvent(OffsetDateTime.now(), payload);
    }
}
