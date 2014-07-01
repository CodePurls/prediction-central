package name.nirav.mp.crawlers;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.core.SecurityContext;

import name.nirav.mp.config.CrawlerConfig;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.utils.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nirav Thaker
 */
public abstract class AbstractCrawler<C extends CrawlerConfig.ICrawlerConfig> {
  protected final Logger            LOGGER = LoggerFactory.getLogger(getClass().getSimpleName());

  protected final PredictionService predictionService;
  protected final C                 config;
  private static final Object       LOCK   = new Object();
  private Timer                     timer;

  public static class CrawlerLog {
    public String sourceId;
    public char   crawlerId;
    public String contentHash;

    public CrawlerLog(char crawlerId, String sourceId, String contentHash) {
      this.crawlerId = crawlerId;
      this.sourceId = sourceId;
      this.contentHash = contentHash;
    }
  }

  protected AbstractCrawler(C config, PredictionService service) {
    this.predictionService = service;
    this.config = config;
  }

  public void start() {
    this.timer = new Timer(getCrawlerUserId() + "-crawler", true);
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        LOGGER.info("scheduling");
        long start = System.nanoTime();
        AbstractCrawler.this.runOnce();
        long duration = System.nanoTime() - start;
        LOGGER.info("done in {}ms", TimeUnit.NANOSECONDS.toMillis(duration));
      }
    }, 30 * 1000, 60 * 1000);
  }

  public SecurityContext getSecurityContext() {
    return new SecurityContext() {
      public Principal getUserPrincipal() {
        return predictionService.getUser(getCrawlerUserId());
      }

      public boolean isUserInRole(String role) {
        return true;
      }

      public boolean isSecure() {
        return true;
      }

      public String getAuthenticationScheme() {
        return "Crawler";
      }
    };
  }

  public void runOnce() {
    List<Prediction> predictions;
    try {
      predictions = search();
    } catch (Exception e) {
      LOGGER.error("Error crawling, ignoring", e);
      return;
    }
    if (predictions == null || predictions.isEmpty()) return;
    int originalSize = predictions.size();
    try {
      List<CrawlerLog> logList = predictions.stream()
          .map((p) -> new CrawlerLog(getCrawlerId(), p.getSourceId(), TextUtils.getContentHash(p.getText())))
          .collect(Collectors.toCollection(ArrayList::new));

      List<CrawlerLog> failed = predictionService.createCrawlerLog(logList);
      if (!failed.isEmpty()) {
        List<Prediction> toRemove = new ArrayList<>();
        for (Prediction p : predictions) {
          for (CrawlerLog failedLog : failed) {
            if (p.getSourceId().equals(failedLog.sourceId)) toRemove.add(p);
          }
        }
        predictions.removeAll(toRemove);
      }
    } catch (Exception e) {
      LOGGER.error("Error creating crawler logs, ignoring.", e);
    }
    LOGGER.info("{} new predictions (de-duped {})", predictions.size(), originalSize - predictions.size());
    try {
      synchronized (LOCK) {
        predictionService.createPredictions(getSecurityContext(), predictions);
      }
    } catch (Exception e) {
      LOGGER.error("Error creating prediction, ignoring.", e);
    }
  }

  public abstract List<Prediction> search();

  public abstract String getCrawlerUserId();

  public abstract char getCrawlerId();

}
