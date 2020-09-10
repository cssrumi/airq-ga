package pl.airq.ga.model.event;

import pl.airq.common.domain.process.Payload;
import pl.airq.common.vo.StationId;

public class NewPhenotypeCreatedPayload implements Payload {

    public final StationId stationId;

    public NewPhenotypeCreatedPayload(StationId stationId) {
        this.stationId = stationId;
    }
}
