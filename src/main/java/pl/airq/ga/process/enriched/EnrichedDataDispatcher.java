package pl.airq.ga.process.enriched;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.ctx.enriched.EnrichedDataCreatedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataDeletedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.ctx.enriched.EnrichedDataUpdatedEvent;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.ga.domain.evolution.EvolutionProcessor;

@ApplicationScoped
public class EnrichedDataDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedDataDispatcher.class);

    private final EvolutionProcessor evolutionProcessor;
    private final Map<String, EnrichedDataHandler> dispatchMap;

    @Inject
    public EnrichedDataDispatcher(EvolutionProcessor evolutionProcessor) {
        this.evolutionProcessor = evolutionProcessor;
        this.dispatchMap = Map.of(
                EnrichedDataCreatedEvent.class.getSimpleName(), this::evolutionHandler,
                EnrichedDataUpdatedEvent.class.getSimpleName(), this::evolutionHandler,
                EnrichedDataDeletedEvent.class.getSimpleName(), this::defaultHandler
        );
    }

    public void dispatch(AirqEvent<EnrichedDataEventPayload> airqEvent) {
        dispatchMap.getOrDefault(airqEvent.eventType(), this::defaultHandler)
                   .handle(airqEvent);
    }

    private void evolutionHandler(AirqEvent<EnrichedDataEventPayload> airqEvent) {
        evolutionProcessor.process(airqEvent.payload.enrichedData.station);
    }

    private void defaultHandler(AirqEvent<EnrichedDataEventPayload> airqEvent) {
        LOGGER.warn("Unhandled event: {}", airqEvent);
    }

    interface EnrichedDataHandler {
        void handle(AirqEvent<EnrichedDataEventPayload> airqEvent);
    }
}
