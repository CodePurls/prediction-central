package name.nirav.mp.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import name.nirav.mp.crawlers.AbstractCrawler;
import name.nirav.mp.security.PasswordHash;
import name.nirav.mp.service.dto.Comment;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.PredictionIndexStatus.Status;
import name.nirav.mp.service.dto.User;
import name.nirav.mp.service.dto.Visitor;
import name.nirav.mp.service.monitoring.MonitoringService;
import name.nirav.mp.utils.Fingerprint;
import name.nirav.mp.utils.Tuple;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.yammer.dropwizard.db.DatabaseConfiguration;
import com.yammer.metrics.core.TimerContext;

public class PredictionDB {
  private static final RowMapper<Prediction>  PREDICTION_MAPPER  = (r, rowNum) -> {
                                                                   Prediction p = new Prediction();
                                                                   p.setId(r.getInt("id"));
                                                                   p.setTitle(r.getString("title"));
                                                                   p.setApproved(r.getBoolean("approved"));
                                                                   p.setText(r.getString("prediction_text"));
                                                                   p.setTime(r.getLong("prediction_time"));
                                                                   p.setLocation(r.getString("location"));
                                                                   p.setAbout(r.getString("actor"));
                                                                   p.setReason(r.getString("reason"));
                                                                   p.setTags(r.getString("tags"));
                                                                   p.setSourceAuthor(r.getString("original_author"));
                                                                   p.setSourceRef(r.getString("original_source"));
                                                                   p.setCreatedByUser(r.getString("created_by"));
                                                                   p.setCreatedByUserId(r.getInt("created_by_id"));
                                                                   p.setCreateTimestamp(r.getLong("created_on"));
                                                                   p.setUpdateTimestamp(r.getLong("updated_on"));
                                                                   p.setUpvotes(r.getInt("ups"));
                                                                   p.setDownvotes(r.getInt("downs"));
                                                                   return p;
                                                                 };
  private static final RowMapper<Comment>     COMMENT_MAPPER     = (rs, rowNum) -> {
                                                                   Comment c = new Comment();
                                                                   c.setId(rs.getInt("id"));
                                                                   c.setAuthor(rs.getString("author"));
                                                                   c.setComment(rs.getString("comment"));
                                                                   c.setPredictionId(rs.getInt("prediction_id"));
                                                                   return c;
                                                                 };
  private static final RowMapper<User>        USER_MAPPER        = (rs, rowNum) -> {
                                                                   User u = new User(rs.getString("user_name"), rs.getString("full_name"));
                                                                   u.setId(rs.getInt("id"));
                                                                   u.setEmail(rs.getString("email"));
                                                                   return u;
                                                                 };

  private static final RowMapper<Visitor>     VISITOR_MAPPER     = (rs, rowNum) -> new Visitor(rs.getInt("id"), rs.getInt("number"));
  private static final RowMapper<Fingerprint> FINGERPRINT_MAPPER = (rs, rowNum) -> new Fingerprint(rs.getInt("id"), rs.getString("fingerprint"),
                                                                     rs.getInt("user_id"), rs.getInt("visitor_id"));

  private static final Logger                 LOGGER             = LoggerFactory.getLogger(PredictionDB.class.getSimpleName());

  private final ComboPooledDataSource         dataSource;

