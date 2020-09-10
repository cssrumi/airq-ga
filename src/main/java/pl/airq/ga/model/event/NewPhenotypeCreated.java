package pl.airq.ga.model.event;

import pl.airq.common.domain.process.event.AppEvent;

import static pl.airq.ga.process.TopicConstant.NEW_PHENOTYPE_CREATED;

public class NewPhenotypeCreated extends AppEvent<NewPhenotypeCreatedPayload> {

    public NewPhenotypeCreated(NewPhenotypeCreatedPayload payload) {
        super(payload);
    }

    @Override
    public String defaultTopic() {
        return NEW_PHENOTYPE_CREATED;
    }
}
