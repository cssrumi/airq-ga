package pl.airq.ga.domain.evolution;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.PersistentRepository;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.phenotype.AirqPhenotypeQuery;
import pl.airq.common.domain.station.StationQuery;
import pl.airq.common.exception.ResourceNotFoundException;
import pl.airq.common.vo.StationId;
import pl.airq.ga.config.AirqGaProperties;
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
    private final StationQuery stationQuery;

    @Inject
    public EvolutionServiceFacade(AirqGaProperties properties,
                                  EvolutionService evolutionService,
                                  TrainingDataService trainingDataService,
                                  AirqPhenotypeQuery airqPhenotypeQuery,
                                  PersistentRepository<AirqPhenotype> repository,
                                  StationQuery stationQuery) {
        this.evolutionService = evolutionService;
        this.trainingDataService = trainingDataService;
        this.airqPhenotypeQuery = airqPhenotypeQuery;
        this.repository = repository;
        this.stationQuery = stationQuery;
        this.timeFrame = properties.getPrediction().duration();
    }

    public Optional<AirqPhenotype> generateNewPhenotype(StationId stationId) {
        if (stationQuery.findById(stationId).await().asOptional().atMost(Duration.ofSeconds(5)).isEmpty()) {
            LOGGER.info("Station: {} does not exist", stationId.value());
            return Optional.empty();
        }

        final TrainingData trainingData;
        try {
            trainingData = trainingDataService.createTrainingData(stationId, timeFrame);
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("Unable to create training data for: {}.", stationId.value(), e);
            return Optional.empty();
        }

        if (trainingData == null || trainingData.size() == 0) {
            LOGGER.warn("TrainingData is empty. Process stopped...");
            return Optional.empty();
        }

        LOGGER.info("{} created for Station: {}.", trainingData, stationId.value());
        Set<AirqPhenotype> basePhenotypes = basePhenotypes(stationId);

        final AirqPhenotype newPhenotype = evolutionService.compute(trainingData, basePhenotypes);
        LOGGER.info("New phenotype computed with fitness: {}", newPhenotype.fitness);

        repository.save(newPhenotype)
                  .await().atMost(Duration.ofSeconds(10));
        LOGGER.info("New phenotype has been saved");
        return Optional.of(newPhenotype);
    }

    private Set<AirqPhenotype> basePhenotypes(StationId stationId) {
        Set<AirqPhenotype> phenotypes = new HashSet<>();
        airqPhenotypeQuery.findBestByStationId(stationId)
                          .await().asOptional().atMost(Duration.ofSeconds(10))
                          .ifPresent(phenotypes::add);

        airqPhenotypeQuery.findLatestByStationId(stationId)
                          .await().asOptional().atMost(Duration.ofSeconds(10))
                          .ifPresent(phenotypes::add);

        if (!phenotypes.isEmpty()) {
            LOGGER.info("Base phenotypes: {}", phenotypes.size());
        } else {
            LOGGER.info("Base phenotypes not found.");
        }

        return phenotypes;
    }
}
