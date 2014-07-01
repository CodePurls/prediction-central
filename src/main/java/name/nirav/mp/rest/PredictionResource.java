package name.nirav.mp.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import name.nirav.mp.filters.TrackingFilter;
import name.nirav.mp.rest.dto.CountedContainer;
import name.nirav.mp.rest.dto.Errors;
import name.nirav.mp.rest.dto.Type;
import name.nirav.mp.security.CaptchaManager;
import name.nirav.mp.security.Roles;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.Comment;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.Source;
import name.nirav.mp.service.dto.User;
import name.nirav.mp.service.search.Schema;
import name.nirav.mp.service.search.WordsWithFreq;
import name.nirav.mp.utils.PredictionExceptionMapper;
import name.nirav.mp.utils.TextUtils;
import name.nirav.mp.utils.TimeUtils;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.yammer.dropwizard.jersey.caching.CacheControl;
import com.yammer.metrics.annotation.Timed;

@Path("predictions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PredictionResource {

  private static final Logger     LOGGER    = LoggerFactory.getLogger(PredictionResource.class.getSimpleName());
  private final PredictionService predictionService;

  private File                    uploadDir = new File(System.getProperty("java.io.tmpdir"));

  public PredictionResource(PredictionService service) {
    this.predictionService = service;
  }

  @Path("search")
  @GET
  @Timed
  @CacheControl(maxAge = 5, sharedMaxAgeUnit = TimeUnit.MINUTES)
  public Response search(@Context SecurityContext sec, @QueryParam("q") String query, @DefaultValue("10") @QueryParam("size") int size) {
    return Response.ok(predictionService.search(sec, query, size)).build();
  }

  @Path("search/suggest")
  @GET
  @Timed
  @CacheControl(maxAge = 5, sharedMaxAgeUnit = TimeUnit.MINUTES)
  public Response suggest(@Context SecurityContext sec, @QueryParam("term") String term, @QueryParam("max") @DefaultValue("10") int top) {
    return Response.ok(predictionService.suggest(sec, term, top)).build();
  }

  @Path("bulk")
  @PUT
  @Timed
  @Consumes(MediaType.TEXT_PLAIN)
  public Response bulk(@Context SecurityContext sec, InputStream input) throws IOException {
    Scanner scanner = new Scanner(input);
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.trim().isEmpty()) continue;
      Map<String, Object> map = TextUtils.infer(line);
      Prediction p = new Prediction();
      p.setText((String) map.get("text"));
      p.setSourceAuthor((String) map.get("author"));
      p.setTime(TimeUtils.getYear((Integer) map.get("year")));
      p.setTags((String) map.get("tags"));
      p.setSource(Source.WEB);
      createPrediction(sec, p, true);
    }
    scanner.close();
    input.close();
    return Response.ok("OK").build();
  }

  @PUT
  @Timed
  @Path("approve/{id}")
  public Response approve(@Context SecurityContext sec, @PathParam("id") int predictionId) {
    boolean approved = predictionService.approve(sec, predictionId);
    return approved ? Response.ok("{\"message\": \"Approved\"}").build() : Response.status(Status.FORBIDDEN)
        .entity("{\"message\": \"Approval failure\"}").build();
  }

  @PUT
  @Timed
  @Path("reject/{id}")
  public Response reject(@Context SecurityContext sec, @PathParam("id") int predictionId) {
    boolean rejected = predictionService.reject(sec, predictionId);
    return rejected ? Response.ok("{\"message\": \"Prediction rejected and removed\"}").build() : Response.status(Status.FORBIDDEN)
        .entity("{\"message\": \"Rejection failure\"}").build();
  }

  @GET
  @Timed
  @Path("captcha")
  public Response getCaptcha() {
    return Response.ok(CaptchaManager.getNextCaptcha()).build();
  }

  @GET
  @Timed
  @Path("count")
  @CacheControl(maxAge = 5, sharedMaxAgeUnit = TimeUnit.MINUTES)
  public Response getTotal(@Context SecurityContext sec) {
    return Response.ok(predictionService.getCounts(sec)).build();
  }

  @GET
  @Timed
  @CacheControl(maxAge = 5, sharedMaxAgeUnit = TimeUnit.MINUTES)
  public Response getPredictions(@Context SecurityContext sec, @DefaultValue("false") @QueryParam("full") Boolean full,
      @DefaultValue("1") @QueryParam("page") int page, @DefaultValue("10") @QueryParam("size") int size) {
    List<Prediction> predictions = predictionService.getPredictions(sec, page, size);
    if (full == Boolean.FALSE) predictions = strip(predictions);
    return Response.ok(CountedContainer.wrap(predictions, predictionService.getPredictionCount(sec), size)).build();
  }

  private List<Prediction> strip(List<Prediction> predictions) {
    for (Prediction prediction : predictions) {
      String original = prediction.getText();
      String abbreviated = StringUtils.abbreviate(prediction.getText(), 128);
      prediction.setText(abbreviated);
      prediction.setHasMore(original.length() > abbreviated.length());
    }
    return predictions;
  }

  @GET
  @Timed
  @Path("{id}")
  @CacheControl(maxAge = 1, sharedMaxAgeUnit = TimeUnit.DAYS)
  public Response getPrediction(@PathParam("id") int id) {
    try {
      return Response.ok(CaptchaManager.encode(predictionService.getPrediction(id))).build();
    } catch (Exception e) {
      LOGGER.info("Request to get prediction with id {} failed with message: {}", id, e.getMessage());
      throw new WebApplicationException(Response.status(Status.NOT_FOUND).build());
    }
  }

  @DELETE
  @Timed
  @Path("{id}")
  public Response deletePrediction(@PathParam("id") int id) {
    if (id > 0) {
      predictionService.deletePrediction(id);
      return Response.status(Status.NO_CONTENT).build();
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  @POST
  @Timed
  @Path("iupload/{rand}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadImage(@PathParam("rand") int rand, @FormDataParam("image") InputStream stream,
      @FormDataParam("image") FormDataContentDisposition fileDetail) throws IOException {
    Files.copy(stream, new File(uploadDir, "" + rand).toPath());
    return Response.ok().build();
  }

  @Path("{id}/comments")
  @POST
  @Timed
  public Response createComment(@Context SecurityContext sec, @PathParam("id") int id, @Valid Comment comment) {
    final User user = predictionService.securityCheck(sec, Roles.COMMENT);
    if (!user.isRegistered()) {
      if (!CaptchaManager.verify(comment.getCaptchaId(), comment.getCaptcha())) { return Response
          .status(PredictionExceptionMapper.UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON_TYPE).entity(Errors.create("captcha,invalid"))
          .build(); }
    }
    int cid = predictionService.createComment(sec, id, comment);
    return Response.created(UriBuilder.fromPath("{cid}").build(cid)).entity(comment).build();
  }

  @Path("users")
  @POST
  @Timed
  public Response createUser(@Context SecurityContext sec, User user) {
    if (!CaptchaManager.verify(user.getCaptchaId(), user.getCaptcha())) { return Response.status(PredictionExceptionMapper.UNPROCESSABLE_ENTITY)
        .type(MediaType.APPLICATION_JSON_TYPE).entity(Errors.create("captcha,invalid")).build(); }
    try {
      final User created = predictionService.createUser(sec, user);
      return Response.created(UriBuilder.fromPath("users/{id}").build(created.getId())).entity(created).build();
    } catch (DuplicateKeyException e) {
      return Response.status(Status.CONFLICT).entity(Errors.create(Type.duplicate, "email")).build();
    }
  }

  @Path("users/current")
  @GET
  @Timed
  @CacheControl(maxAge = 1, sharedMaxAgeUnit = TimeUnit.HOURS)
  public Response getCurrentUser(@Context SecurityContext sec) {
    User user = (User) sec.getUserPrincipal();
    if (user == null) { return Response.status(Status.NOT_FOUND).build(); }
    User userInDB = predictionService.getUser(user.getId());
    return Response.ok(userInDB).build();
  }

  @Path("users/login/{time}")
  @POST
  @Timed
  public Response login(User user, @Context HttpServletResponse resp) {
    User verifiedUser = predictionService.authenticate(user);
    if (verifiedUser == null) {
      Errors error = Errors.create(Type.invalid, "id");
      error.addError(Type.invalid, "pass");
      return Response.status(Status.UNAUTHORIZED).entity(error).build();
    }
    TrackingFilter.createUserCookie(verifiedUser, resp);
    return Response.ok(verifiedUser).build();
  }

  @POST
  @Timed
  public Response createPrediction(@Context SecurityContext sec, @Valid @NotEmpty Prediction pred, @QueryParam("temp") boolean dontVoteUp) {

    if (StringUtils.isBlank(pred.getText())) { return Response.status(PredictionExceptionMapper.UNPROCESSABLE_ENTITY)
        .type(MediaType.APPLICATION_JSON_TYPE).entity(Errors.create(Type.required, "text")).build(); }
    pred.setApproved(true);
    pred.setSource(Source.WEB);
    Prediction prediction = predictionService.createPrediction(sec, pred);
    return Response.created(UriBuilder.fromPath("{id}").build(prediction.getId())).entity(prediction).build();
  }

  @Path("{id}/up")
  @PUT
  @Timed
  public Response voteUp(@Context SecurityContext sec, @PathParam("id") int id) {
    boolean voteUp = predictionService.voteUp(sec, id);
    return voteUp ? Response.noContent().build() : Response.status(Status.PRECONDITION_FAILED)
        .entity(Errors.createPlain("You already upvoted that prediction")).build();
  }

  @Path("{id}/down")
  @PUT
  @Timed
  public Response voteDown(@Context SecurityContext sec, @PathParam("id") int id) {
    boolean voteDown = predictionService.voteDown(sec, id);
    return voteDown ? Response.noContent().build() : Response.status(Status.PRECONDITION_FAILED)
        .entity(Errors.createPlain("You already downvoted that prediction")).build();
  }

  @Path("analytics")
  @GET
  @Timed
  @CacheControl(maxAge = 1, sharedMaxAgeUnit = TimeUnit.MINUTES)
  public Response analyze(@Context SecurityContext sec, @QueryParam("days") @DefaultValue("10") Integer days,
      @QueryParam("top") @DefaultValue("10") Integer top) throws IOException {
    Map<String, Object> predAnalytics = new HashMap<>();
    predAnalytics.put("Trend", predictionService.aggregateByCreateDate(sec, days, Schema.PredictionDoc.CREATED_ON.fieldName()));
    predAnalytics.put("Top Sources", predictionService.aggregateBySource(sec));

    Map<String, List<WordsWithFreq>> analyticsMap = new HashMap<>();
    for (Schema.EntityDoc entityDoc : Schema.EntityDoc.values()) {
      analyticsMap.put(entityDoc.name(), predictionService.getTopTagsForField(sec, top, entityDoc));
    }
    return Response.ok(new Object[] { predAnalytics, analyticsMap }).build();
  }

  @Path("search/aggregates")
  @GET
  @Timed
  @CacheControl(maxAge = 5, sharedMaxAgeUnit = TimeUnit.MINUTES)
  public Response searchAggregates(@Context SecurityContext sec, @QueryParam("days") @DefaultValue("10") Integer days,
      @QueryParam("field") @DefaultValue("created_on") String field) {
    return Response.ok(predictionService.aggregateByCreateDate(sec, days, field)).build();
  }

  @Path("tags")
  @GET
  @Timed
  @CacheControl(maxAge = 1, sharedMaxAgeUnit = TimeUnit.HOURS)
  public Response getTopTags(@Context SecurityContext sec, @QueryParam("max") @DefaultValue("25") int top,
      @QueryParam("field") @DefaultValue("text") String field) {
    return Response.ok(predictionService.getTopTags(sec, top, field)).build();
  }
}