package name.nirav.mp;

import java.io.IOException;
import java.util.Iterator;

import name.nirav.mp.config.MPConfiguration;
import name.nirav.mp.crawlers.FacebookCrawler;
import name.nirav.mp.crawlers.GooglePlusCrawler;
import name.nirav.mp.crawlers.TwitterCrawler;
import name.nirav.mp.db.PredictionDB;
import name.nirav.mp.filters.HighLatencySimulatingFilter;
import name.nirav.mp.filters.RateLimitingFilter;
import name.nirav.mp.filters.RealIPFilter;
import name.nirav.mp.filters.TrackingFilter;
import name.nirav.mp.health.MPHealthCheck;
import name.nirav.mp.rest.PredictionResource;
import name.nirav.mp.rest.PredictionViewResource;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.analytics.EntityExtractionService;
import name.nirav.mp.service.search.IndexingService;
import name.nirav.mp.service.search.SearchService;
import name.nirav.mp.tasks.ApproveAllPredictionsTask;
import name.nirav.mp.tasks.CreatedByCleanupTask;
import name.nirav.mp.tasks.DBMigrationTask;
import name.nirav.mp.tasks.DropUnapprovedPostsTask;
import name.nirav.mp.tasks.ImportDataTask;
import name.nirav.mp.tasks.ReindexTask;
import name.nirav.mp.tasks.ReindexWithEntitiesTask;
import name.nirav.mp.utils.PredictionExceptionMapper;
import name.nirav.mp.utils.ServerExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.TwitterException;

import com.sun.jersey.multipart.impl.MultiPartReaderServerSide;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.InvalidEntityExceptionMapper;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;
import com.yammer.dropwizard.views.ViewBundle;

public class Main extends Service<MPConfiguration> {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getSimpleName());

  @Override
  public void initialize(Bootstrap<MPConfiguration> bootstrap) {
    bootstrap.setName("mp");
    bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    bootstrap.addBundle(new ViewBundle());
  }

  public static void log(String s, Exception e) {
    LOGGER.error(s, e);
  }

  @Override
  public void run(MPConfiguration configuration, Environment environment) throws ClassNotFoundException, IOException {
    Iterator<Object> iterator = environment.getJerseyResourceConfig().getSingletons().iterator();
    while (iterator.hasNext()) {
      Object s = iterator.next();
      if (s.getClass().equals(InvalidEntityExceptionMapper.class) || s instanceof LoggingExceptionMapper) iterator.remove();
    }
    final String defaultName = configuration.getAppName();
    environment.addHealthCheck(new MPHealthCheck(defaultName));
    PredictionDB predictionDB = new PredictionDB(configuration.getDatabase());
    EntityExtractionService entityExtractionService = new EntityExtractionService(configuration.getCalaisConfig(), predictionDB);
    IndexingService indexingService = new IndexingService(configuration.getSearch(), predictionDB, entityExtractionService);
    SearchService searchService = new SearchService(indexingService);
    PredictionService predictionService = new PredictionService(predictionDB, searchService);

    environment.addResource(new PredictionResource(predictionService));
    environment.addResource(new PredictionViewResource(predictionService));
    environment.addTask(new DBMigrationTask(configuration.getDatabase().getUrl(), configuration.getLiquibaseContext()));
    environment.addTask(new ReindexTask(indexingService));
    environment.addTask(new DropUnapprovedPostsTask(predictionService));
    environment.addTask(new ApproveAllPredictionsTask(predictionService));
    environment.addTask(new ImportDataTask(predictionService));
    environment.addTask(new ReindexWithEntitiesTask(indexingService));
    environment.addTask(new CreatedByCleanupTask(predictionService, indexingService));

    environment.addProvider(new PredictionExceptionMapper());
    environment.addProvider(new ServerExceptionMapper<Throwable>() {
    });
    environment.addProvider(MultiPartReaderServerSide.class);

    if (configuration.getRateLimits().getSimulateHighLatency()) {
      environment.addFilter(new HighLatencySimulatingFilter(.5), "/*");
    }
    environment.addFilter(new RealIPFilter(), "/*");
    environment.addFilter(new RateLimitingFilter(configuration.getRateLimits()), "/api/*");
    environment.addFilter(new TrackingFilter(predictionService), "/api/*");

    if (configuration.getCrawlers().getTwitter().getEnabled()) {
      try {
        new TwitterCrawler(configuration.getCrawlers().getTwitter(), predictionService).start();
      } catch (TwitterException e) {
        log("TwitterCrawler failed to initialize", e);
      }
    }
    if (configuration.getCrawlers().getFacebook().getEnabled()) {
      try {
        new FacebookCrawler(configuration.getCrawlers().getFacebook(), predictionService).start();
      } catch (Exception e) {
        log("FacebookCrawler failed to initialize", e);
      }
    }
    if (configuration.getCrawlers().getGoogleplus().getEnabled()) {
      try {
        new GooglePlusCrawler(configuration.getCrawlers().getGoogleplus(), predictionService).start();
      } catch (Exception e) {
        log("GoogleplusCrawler failed to initialize", e);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    new Main().run(args);
  }

}