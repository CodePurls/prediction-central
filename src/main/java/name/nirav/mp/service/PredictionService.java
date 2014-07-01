package name.nirav.mp.service;

import static java.lang.String.format;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import name.nirav.mp.crawlers.AbstractCrawler;
import name.nirav.mp.db.PredictionDB;
import name.nirav.mp.rest.dto.CountedContainer;
import name.nirav.mp.rest.dto.Errors;
import name.nirav.mp.security.Roles;
import name.nirav.mp.service.dto.Comment;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.User;
import name.nirav.mp.service.dto.Visitor;
import name.nirav.mp.service.search.FacetedSearchResults;
import name.nirav.mp.service.search.Schema;
import name.nirav.mp.service.search.SearchResults;
import name.nirav.mp.service.search.SearchService;
import name.nirav.mp.service.search.WordsWithFreq;
import name.nirav.mp.utils.Fingerprint;
import name.nirav.mp.utils.InputSanitizer;
import name.nirav.mp.utils.TextUtils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

/**
 * @author Nirav Thaker
 */
public class PredictionService {
  private static final int    MIN_PREDICTION_LENGTH = 64;
  private static final int    MAX_PREDICTION_LENGTH = 10240;
  private final PredictionDB  db;
  private final SearchService searchService;
  private static final Logger LOGGER                = LoggerFactory.getLogger(PredictionService.class.getSimpleName());

  public static final class Counts {
    public int predictions, users, visitors;
  }

  private final SecurityContext adminSecurityContext = new SecurityContext() {
                                                       @Override
                                                       public Principal getUserPrincipal() {
                                                         return getUser("admin@predictions.com");
                                                       }

                                                       @Override
                                                       public boolean isUserInRole(String role) {
                                                         return true;
                                                       }

                                                       @Override
                                                       public boolean isSecure() {
                                                         return true;
                                                       }

                                                       @Override
                                                       public String getAuthenticationScheme() {
                                                         return "System";
                                                       }
                                                     };

  public PredictionService(PredictionDB db, SearchService searchService) {
    this.db = db;
    this.searchService = searchService;
  }

  public SecurityContext getAdminSecurityContext() {
    return adminSecurityContext;
  }

  public Visitor recordVisit(Fingerprint fingerprint) {
    return db.recordVisit(fingerprint);
  }

  public Visitor createVisitor(int id) {
    return db.createVisitor(id);
  }

  public List<AbstractCrawler.CrawlerLog> createCrawlerLog(List<AbstractCrawler.CrawlerLog> logList) {
    return db.createCralwerLog(logList);
  }

  public int fixTwitterCreatedBy() {
    return db.fixTwitterCreatedBy();
  }

  public int fixFacebookCreatedBy() {
    return db.fixFacebookCreatedBy();
  }

  public void dropUnapprovedPredictions() {
    db.dropUnapprovedPredictions();
  }

  public void approveAllPredictions() {
    LOGGER.info("Approving all unapproved predictions");
    List<Prediction> predictions = db.approveAllPredictions();
    LOGGER.info("Approved {} predictions", predictions.size());
  }

  public List<WordsWithFreq> getTopTagsForField(SecurityContext sec, int top, Schema field) {
    securityCheck(sec, Roles.SEARCH);
    return searchService.getTopTags(top, field);
  }

  public List<WordsWithFreq> getTopTags(SecurityContext sec, int top, String field) {
    Schema schema;
    try {
      schema = Schema.PredictionDoc.fromFieldName(field);
    } catch (IllegalArgumentException e) {
      try {
        schema = Schema.EntityDoc.fromFieldName(field);
      } catch (IllegalArgumentException e2) {
        schema = new Schema() {
          public String fieldName() {
            return field;
          }

          public boolean isDynamic() {
            return true;
          }
        };
      }
    }
    return getTopTagsForField(sec, top, schema);
  }

  public Map<String, FacetedSearchResults> aggregateByCreateDate(SecurityContext sec, Integer days, String field) {
    securityCheck(sec, Roles.SEARCH);
    return searchService.getCountsForLastNDays(days, field);
  }

  public FacetedSearchResults aggregateBySource(SecurityContext sec) throws IOException {
    securityCheck(sec, Roles.SEARCH);
    return searchService.aggregateBySource();
  }

  public Object suggest(SecurityContext sec, String term, int top) {
    securityCheck(sec, Roles.SEARCH);
    return searchService.suggest(term, top);
  }

  public CountedContainer<Prediction> search(SecurityContext sec, String query, int resultSize) {
    securityCheck(sec, Roles.SEARCH);
    SearchResults results = searchService.search(query, resultSize);
    if (results.predictions.isEmpty()) { return CountedContainer.wrap(Collections.emptyList(), results.hits, resultSize); }
    List<Prediction> list = InputSanitizer.desanitize(db.getPredictions(results.predictions));
    return CountedContainer.wrap(list, results.hits, resultSize);
  }

  public Counts getCounts(SecurityContext sec) {
    Counts c = new Counts();
    c.predictions = db.getPredictionCount(isAdmin(sec));
    c.users = db.getUserCount();
    c.visitors = db.getVisitorCount();
    return c;
  }

