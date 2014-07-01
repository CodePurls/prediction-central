package name.nirav.mp.service.analytics;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import mx.bigdata.jcalais.CalaisClient;
import mx.bigdata.jcalais.CalaisConfig;
import mx.bigdata.jcalais.CalaisException;
import mx.bigdata.jcalais.CalaisObject;
import mx.bigdata.jcalais.CalaisResponse;
import mx.bigdata.jcalais.rest.CalaisRestClient;
import name.nirav.mp.config.OpenCalaisConfig;
import name.nirav.mp.db.PredictionDB;
import name.nirav.mp.utils.TextUtils;
import name.nirav.mp.utils.Tuple;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author Nirav Thaker
 */
public class EntityExtractionService {
  private static final Logger LOG               = LoggerFactory.getLogger("EntityExtractionService");
  private static final int    MAX_CALLS_PER_DAY = 50_000;
  private final CalaisClient  client;
  private final ObjectMapper  mapper            = new ObjectMapper();
  private final RateLimiter   rateLimiter;
  private OpenCalaisConfig    config;
  private CalaisConfig        calaisConfig;
  private final PredictionDB  db;
  private int                 callCount;

  public static class UnsupportedLanguageException extends RuntimeException {
    private static final long serialVersionUID = -5818167473924785080L;
  }

  public EntityExtractionService(OpenCalaisConfig config, PredictionDB db) {
    this.config = config;
    this.calaisConfig = new CalaisConfig();
    this.calaisConfig.set(CalaisConfig.ConnParam.READ_TIMEOUT, config.readTimeout);
    this.calaisConfig.set(CalaisConfig.ConnParam.CONNECT_TIMEOUT, config.readTimeout);
    this.db = db;
    this.client = new CalaisRestClient(config.licenseKey);
    this.rateLimiter = RateLimiter.create(4);
    Timer timer = new Timer("Calais-ban-reset");
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() >= 23) {
          LOG.info("It is close to midnight {}, resetting callCount", now.getHour());
          callCount = 0;
        }
      }
    }, 1000, TimeUnit.MINUTES.toMillis(30));
  }

  public boolean isEnabled() {
    return config.enabled;
  }

  public EntityObjects getEntities(String text) throws UnsupportedLanguageException {
    text = StringUtils.abbreviate(text, 100 * 1024);
    EntityObjects objects = null;
    CalaisResponse response;
    try {
      Tuple<String, String> log = db.lookupWebServiceCall(TextUtils.getMD5(text));
      if (log == null) {
        rateLimiter.acquire();
        if (callCount >= MAX_CALLS_PER_DAY) {
          LOG.warn("Reached max calais calls per day [{} of {}], preventing call", callCount, MAX_CALLS_PER_DAY);
          return objects;
        }
        try {
          response = client.analyze(text, calaisConfig);
          LOG.info("OpenCalais web service call complete, count {}", callCount);
        } catch (CalaisException e) {
          if (e.getMessage().contains("supported languages")) {
            throw new UnsupportedLanguageException();
          } else {
            throw new IOException(e);
          }
        }
        callCount++;
        Tuple<String, String> tuple = Tuple.of(TextUtils.getMD5(text), response.getPayload());
        db.logWebServiceCall(tuple);
      } else {
        String payload = log.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(payload, Map.class);
        response = CalaisRestClient.processResponse(map, payload);
      }
    } catch (IOException e) {
      LOG.error("Error calling cailas", e);
      return objects;
    }
    objects = new EntityObjects();
    for (CalaisObject entity : response.getEntities()) {
      objects.getEntities().add(new EntityObjects.Entity(entity.getField("_type"), entity.getField("name")));
    }
    for (CalaisObject topic : response.getTopics()) {
      objects.getTopics().add(new EntityObjects.Topic(topic.getField("categoryName")));
    }
    for (CalaisObject tags : response.getSocialTags()) {
      objects.getSocialTags().add(new EntityObjects.SocialTag(tags.getField("name")));
    }
    return objects;
  }

}
