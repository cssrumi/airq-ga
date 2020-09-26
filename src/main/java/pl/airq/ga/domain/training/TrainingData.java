package pl.airq.ga.domain.training;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import pl.airq.common.domain.prediction.PredictionConfig;
import pl.airq.common.vo.StationId;


public final class TrainingData {

    private final List<TrainingDataRow> rows;
    public final StationId stationId;
    public final List<String> fields;
    public final int rowSize;
    public final PredictionConfig predictionConfig;

    TrainingData(StationId stationId, List<String> fields, PredictionConfig predictionConfig) {
        this.stationId = stationId;
        this.fields = fields;
        this.rowSize = fields.size();
        this.predictionConfig = predictionConfig;
        this.rows = new ArrayList<>();
    }

    void addData(TrainingDataRow row) {
        rows.add(row);
    }

    public Stream<TrainingDataRow> stream() {
        return rows.stream();
    }

    public long size() {
        return rows.size();
    }

    @Override
    public String toString() {
        return "TrainingData{" +
                "stationId=" + stationId +
                ", fields=" + fields +
                ", size=" + size() +
                '}';
    }
}
