package pl.airq.ga.domain.evolution;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.PersistentRepository;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.phenotype.AirqPhenotypeQuery;
import pl.airq.common.vo.StationId;
import pl.airq.ga.domain.training.TrainingData;
import pl.airq.ga.domain.training.TrainingDataService;

@ApplicationScoped
public class EvolutionServiceFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvolutionServiceFacade.class);
    private final EvolutionService evolutionService;
    private final TrainingDataService trainingDataService;
    private final AirqPhenotypeQuery airqPhenotypeQuery;
    private final PersistentRepository<AirqPhenotype> repository;
    private final Duration timeFrame;

    @Inject
    public EvolutionServiceFacade(
            @ConfigProperty(name = "ga.prediction.timeFrame") Long timeFrame,
            @ConfigProperty(name = "ga.prediction.timeUnit") ChronoUnit timeUnit,
            EvolutionService evolutionService,
            TrainingDataService trainingDataService,
            AirqPhenotypeQuery airqPhenotypeQuery,
            PersistentRepository<AirqPhenotype> repository) {
        this.evolutionService = evolutionService;
        this.trainingDataService = trainingDataService;
        this.airqPhenotypeQuery = airqPhenotypeQuery;
        this.repository = repository;
        this.timeFrame = Duration.of(timeFrame, timeUnit);
    }

    public AirqPhenotype generateNewPhenotype(StationId stationId) {
        final TrainingData trainingData = trainingDataService.createTrainingData(stationId, timeFrame);
        LOGGER.info("{} created for Station: {}.", trainingData, stationId.getId());
        final Set<AirqPhenotype> phenotypes = airqPhenotypeQuery.findByStationId(stationId)
                                                                .await()
                                                                .asOptional()
                                                                .atMost(Duration.ofSeconds(10))
                                                                .orElse(Collections.emptySet());
        final Set<AirqPhenotype> best;
        if (!phenotypes.isEmpty()) {
            LOGGER.info("Airq phenotypes query result: {}", phenotypes);
            best = Collections.singleton(Collections.min(phenotypes, Comparator.comparing(phenotype -> phenotype.fitness)));
        } else {
            best = phenotypes;
        }

        final AirqPhenotype newPhenotype = evolutionService.compute(trainingData, best);
        LOGGER.info("New phenotype computed with fitness: {}", newPhenotype.fitness);

        if (best.stream().findFirst().map(old -> newPhenotype.fitness < old.fitness).orElse(Boolean.TRUE)) {
            repository.save(newPhenotype)
                      .await()
                      .indefinitely();
            LOGGER.info("New phenotype has been saved");
        }

        return newPhenotype;
    }
}
