package pl.airq.ga.domain.training;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.enriched.EnrichedDataQuery;
import pl.airq.common.domain.phenotype.Prediction;
import pl.airq.common.vo.StationId;
import pl.airq.ga.domain.phenotype.Pm10PhenotypeMap;

@ApplicationScoped
public class TrainingDataService {

    private final EnrichedDataQuery enrichedDataQuery;

    @Inject
    public TrainingDataService(EnrichedDataQuery enrichedDataQuery) {
        this.enrichedDataQuery = enrichedDataQuery;
    }

    public TrainingData createTrainingData(StationId stationId, Duration withPredictionAfter) {
        return createTrainingData(stationId, withPredictionAfter, Pm10PhenotypeMap.DEFAULT_ENRICHED_DATA_FIELDS);
    }

    public TrainingData createTrainingData(StationId stationId, Duration withPredictionAfter, List<String> fields) {
        final Pm10PhenotypeMap pm10PhenotypeMap = Pm10PhenotypeMap.withFields(fields);
        final Prediction predictionConfig = new Prediction(
                withPredictionAfter.toHours(),
                ChronoUnit.HOURS,
                pm10PhenotypeMap.fieldToPredict());
        final TrainingData trainingData = new TrainingData(stationId, pm10PhenotypeMap.getFields(), predictionConfig);
        final Set<EnrichedData> enrichedData = enrichedDataQuery.findAllByStationId(stationId)
                                                                .await()
                                                                .indefinitely();
        for (EnrichedData entry : enrichedData) {
            findClosest(entry, enrichedData, withPredictionAfter)
                    .map(pm10PhenotypeMap::valueToPredict)
                    .map(valueToPredict -> {
                        final float[] values = pm10PhenotypeMap.map(entry);
                        if (values == null) {
                            return null;
                        }
                        return new TrainingDataRow(entry.timestamp, values, valueToPredict);
                    })
                    .ifPresent(trainingData::addData);
        }

        return trainingData;
    }

    private Optional<EnrichedData> findClosest(EnrichedData enrichedData, Set<EnrichedData> from, Duration withPredictionAfter) {
        long delta = Duration.ofMinutes(5).toSeconds();
        OffsetDateTime min = enrichedData.timestamp.plusSeconds(withPredictionAfter.getSeconds() - delta);
        OffsetDateTime max = enrichedData.timestamp.plusSeconds(withPredictionAfter.getSeconds() + delta);
        return from.stream()
                   .filter(ed -> ed.timestamp.isAfter(min))
                   .filter(ed -> ed.timestamp.isBefore(max))
                   .findFirst();
    }
}
