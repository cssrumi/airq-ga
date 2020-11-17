package pl.airq.ga.domain.phenotype.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.process.Payload;

@RegisterForReflection
public class PhenotypeCreatedPayload implements Payload {

    public final AirqPhenotype airqPhenotype;

    public PhenotypeCreatedPayload(AirqPhenotype airqPhenotype) {
        this.airqPhenotype = airqPhenotype;
    }

    @Override
    public String toString() {
        return "PhenotypeCreatedPayload{" +
                "airqPhenotype=" + airqPhenotype +
                '}';
    }
}
