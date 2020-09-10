package pl.airq.ga.domain.evolution;

import java.util.Set;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.ga.domain.training.TrainingData;

public interface EvolutionService {

     AirqPhenotype compute(TrainingData trainingData, Set<AirqPhenotype> phenotypes);

}
