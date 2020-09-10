package pl.airq.ga.model.command;

import pl.airq.common.domain.process.command.AppCommand;
import pl.airq.ga.domain.phenotype.PhenotypeCreationResult;

import static pl.airq.ga.process.TopicConstant.CREATE_NEW_PHENOTYPE;

public class CreateNewPhenotype extends AppCommand<CreateNewPhenotypePayload, PhenotypeCreationResult> {

    public CreateNewPhenotype(CreateNewPhenotypePayload payload) {
        super(payload);
    }

    @Override
    public Class<PhenotypeCreationResult> responseType() {
        return PhenotypeCreationResult.class;
    }

    @Override
    public String defaultTopic() {
        return CREATE_NEW_PHENOTYPE;
    }
}
