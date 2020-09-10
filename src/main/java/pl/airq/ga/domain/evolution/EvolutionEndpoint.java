package pl.airq.ga.domain.evolution;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.station.StationQuery;
import pl.airq.common.vo.StationId;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvolutionEndpoint {

    private final EvolutionServiceFacade evolutionServiceFacade;
    private final StationQuery stationQuery;

    @Inject
    public EvolutionEndpoint(EvolutionServiceFacade evolutionServiceFacade, StationQuery stationQuery) {
        this.evolutionServiceFacade = evolutionServiceFacade;
        this.stationQuery = stationQuery;
    }

    @GET
    @Path("/stations")
    public Response getStations() {
        return Response.ok(stationQuery.findAll().await().indefinitely()).build();
    }

    @GET
    @Path("/calculate/{stationId}")
    public Response calculatePhenotype(@PathParam String stationId) {
        final StationId id = StationId.from(stationId);
        final AirqPhenotype phenotype = evolutionServiceFacade.generateNewPhenotype(id);
        return Response.ok(phenotype).build();
    }
}
