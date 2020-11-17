package pl.airq.ga.domain.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.impl.Reflections;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import org.apache.commons.lang3.ClassUtils;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.enriched.EnrichedDataCreatedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataDeletedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataUpdatedEvent;
import pl.airq.common.process.ctx.gios.aggragation.GiosMeasurementCreatedEvent;
import pl.airq.common.process.ctx.gios.aggragation.GiosMeasurementDeletedEvent;
import pl.airq.common.process.ctx.gios.aggragation.GiosMeasurementUpdatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;

@Dependent
class EventParserProducer {

    @Produces
    @Singleton
    EventParser eventParser(ObjectMapper objectMapper) {
        final EventParser eventParser = new EventParser(objectMapper);
        eventParser.registerEvents(Set.of(
                AirqPhenotypeCreatedEvent.class,
                EnrichedDataCreatedEvent.class,
                EnrichedDataUpdatedEvent.class,
                EnrichedDataDeletedEvent.class
        ));
        return eventParser;
    }

}
