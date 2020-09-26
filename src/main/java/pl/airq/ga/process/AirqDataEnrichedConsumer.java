package pl.airq.ga.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.enriched.AirqDataEnrichedEvent;
import pl.airq.common.process.event.Consumer;
import pl.airq.ga.domain.evolution.EvolutionProcessor;

@Singleton
public class AirqDataEnrichedConsumer implements Consumer<AirqDataEnrichedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqDataEnrichedConsumer.class);
    private final EvolutionProcessor evolutionProcessor;
    private final ObjectMapper mapper;

    @Inject
    public AirqDataEnrichedConsumer(EvolutionProcessor evolutionProcessor, ObjectMapper mapper) {
        this.evolutionProcessor = evolutionProcessor;
        this.mapper = mapper;
    }

    @Override
    public Uni<Void> consume(AirqDataEnrichedEvent event) {
        return Uni.createFrom()
                  .item(event.payload.enrichedData.station)
                  .onItem()
                  .transform(stationId -> {
                      evolutionProcessor.process(stationId);
                      LOGGER.info("Station: {} added to EvolutionProcessor", stationId);
                      return null;
                  });
    }

    @Incoming(TopicConstant.DATA_ENRICHED_EXTERNAL_TOPIC)
    public void process(String raw) {
        LOGGER.info("Raw event consumed: {}", raw);
        try {
            final AirqDataEnrichedEvent event = mapper.readValue(raw, AirqDataEnrichedEvent.class);
            consume(event).await().indefinitely();
        } catch (JsonProcessingException e) {
            LOGGER.warn("Problem during deserialization of {} occurred", AirqDataEnrichedEvent.class.getSimpleName(), e);
        }
    }
}
