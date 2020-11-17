package pl.airq.ga.process.phenotype;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.ga.domain.phenotype.event.PhenotypeCreated;
import pl.airq.ga.process.TopicConstant;

@Singleton
public class PhenotypeCreatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenotypeCreatedProcessor.class);
    private final AirqPhenotypePublisher publisher;

    @Inject
    public PhenotypeCreatedProcessor(AirqPhenotypePublisher publisher) {
        this.publisher = publisher;
    }

    @ConsumeEvent(TopicConstant.PHENOTYPE_CREATED)
    Uni<Void> consume(PhenotypeCreated event) {
        return Uni.createFrom().voidItem()
                  .invoke(() -> LOGGER.info("Consuming: {}", event))
                  .call(() -> publisher.created(event));
    }
}
