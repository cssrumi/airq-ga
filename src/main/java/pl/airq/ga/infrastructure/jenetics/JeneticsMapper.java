package pl.airq.ga.infrastructure.jenetics;

import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.ga.domain.phenotype.AirqPhenotypeMapper;
import pl.airq.ga.domain.training.TrainingData;

@ApplicationScoped
class JeneticsMapper implements AirqPhenotypeMapper<Phenotype<DoubleGene, Double>> {

    public AirqPhenotype from(Phenotype<DoubleGene, Double> phenotype, TrainingData trainingData) {
        final List<Float> values = phenotype.genotype()
                                            .chromosome()
                                            .stream()
                                            .map(DoubleGene::floatValue)
                                            .collect(Collectors.toList());

        return new AirqPhenotype(
                OffsetDateTime.now(),
                trainingData.stationId,
                trainingData.fields,
                values,
                trainingData.predictionConfig,
                phenotype.fitness()
        );
    }

    @Override
    public Phenotype<DoubleGene, Double> to(AirqPhenotype airqPhenotype, Integer minValue, Integer maxValue) {
        final List<DoubleGene> genes = airqPhenotype.values.stream()
                                                           .map(value -> DoubleGene.of(value, minValue, maxValue))
                                                           .collect(Collectors.toUnmodifiableList());

        return Phenotype.of(Genotype.of(DoubleChromosome.of(genes)), 1);
    }
}
