package name.nirav.mp.crawlers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import name.nirav.mp.config.CrawlerConfig.GooglePlusCrawlerConfig;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.Source;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.PlusScopes;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.ActivityFeed;

/**
 * @author Nirav Thaker
 */
public class GooglePlusCrawler extends AbstractCrawler<GooglePlusCrawlerConfig> {
  private static final JacksonFactory JSON_FACTORY     = JacksonFactory.getDefaultInstance();
  private static final String         APPLICATION_NAME = "Prediction-Central/1.0";
  private final HttpTransport         transport;
  private final Plus                  plus;

  public GooglePlusCrawler(GooglePlusCrawlerConfig config, PredictionService service) {
    super(config, service);
    try {
      transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = new GoogleCredential.Builder().setTransport(transport).setJsonFactory(JSON_FACTORY)
          .setServiceAccountId(config.getServiceAccountEmail()).setServiceAccountScopes(Collections.singleton(PlusScopes.PLUS_ME))
          .setServiceAccountPrivateKeyFromP12File(new File(config.getServiceAccountPrivateKeyFile())).build();
      plus = new Plus.Builder(transport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Prediction> search() {
    List<Prediction> predictions = new ArrayList<>(20);
    try {
      Plus.Activities.Search search = plus.activities().search("prediction");
      search.setMaxResults(20L);
      search.setOrderBy("recent");
      ActivityFeed activityFeed = search.execute();
      List<Activity> items = activityFeed.getItems();
      int page = 0;
      while (items != null && page++ < 2) {
        for (Activity activity : items) {
          if (activity.getVerb().equalsIgnoreCase("share")) continue;
          Activity.PlusObject plusObject = activity.getObject();
          Prediction p = new Prediction();
          p.setSourceId(activity.getId());
          p.setSource(Source.GOOGLEPLUS);
          p.setTitle(activity.getTitle());
          if (activity.getLocation() != null) p.setLocation(activity.getLocation().toString());
          p.setSourceAuthor(activity.getActor().getDisplayName());
          p.setSourceRef(activity.getUrl());
          p.setCreatedByUser("GoogleplusCrawler");
          p.setCreateTimestamp(activity.getPublished().getValue());
          p.setUpdateTimestamp(activity.getUpdated().getValue());
          p.setText(plusObject.getContent());
          List<Activity.PlusObject.Attachments> attachments = plusObject.getAttachments();
          if (attachments != null) {
            for (Activity.PlusObject.Attachments a : attachments) {
              if (a.getDisplayName() != null && !a.getDisplayName().isEmpty()) {
                p.setTitle(a.getDisplayName());
              }
              if (a.getContent() != null && !a.getContent().isEmpty()) {
                p.setText(a.getContent());
              }
            }
          }
          predictions.add(p);
        }
        // We will know we are on the last page when the next page token is null.
        // If this is the case, break.
        if (activityFeed.getNextPageToken() == null) {
          break;
        }

        // Prepare to request the next page of activities
        search.setPageToken(activityFeed.getNextPageToken());
        search.setOrderBy("best");
        // Execute and process the next page request
        activityFeed = search.execute();
        items = activityFeed.getItems();
        LOGGER.info("Searching page {} of 2", page);
      }
    } catch (Exception e) {
      if (e instanceof GoogleJsonResponseException) {
        GoogleJsonResponseException je = ((GoogleJsonResponseException) e);
        if (je.getDetails().getCode() == 403) {
          if (je.getDetails().getMessage().contains("Rate Limit Exceeded")) {
            try {
              LOGGER.info("Rate limit exceeded sleeping for 1 hour");
              Thread.sleep(TimeUnit.HOURS.toMillis(1));
            } catch (Exception e1) {
              e1.printStackTrace();
            }

          }
        }
      }
      LOGGER.error("Error crawling GooglePlus", e);
    }
    return predictions;
  }

  @Override
  public String getCrawlerUserId() {
    return "googleplus-crawler@prediction-central.com";
  }

  @Override
  public char getCrawlerId() {
    return 'G';
  }
}
