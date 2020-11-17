package pl.airq.ga.domain.phenotype.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.process.event.AppEvent;

import static pl.airq.ga.process.TopicConstant.PHENOTYPE_CREATED;

@RegisterForReflection
public class PhenotypeCreated extends AppEvent<PhenotypeCreatedPayload> {

    public PhenotypeCreated(PhenotypeCreatedPayload payload) {
        super(payload);
    }

    @Override
    public String defaultTopic() {
        return PHENOTYPE_CREATED;
    }

    @Override
    public String toString() {
        return "PhenotypeCreated{" +
                "timestamp=" + timestamp +
                ", payload=" + payload +
                '}';
    }
}