  public List<Prediction> createPredictions(SecurityContext sec, List<Prediction> preds) {
    int originalSize = preds.size();
    List<Prediction> filtered = preds.stream().filter(p -> p.getText() != null && !p.getText().isEmpty())
        .filter(p -> p.getText().length() > MIN_PREDICTION_LENGTH).collect(Collectors.toCollection(ArrayList::new));
    int newSize = filtered.size();
    if (newSize != originalSize) LOGGER.info("Filtered predictions with large/small bodies: {}", originalSize - newSize);
    filtered.stream().forEach(p -> createPrediction(sec, p));
    return preds;
  }

  public Prediction createPrediction(SecurityContext sec, Prediction pred) {
    User user = securityCheck(sec, Roles.CREATE_PREDICTION);
    pred.setTags(TextUtils.tag(pred.getTags()));
    int id;
    pred = InputSanitizer.sanitize(pred);
    if (StringUtils.isBlank(pred.getSourceAuthor())) {
      pred.setType(Prediction.PredictionType.quote.name());
    } else {
      pred.setType(Prediction.PredictionType.prediction.name());
    }
    if (pred.getTitle() == null || pred.getTitle().trim().isEmpty()) {
      pred.setTitle(StringUtils.abbreviate(pred.getText(), 64));
    } else {
      pred.setTitle(StringUtils.abbreviate(pred.getTitle(), 64));
    }
    pred.setText(StringUtils.abbreviate(pred.getText(), MAX_PREDICTION_LENGTH));
    if (sec != getAdminSecurityContext()) {
      pred.setCreatedByUserId(user.getId());
      pred.setCreatedByUser(user.getFullName());
    }
    pred.setTime(TextUtils.getProbablePredictionTime(pred.getText()));
    id = db.createPrediction(pred);
    pred.setId(id);
    return db.getPrediction(id);
  }

  public boolean approve(SecurityContext sec, int predictionId) {
    if (isOwner(sec, predictionId)) {
      this.db.approvePrediction(predictionId);
      this.db.getPrediction(predictionId);
      return true;
    }
    return false;
  }

  public boolean reject(SecurityContext sec, int predictionId) {
    if (isOwner(sec, predictionId)) {
      this.db.deletePrediction(predictionId);
      return true;
    }
    return false;
  }

  public int getPredictionCount(SecurityContext sec) {
    return db.getPredictionCount(isAdmin(sec));
  }

  public List<Prediction> getPredictions(SecurityContext sec, int page, int pageSize) {
    boolean includedApproved = isAdmin(sec);
    return InputSanitizer.desanitize(db.getPredictions(includedApproved, page, pageSize));
  }

  public boolean isOwner(SecurityContext sec, int predictionId) {
    User user = ((User) sec.getUserPrincipal());
    return isAdmin(sec) || db.getPrediction(predictionId).getCreatedByUserId() == user.getId();
  }

  private boolean isAdmin(SecurityContext sec) {
    String userName = ((User) sec.getUserPrincipal()).getUserName();
    return userName != null && userName.equals("admin");
  }

  public Prediction getPrediction(int id) {
    return InputSanitizer.desanitize(db.getPrediction(id));
  }

  public void deletePrediction(int id) {
    db.deletePrediction(id);
  }

  public int createComment(SecurityContext sec, int predictionId, Comment comment) {
    User user = securityCheck(sec, Roles.COMMENT);
    Prediction prediction = getPrediction(predictionId);
    comment = InputSanitizer.sanitize(comment);
    comment.setCreatedByUserId(user.getId());
    if (StringUtils.isBlank(comment.getAuthor())) {
      comment.setAuthor(user.getEmail());
    }
    int cid = db.createComment(prediction.getId(), comment);
    comment.setId(cid);
    comment.setPredictionId(predictionId);
    return cid;
  }

  public boolean voteUp(SecurityContext sec, int predictionId) {
    User user = securityCheck(sec, Roles.VOTE);
    return db.voteUp(user, predictionId);
  }

  public boolean voteDown(SecurityContext sec, int predictionId) {
    User user = securityCheck(sec, Roles.VOTE);
    return db.voteDown(user, predictionId);
  }

  public User getUser(int userId) {
    return db.getUser(userId);
  }

  public User getUser(String email) {
    return db.getUser(email);
  }

  public User createUser(SecurityContext sec, User user) {
    User existingUser = getUser(((User) sec.getUserPrincipal()).getId());
    if (existingUser == null) {// User never registed
      return db.createUser(user, ((User) sec.getUserPrincipal()));
    } else if (existingUser.getEmail().equals(user.getEmail())) { // Email exists
      throw new DuplicateKeyException("");
    }
    return existingUser;
  }

  public User authenticate(User user) {
    return db.verifyUser(user);
  }

  public User securityCheck(SecurityContext sec, Roles role) {
    if (sec == null) { throw new NullPointerException(); }

    if (!sec.isUserInRole(role.name())) { throw new WebApplicationException(forbiddenResponse(role, sec)); }

    return (User) sec.getUserPrincipal();
  }

  private Response forbiddenResponse(Roles search, SecurityContext sec) {
    return Response.status(Response.Status.FORBIDDEN)
        .entity(Errors.createPlain(format("%s is unavailable to %s ", search, sec.getUserPrincipal().getName()))).build();
  }
}
