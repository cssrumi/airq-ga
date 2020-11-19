package pl.airq.ga.process.phenotype;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedPayload;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.store.key.SKey;
import pl.airq.ga.domain.phenotype.event.PhenotypeCreated;
import pl.airq.ga.process.TopicConstant;

@ApplicationScoped
public class AirqPhenotypePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqPhenotypePublisher.class);
    private final Emitter<String> emitter;
    private final String topic;
    private final EventParser parser;

    @Inject
    public AirqPhenotypePublisher(@OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 100)
                                  @Channel(TopicConstant.PHENOTYPE_CREATED_EXTERNAL_TOPIC) Emitter<String> emitter,
                                  @ConfigProperty(name = "mp.messaging.outgoing.phenotype-created.topic") String topic,
                                  EventParser parser) {
        this.emitter = emitter;
        this.topic = topic;
        this.parser = parser;
    }

    public Uni<Void> created(PhenotypeCreated appEvent) {
        return publish(new SKey(appEvent.payload.airqPhenotype.stationId), AirqPhenotypeEventFactory.from(appEvent));
    }

    Uni<Void> publish(SKey key, AirqPhenotypeCreatedEvent event) {
        return Uni.createFrom().item(createMessage(key, event))
                  .invoke(() -> LOGGER.info("Publishing... {} - {}", key.value(), event.eventType()))
                  .onItem().invoke(emitter::send)
                  .invoke(() -> LOGGER.info("Published {} - {}", key.value(), event.eventType()))
                  .onItem().ignore().andContinueWithNull();
    }

    private Message<String> createMessage(SKey key, AirqEvent<AirqPhenotypeCreatedPayload> event) {
        OutgoingKafkaRecordMetadata<SKey> metadata = OutgoingKafkaRecordMetadata
                .<SKey>builder()
                .withTopic(topic)
                .withKey(key)
                .build();
        return Message.of(parser.parse(event))
                      .addMetadata(metadata);
    }
}
