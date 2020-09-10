package pl.airq.ga.domain.phenotype;

import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.ga.domain.training.TrainingData;

public interface AirqPhenotypeMapper<P> {

    AirqPhenotype from(P phenotype, TrainingData trainingData);

    P to(AirqPhenotype airqPhenotype, Integer minValue, Integer maxValue);
}
