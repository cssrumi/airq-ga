package pl.airq.ga.infrastructure.jenetics;

import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStart;
import io.jenetics.util.Factory;
import io.jenetics.util.ISeq;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.ga.config.AirqGaProperties;
import pl.airq.ga.domain.evolution.EvolutionService;
import pl.airq.ga.domain.evolution.ProblemEvaluation;
import pl.airq.ga.domain.phenotype.AirqPhenotypeMapper;
import pl.airq.ga.domain.training.TrainingData;

@ApplicationScoped
class JeneticsEvolutionService implements EvolutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JeneticsEvolutionService.class);

    private final Integer min;
    private final Integer max;
    private final Integer maximalPhenotypeAge;
    private final Long generations;
    private final Integer populationSize;
    private final ProblemEvaluation<Genotype<DoubleGene>> problemEvaluation;
    private final AirqPhenotypeMapper<Phenotype<DoubleGene, Double>> mapper;

    @Inject
    JeneticsEvolutionService(AirqGaProperties properties,
                             ProblemEvaluation<Genotype<DoubleGene>> problemEvaluation,
                             AirqPhenotypeMapper<Phenotype<DoubleGene, Double>> mapper) {
        this.min = properties.getPhenotype().getGenotype().getGene().getMin();
        this.max = properties.getPhenotype().getGenotype().getGene().getMax();
        this.maximalPhenotypeAge = properties.getPhenotype().getMaximalAge();
        this.generations = properties.getEvolution().getGenerations();
        this.populationSize = properties.getEvolution().getPopulationSize();
        this.problemEvaluation = problemEvaluation;
        this.mapper = mapper;
    }

    @Override
    public AirqPhenotype compute(TrainingData trainingData, Set<AirqPhenotype> phenotypes) {
        LOGGER.info("Computing phenotype for station: {}. TrainingData size: {}", trainingData.stationId, trainingData.size());
        final Factory<Genotype<DoubleGene>> genotypeFactory = generateFactory(trainingData.rowSize);
        final Engine<DoubleGene, Double> engine = Engine
                .builder(genotype -> problemEvaluation.evaluate(genotype, trainingData), genotypeFactory)
                .populationSize(populationSize)
                .maximalPhenotypeAge(maximalPhenotypeAge)
                .minimizing()
                .build();

        final Phenotype<DoubleGene, Double> best = engine.stream(initPopulation(phenotypes))
                                                         .limit(generations)
                                                         .collect(EvolutionResult.toBestEvolutionResult())
                                                         .bestPhenotype();

        final AirqPhenotype mappedBest = mapper.from(best, trainingData);
        LOGGER.info("Best phenotype found: {}", mappedBest);

        return mappedBest;
    }

    private Factory<Genotype<DoubleGene>> generateFactory(int size) {
        final List<DoubleGene> genes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            genes.add(DoubleGene.of(min, max));
        }

        return Genotype.of(DoubleChromosome.of(genes));
    }

    private EvolutionStart<DoubleGene, Double> initPopulation(Set<AirqPhenotype> phenotypes) {
        if (phenotypes.isEmpty()) {
            LOGGER.info("Initial population is empty.");
            return EvolutionStart.empty();
        }

        LOGGER.info("Initial population created!");
        return EvolutionStart.of(phenotypes.stream()
                                           .limit(5)
                                           .map(phenotype -> mapper.to(phenotype, min, max))
                                           .collect(ISeq.toISeq()), 1);
    }
}
