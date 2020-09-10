package pl.airq.ga.domain.training;

import java.time.OffsetDateTime;

public class TrainingDataRow {

    public final OffsetDateTime timestamp;
    public final float[] values;
    public final float expectedValue;

    public TrainingDataRow(OffsetDateTime timestamp, float[] values, float expectedValue) {
        this.timestamp = timestamp;
        this.values = values;
        this.expectedValue = expectedValue;
    }
}
