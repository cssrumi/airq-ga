package pl.airq.ga.process.enriched;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.ga.process.TopicConstant;

@Singleton
public class EnrichedDataConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedDataConsumer.class);
    private final EnrichedDataDispatcher dispatcher;
    private final EventParser parser;

    @Inject
    public EnrichedDataConsumer(EnrichedDataDispatcher dispatcher, EventParser parser) {
        this.dispatcher = dispatcher;
        this.parser = parser;
    }

    @SuppressWarnings("unchecked")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
    @Incoming(TopicConstant.DATA_ENRICHED_EXTERNAL_TOPIC)
    public void process(String raw) {
        LOGGER.info("Raw event consumed: {}", raw);
        AirqEvent<EnrichedDataEventPayload> airqEvent = parser.deserializeDomainEvent(raw);
        dispatcher.dispatch(airqEvent);
    }
}
