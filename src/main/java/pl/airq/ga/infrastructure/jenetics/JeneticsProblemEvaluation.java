package pl.airq.ga.infrastructure.jenetics;

import com.google.common.base.Preconditions;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.ga.domain.evolution.ProblemEvaluation;
import pl.airq.ga.domain.training.TrainingData;

@Singleton
class JeneticsProblemEvaluation implements ProblemEvaluation<Genotype<DoubleGene>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JeneticsProblemEvaluation.class);
    private static final String PRECONDITION_MESSAGE = "Row size %s and genotype gene count %s doesn't match";

    @Override
    public double evaluate(Genotype<DoubleGene> genotype, TrainingData trainingData) {
        Preconditions.checkArgument(genotype.geneCount() == trainingData.rowSize,
                String.format(PRECONDITION_MESSAGE, trainingData.rowSize, genotype.geneCount()));
        final int rowSize = trainingData.rowSize;
        return trainingData.stream()
                           .mapToDouble(row -> {
                               double result = 0;
                               for (int i = 0; i < rowSize; i++) {
                                   LOGGER.debug("{}, {}, {}", genotype.chromosome(), genotype.chromosome().length(), row.values);
                                   result += Math.abs(row.values[i] * genotype.chromosome().get(i).doubleValue());
                               }

                               return Math.abs(result - row.expectedValue);
                           })
                           .sum();
    }
}
