package pl.airq.ga.model.command;

import pl.airq.common.domain.process.Payload;
import pl.airq.common.vo.StationId;

public class CreateNewPhenotypePayload implements Payload {

    public final StationId stationId;

    public CreateNewPhenotypePayload(StationId stationId) {
        this.stationId = stationId;
    }
}
