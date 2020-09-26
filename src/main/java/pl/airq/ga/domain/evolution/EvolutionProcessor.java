package pl.airq.ga.domain.evolution;

import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.Cancellable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.vo.StationId;
import pl.airq.ga.process.event.PhenotypeCreated;
import pl.airq.ga.process.event.PhenotypeCreatedPayload;

@Singleton
public class EvolutionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvolutionProcessor.class);
    private final UnicastProcessor<StationId> processor;
    private final EvolutionServiceFacade serviceFacade;
    private final AppEventBus eventBus;
    private final ExecutorService executor;
    private Cancellable subscription;

    @Inject
    public EvolutionProcessor(EvolutionServiceFacade serviceFacade, AppEventBus eventBus) {
        this.serviceFacade = serviceFacade;
        this.eventBus = eventBus;
        this.processor = UnicastProcessor.create();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    void startProcessing() {
        LOGGER.info("{} started!", EvolutionProcessor.class.getSimpleName());
        subscription = processor.emitOn(executor).subscribe().with(this::consume);
    }

    @PreDestroy
    void unsubscribe() {
        subscription.cancel();
    }

    public void process(StationId stationId) {
        processor.onNext(stationId);
    }

    private void consume(StationId stationId) {
        LOGGER.info("Processing started for: {}", stationId.getId());
        serviceFacade.generateNewPhenotype(stationId)
                     .map(PhenotypeCreatedPayload::new)
                     .map(PhenotypeCreated::new)
                     .ifPresent(eventBus::publish);
        LOGGER.info("Processing for {} completed.", stationId.getId());
    }
}
