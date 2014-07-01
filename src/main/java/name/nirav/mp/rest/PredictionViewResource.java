package name.nirav.mp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import name.nirav.mp.security.CaptchaManager;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.User;
import name.nirav.mp.views.PredictionView;

import com.yammer.metrics.annotation.Timed;

@Path("view")
@Produces(MediaType.TEXT_HTML)
public class PredictionViewResource {
  private PredictionService service;

  public PredictionViewResource(PredictionService service) {
    this.service = service;
  }

  @Path("{id}")
  @GET
  @Timed
  public PredictionView getPredictionView(@Context SecurityContext sec, @PathParam("id") int id) {
    return new PredictionView(CaptchaManager.encode(service.getPrediction(id)), (User) sec.getUserPrincipal());
  }
}
