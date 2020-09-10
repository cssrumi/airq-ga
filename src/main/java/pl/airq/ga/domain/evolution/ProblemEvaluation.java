package pl.airq.ga.domain.evolution;

import pl.airq.ga.domain.training.TrainingData;

public interface ProblemEvaluation<G> {

    double evaluate(G genotype, TrainingData trainingData);
}
