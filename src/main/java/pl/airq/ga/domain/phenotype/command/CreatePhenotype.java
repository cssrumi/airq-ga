package pl.airq.ga.domain.phenotype.command;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.process.command.AppCommand;
import pl.airq.ga.domain.phenotype.PhenotypeCreationResult;

import static pl.airq.ga.process.TopicConstant.CREATE_PHENOTYPE;

@RegisterForReflection
public class CreatePhenotype extends AppCommand<CreatePhenotypePayload, PhenotypeCreationResult> {

    public CreatePhenotype(CreatePhenotypePayload payload) {
        super(payload);
    }

    @Override
    public Class<PhenotypeCreationResult> responseType() {
        return PhenotypeCreationResult.class;
    }

    @Override
    public String defaultTopic() {
        return CREATE_PHENOTYPE;
    }

    @Override
    public String toString() {
        return "CreatePhenotype{" +
                "timestamp=" + timestamp +
                ", payload=" + payload +
                '}';
    }
}
