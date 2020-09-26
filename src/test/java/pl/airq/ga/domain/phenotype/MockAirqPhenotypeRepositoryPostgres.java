package pl.airq.ga.domain.phenotype;

import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.phenotype.AirqPhenotype;

@Mock
@ApplicationScoped
public class MockAirqPhenotypeRepositoryPostgres extends AirqPhenotypeRepositoryPostgres {

    private Boolean result = Boolean.TRUE;

    public MockAirqPhenotypeRepositoryPostgres() {
        super(null, null);
    }

    @Override
    public Uni<Boolean> save(AirqPhenotype data) {
        return Uni.createFrom().item(result);
    }

    @Override
    public Uni<Boolean> upsert(AirqPhenotype data) {
        return Uni.createFrom().item(result);
    }

    public void setSaveAndUpsertResult(Boolean result) {
        this.result = result;
    }
}