  public PredictionDB(DatabaseConfiguration database) {
    try {
      ComboPooledDataSource ds = new ComboPooledDataSource();
      ds.setDriverClass(database.getDriverClass());
      ds.setJdbcUrl(database.getUrl());
      ds.setUser(database.getUser());
      ds.setMinPoolSize(1);
      ds.setInitialPoolSize(1);
      ds.setMaxPoolSize(10);
      ds.setPreferredTestQuery("select 1");
      this.dataSource = ds;
      getJdbcTemplate().execute("select 1");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public JdbcTemplate getJdbcTemplate() {
    return new JdbcTemplate(dataSource);
  }

  public NamedParameterJdbcTemplate getNamedJdbcTemplate() {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  public Visitor createVisitor(int id) {
    String sql = "insert into visitors(id) values(?)";
    getJdbcTemplate().update(sql, id);
    return getVisitor(id);
  }

  public Visitor recordVisit(Fingerprint fingerprint) {
    Visitor existing = getVisitor(fingerprint.getId());
    Fingerprint f = getFingerprint(fingerprint.getId());
    if (f == null) {
      Visitor v = existing == null ? createVisitor(fingerprint.getId()) : existing;
      String sql = "insert into fingerprints(id, fingerprint, visitor_id) values(:id, :fingerprint, :visitor_id)";
      Map<String, Object> params = new HashMap<>();
      params.put("id", fingerprint.getId());
      params.put("fingerprint", fingerprint.toString());
      params.put("visitor_id", v.getId());
      getNamedJdbcTemplate().update(sql, params);
    }
    return getVisitor(fingerprint.getId());
  }

  private Fingerprint getFingerprint(int id) {
    try {
      return getJdbcTemplate().queryForObject("select * from fingerprints where id = ?", FINGERPRINT_MAPPER, id);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  private Visitor getVisitor(int id) {
    try {
      return getJdbcTemplate().queryForObject(" select * from visitors where id = ? ", VISITOR_MAPPER, id);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public int getPredictionCount(boolean includeUnapproved) {
    if (includeUnapproved) return getJdbcTemplate().queryForObject("select count(id) from prediction ", Integer.class);
    else return getJdbcTemplate().queryForObject("select count(id) from prediction ", Integer.class);
  }

  public int getUserCount() {
    return getJdbcTemplate().queryForObject("select count(id) from users", Integer.class);
  }

  public int getVisitorCount() {
    return getJdbcTemplate().queryForObject("select count(id) from visitors", Integer.class);
  }

  public List<Tuple<Integer, Integer>> getCommentCounts(Collection<Integer> predictionIds) {
    if (predictionIds.isEmpty()) return Collections.emptyList();
    String ids = StringUtils.join(predictionIds, ',');
    return getJdbcTemplate().query("select prediction_id, count(id) from comment where prediction_id in (" + ids + ") group by prediction_id ",
        (rs, rowNum) -> Tuple.of(rs.getInt(1), rs.getInt(2)));
  }

  public List<Prediction> getPredictions(boolean includeUnapproved, int page, int limit) {
    TimerContext context = MonitoringService.time(getClass(), "getPredictions");
    int offset = (page - 1) * limit;
    Map<String, Object> params = new HashMap<>();
    params.put("offset", offset);
    params.put("limit", limit);
    String fragment = "";
    String orderBy = " order by created_on desc ";
    if (!includeUnapproved) {
      fragment = " where  approved = true ";
    }
    String query = "select * from prediction " + fragment + orderBy + " limit :limit offset :offset";
    List<Prediction> results = getNamedJdbcTemplate().query(query, params, PREDICTION_MAPPER);
    context.stop();
    List<Integer> ids = new ArrayList<>(results.size());
    for (Prediction prediction : results) {
      ids.add(prediction.getId());
    }
    List<Tuple<Integer, Integer>> counts = getCommentCounts(ids);
    for (Prediction prediction : results) {
      for (Tuple<Integer, Integer> tuple : counts) {
        if (tuple.getKey().equals(prediction.getId())) {
          prediction.setCommentCount(tuple.getValue());
        }
      }
    }
    return results;
  }

  public Prediction getPrediction(int id) {
    if (id <= 0) return null;
    Prediction prediction = null;
    int retryMax = 5, retries = 0;
    while (retries++ < retryMax) {
      try {
        prediction = getJdbcTemplate().queryForObject("select * from prediction where id = ?", new Integer[] { id }, PREDICTION_MAPPER);
        break;
      } catch (EmptyResultDataAccessException e) {
        LOGGER.warn("Retrying get Prediction {}", retries);
        try {
          Thread.sleep(100);
        } catch (InterruptedException e1) {
          Thread.interrupted();
          LOGGER.error("Interrupted while sleeping ", e1);
        }
      }
    }
    if (prediction == null) throw new RuntimeException("Unable to find prediction with id " + id);
    List<Comment> comments = getComments(id);
    prediction.setComments(comments);
    prediction.setCommentCount(comments.size());
    return prediction;
  }

  public List<Comment> getComments(int predictionId) {
    return getJdbcTemplate().query("select * from comment where prediction_id = ?", new Object[] { predictionId }, COMMENT_MAPPER);
  }

  public void deletePrediction(int id) {
    getJdbcTemplate().update("delete from prediction where id = ?", id);
  }

  public int createPrediction(Prediction pred) {
    String sql = "insert into prediction(title, prediction_text, prediction_time, location, actor, reason, tags, original_author, original_source, created_on, updated_on, created_by_id,created_by, approved) "
        + "values(:title, :prediction_text, :prediction_time, :location,:actor,:reason,:tags,:original_author, :original_source, :created_on, :updated_on, :created_by_id, :created_by, :approved)";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("approved", pred.isApproved());
    params.addValue("title", pred.getTitle());
    params.addValue("prediction_text", pred.getText());
    params.addValue("prediction_time", pred.getTime());
    params.addValue("location", pred.getLocation());
    params.addValue("actor", pred.getAbout());
    params.addValue("reason", pred.getReason());
    params.addValue("tags", pred.getTags());
    params.addValue("original_author", pred.getSourceAuthor());
    params.addValue("original_source", pred.getSourceRef());
    params.addValue("created_on", pred.getCreateTimestamp());
    params.addValue("updated_on", pred.getCreateTimestamp());
    params.addValue("created_by_id", pred.getCreatedByUserId());
    params.addValue("created_by", pred.getCreatedByUser());
    GeneratedKeyHolder holder = new GeneratedKeyHolder();
    getNamedJdbcTemplate().update(sql, params, holder);
    int nextId = holder.getKey().intValue();
    while (nextId < 0) {
      nextId = getNextId();
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
    }
    getJdbcTemplate().update("INSERT INTO prediction_index_status (prediction_id, index_status) values (? ,'U')", nextId);
    return nextId;
  }

  private int getNextId() {
    return getJdbcTemplate().queryForObject("call SCOPE_IDENTITY()", Integer.class);
  }

  public List<Prediction> getUnindexedPredictions(Status status, int pageSize) {
    TimerContext context = MonitoringService.time(PredictionDB.class, "getUnindexedPredictions");
    String sql = "select prediction_id from prediction_index_status where index_status = :status limit :limit";
    Map<String, Object> params = new HashMap<>();
    params.put("status", status.getCode());
    params.put("limit", pageSize);
    List<Integer> predIds = getNamedJdbcTemplate().queryForList(sql, params, Integer.class);
    context.stop();
    if (predIds.isEmpty()) return Collections.emptyList();
    return getPredictions(predIds);
  }

  public int markIndexed(Collection<Integer> indexedIds, Status status) {
    String sql = "update prediction_index_status set index_status = ? where prediction_id = ?";
    return indexedIds.stream().mapToInt(x -> getJdbcTemplate().update(sql, status.getCode(), x)).sum();
  }

  public void resetIndexStatus(Status status) {
    if (status == Status.INDEXED_WITHOUT_ENTITIES) {
      getJdbcTemplate().update("UPDATE prediction_index_status SET index_status = ? where index_status = 'I'", status.getCode());
    } else {
      getJdbcTemplate().update("delete from prediction_index_status");
      getJdbcTemplate().update("INSERT INTO prediction_index_status (prediction_id, index_status) SELECT id, 'U' from PREDICTION ORDER BY ID ASC ");
    }
  }

  public List<Prediction> getPredictions(List<Integer> predictions) {
    TimerContext time = MonitoringService.time(PredictionDB.class, "getPredictions");
    predictions = (List<Integer>) predictions.stream().distinct().collect(Collectors.toList());
    String sql = "select * from prediction where id in (:ids) ";
    Map<String, Object> params = new HashMap<>();
    params.put("ids", predictions);
    List<Prediction> list = getNamedJdbcTemplate().query(sql, params, PREDICTION_MAPPER);
    time.stop();
    return list;
  }

  public int createComment(int id, Comment comment) {
    String sql = "insert into comment(prediction_id, author, comment)  values(:prediction_id, :author, :comment)";
    Map<String, Object> params = new HashMap<>();
    params.put("prediction_id", id);
    params.put("author", comment.getAuthor());
    params.put("comment", comment.getComment());
    getNamedJdbcTemplate().update(sql, params);
    return getNextId();
  }

  public User createUser(User user, User existingUser) {
    String sql = "insert into users (user_name, full_name, email, phash) values (:uname, :fname, :email, :phash)";
    Map<String, Object> params = new HashMap<>();
    params.put("uname", user.getUserName());
    params.put("fname", user.getFullName());
    params.put("email", user.getEmail());
    params.put("phash", PasswordHash.createHash(user.getPass()));
    getNamedJdbcTemplate().update(sql, params);
    return getUser(getNextId());
  }

  public User verifyUser(User user) {
    String pass;
    try {
      pass = getJdbcTemplate().queryForObject("select phash from users where email = ? or user_name = ?", String.class, user.getEmail(),
          user.getUserName());
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
    boolean valid = PasswordHash.validatePassword(user.getPass(), pass);
    if (valid) { return getUser(user.getEmail()); }
    return null;
  }

  public User getUser(String email) {
    try {
      return getJdbcTemplate().queryForObject("select * from users where email = ?", new Object[] { email }, USER_MAPPER);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public User getUser(int id) {
    try {
      User user = getJdbcTemplate().queryForObject("select * from users where id = ?", new Object[] { id }, USER_MAPPER);
      List<Integer> upvotes = getJdbcTemplate().queryForList("select prediction_id from votes where voted = true and user_id = ?", Integer.class, id);
      List<Integer> downvotes = getJdbcTemplate().queryForList("select prediction_id from votes where voted = false and user_id = ?", Integer.class,
          id);
      user.setVotedUp(upvotes);
      user.setVotedDown(downvotes);
      return user;
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public boolean voteUp(User user, int pid) {
    if (!user.isRegistered()) return false;
    Map<String, Object> map = new HashMap<>();
    map.put("pid", pid);
    map.put("userId", user.getId());
    Integer count = getNamedJdbcTemplate().queryForObject(
        "select count(*) from votes where prediction_id = :pid and user_id = :userId and voted = true", map, Integer.class);
    if (count > 0) return false;
    getNamedJdbcTemplate().update("insert into votes (user_id, prediction_id, voted) values(:userId, :pid, true)", map);
    return getJdbcTemplate().update("update prediction set ups =  ups + 1 where id = ?", pid) > 0;
  }

  public boolean voteDown(User user, int pid) {
    if (!user.isRegistered()) return false;
    Map<String, Object> map = new HashMap<>();
    map.put("pid", pid);
    map.put("userId", user.getId());
    Integer count = getNamedJdbcTemplate().queryForObject(
        "select count(*) from votes where prediction_id = :pid and user_id = :userId and voted = false", map, Integer.class);
    if (count > 0) { return false; }
    getNamedJdbcTemplate().update("insert into votes (user_id, prediction_id, voted) values(:userId, :pid, false)", map);
    return getJdbcTemplate().update("update prediction set downs =  downs + 1 where id = ?", pid) > 0;
  }

  public void approvePrediction(int predictionId) {
    getJdbcTemplate().update("update prediction set approved = true where id = ?", predictionId);
  }

  public List<AbstractCrawler.CrawlerLog> createCralwerLog(List<AbstractCrawler.CrawlerLog> logList) {
    Map<String, AbstractCrawler.CrawlerLog> logMap = new HashMap<>(logList.size());
    for (AbstractCrawler.CrawlerLog l : logList) {
      logMap.put(l.sourceId, l);
    }
    List<AbstractCrawler.CrawlerLog> failedLog = new ArrayList<>();
    int dupIds = 0, dupContent = 0;
    for (AbstractCrawler.CrawlerLog l : logMap.values()) {
      try {
        getJdbcTemplate().update("insert into crawlerLog(sourceId, crawlerId, contentHash) values(?, ?, ?)", l.sourceId, l.crawlerId, l.contentHash);
      } catch (DataIntegrityViolationException e) {
        dupIds += e.getMessage().contains("UNIQUE_SOURCE_ID") ? 1 : 0;
        dupContent += e.getMessage().contains("UNIQUE_CONTENT") ? 1 : 0;
        failedLog.add(l);
      }
    }
    LOGGER.warn("Dedup report: Original posts {}, posts with duplicate ids {}, posts with duplicate contents {}", logList.size(), dupIds, dupContent);
    return failedLog;
  }

  public void dropUnapprovedPredictions() {
    getJdbcTemplate().update("delete from prediction where approved = false");
  }

  public List<Prediction> approveAllPredictions() {
    List<Prediction> predictions = getJdbcTemplate().query("select * from prediction where approved = false", PREDICTION_MAPPER);
    getJdbcTemplate().update("update prediction set approved = true");
    return predictions;
  }

  public void logWebServiceCall(Tuple<String, String> tuple) {
    try {
      getJdbcTemplate().update("insert into webservice_call_log (key, value) values (?, ?)", tuple.getKey(), tuple.getValue());
    } catch (Exception e) {
      LOGGER.error("Unable to insert in call log", e);
    }
  }

  public Tuple<String, String> lookupWebServiceCall(String hash) {
    try {
      String value = getJdbcTemplate().queryForObject("select value from webservice_call_log where key = ?", String.class, hash);
      return Tuple.of(hash, value);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public int fixTwitterCreatedBy() {
    List<Integer> pids;
    int updateCount = 0;
    while (true) {
      pids = getJdbcTemplate().queryForList("select id from prediction  where  original_source like '%twitter%'  and created_by_id = 1 limit 1000",
          Integer.class);
      if (pids.isEmpty()) break;
      updateCount += getJdbcTemplate().update(
          "update prediction set created_by_id = 2, created_by = 'Twitter Crawler' where id in (" + StringUtils.join(pids, ',') + ")");
    }
    return updateCount;
  }

  public int fixFacebookCreatedBy() {
    List<Integer> pids;
    int updateCount = 0;
    while (true) {
      pids = getJdbcTemplate().queryForList("select  id from prediction  where  created_by_id = 1 and id  > 15310 limit 1000", Integer.class);
      if (pids.isEmpty()) break;
      updateCount += getJdbcTemplate().update(
          "update prediction set created_by_id = 3, created_by = 'Facebook Crawler' where id in (" + StringUtils.join(pids, ',') + ")");
    }
    return updateCount;
  }
}
