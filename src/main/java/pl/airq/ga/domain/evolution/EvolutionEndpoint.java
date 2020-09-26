package pl.airq.ga.domain.evolution;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import pl.airq.common.vo.StationId;

@Path("/api/evolution")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvolutionEndpoint {

    private final EvolutionProcessor evolutionProcessor;

    @Inject
    public EvolutionEndpoint(EvolutionProcessor evolutionProcessor) {
        this.evolutionProcessor = evolutionProcessor;
    }

    @GET
    @Path("/{stationId}")
    public Response calculatePhenotype(@PathParam String stationId) {
        final StationId id = StationId.from(stationId);
        evolutionProcessor.process(id);
        return Response.ok().build();
    }
}
