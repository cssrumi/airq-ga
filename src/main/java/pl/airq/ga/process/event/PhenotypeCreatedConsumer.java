package pl.airq.ga.process.event;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.event.Consumer;
import pl.airq.ga.process.TopicConstant;

@Singleton
public class PhenotypeCreatedConsumer implements Consumer<PhenotypeCreated> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenotypeCreatedConsumer.class);
    private final Emitter<String> externalTopic;
    private final EventParser parser;

    @Inject
    public PhenotypeCreatedConsumer(@Channel(TopicConstant.PHENOTYPE_CREATED_EXTERNAL_TOPIC) Emitter<String> externalTopic,
                                    EventParser parser) {
        this.externalTopic = externalTopic;
        this.parser = parser;
    }

    @PostConstruct
    void afterInit() {
        LOGGER.info("{} created.", PhenotypeCreatedConsumer.class.getSimpleName());
    }

    @Override
    public Uni<Void> consume(PhenotypeCreated event) {
        LOGGER.info("Consume : {}", event);
        return Uni.createFrom()
                  .item(event)
                  .map(AirqPhenotypeCreatedEventFactory::from)
                  .map(parser::parse)
                  .flatMap(rawEvent -> Uni.createFrom().completionStage(externalTopic.send(rawEvent)))
                  .onItem()
                  .invoke(ignore -> LOGGER.info("{} has been passed to External Bus.", AirqPhenotypeCreatedEvent.class.getSimpleName()));
    }

    @ConsumeEvent(TopicConstant.PHENOTYPE_CREATED)
    Uni<Void> consumeEvent(PhenotypeCreated event) {
        return consume(event);
    }
}
