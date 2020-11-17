package pl.airq.ga.domain.phenotype.command;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.process.Payload;
import pl.airq.common.vo.StationId;

@RegisterForReflection
public class CreatePhenotypePayload implements Payload {

    public final StationId stationId;

    public CreatePhenotypePayload(StationId stationId) {
        this.stationId = stationId;
    }

    @Override
    public String toString() {
        return "CreatePhenotypePayload{" +
                "stationId=" + stationId +
                '}';
    }
}
