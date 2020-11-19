package pl.airq.ga.config;

import io.quarkus.arc.config.ConfigProperties;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@ConfigProperties(prefix = "ga")
public class AirqGaProperties {

    @NotNull
    private PredictionProperties prediction;
    @NotNull
    private PhenotypeProperties phenotype;
    @NotNull
    private EvolutionProperties evolution;

    public PredictionProperties getPrediction() {
        return prediction;
    }

    public void setPrediction(PredictionProperties prediction) {
        this.prediction = prediction;
    }

    public PhenotypeProperties getPhenotype() {
        return phenotype;
    }

    public void setPhenotype(PhenotypeProperties phenotype) {
        this.phenotype = phenotype;
    }

    public EvolutionProperties getEvolution() {
        return evolution;
    }

    public void setEvolution(EvolutionProperties evolution) {
        this.evolution = evolution;
    }

    public static class PredictionProperties {

        @NotNull
        private ChronoUnit timeUnit;
        @Min(1)
        private long timeFrame;

        public Duration duration() {
            return Duration.of(timeFrame, timeUnit);
        }

        public ChronoUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(ChronoUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        public long getTimeFrame() {
            return timeFrame;
        }

        public void setTimeFrame(long timeFrame) {
            this.timeFrame = timeFrame;
        }

    }

    public static class PhenotypeProperties {

        @NotNull
        private GenotypeProperties genotype;
        @Min(1)
        private Integer maximalAge = 1;

        public GenotypeProperties getGenotype() {
            return genotype;
        }

        public void setGenotype(GenotypeProperties genotype) {
            this.genotype = genotype;
        }

        public Integer getMaximalAge() {
            return maximalAge;
        }

        public void setMaximalAge(Integer maximalAge) {
            this.maximalAge = maximalAge;
        }

        public static class GenotypeProperties {

            @NotNull
            private GeneProperties gene;

            public GeneProperties getGene() {
                return gene;
            }

            public void setGene(GeneProperties gene) {
                this.gene = gene;
            }

            public static class GeneProperties {

                @NotNull
                private Integer min;
                @NotNull
                private Integer max;

                public Integer getMin() {
                    return min;
                }

                public void setMin(Integer min) {
                    this.min = min;
                }

                public Integer getMax() {
                    return max;
                }

                public void setMax(Integer max) {
                    this.max = max;
                }
            }

        }
    }

    public static class EvolutionProperties {

        @Positive
        private Long generations = 1000L;
        @Positive
        private Integer populationSize = 100;

        public Long getGenerations() {
            return generations;
        }

        public void setGenerations(Long generations) {
            this.generations = generations;
        }

        public Integer getPopulationSize() {
            return populationSize;
        }

        public void setPopulationSize(Integer populationSize) {
            this.populationSize = populationSize;
        }
    }
}
