package name.nirav.mp.crawlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.nirav.mp.config.CrawlerConfig.FacebookCrawlerConfig;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.Source;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.types.Post;

/**
 * @author Nirav Thaker
 */
public class FacebookCrawler extends AbstractCrawler<FacebookCrawlerConfig> {
  private FacebookClient fbClient;

  public FacebookCrawler(FacebookCrawlerConfig config, PredictionService predictionService) {
    super(config, predictionService);
    this.fbClient = new DefaultFacebookClient();
  }

  public String getCrawlerUserId() {
    return "facebook-crawler@prediction-central.com";
  }

  public char getCrawlerId() {
    return 'F';
  }

  public List<Prediction> search() {
    FacebookClient.AccessToken token = fbClient.obtainAppAccessToken(config.getAppId(), config.getAppSecret());
    this.fbClient = new DefaultFacebookClient(token.getAccessToken());
    List<Prediction> probablePredictions = Collections.emptyList();
    try {
      List<Post> allPredictions = new ArrayList<>(100 * 2);
      Set<String> duplicateAuthors = new HashSet<>();
      allPredictions.addAll(getResults("prediction"));
      allPredictions.addAll(getResults("predictions"));
      probablePredictions = new ArrayList<>(allPredictions.size());
      for (Post p : allPredictions) {
        if (!p.getTo().isEmpty() || p.getMessage() == null) continue;
        if (duplicateAuthors.contains(p.getFrom().getName())) continue;
        duplicateAuthors.add(p.getFrom().getName());
        Prediction pred = new Prediction();
        pred.setSourceId(p.getId());
        pred.setApproved(false);
        pred.setSource(Source.FACEBOOK);
        pred.setSourceAuthor(p.getFrom().getName());
        pred.setSourceRef(p.getLink());
        pred.setCreatedByUser("FacebookCrawler");
        pred.setCreateTimestamp(p.getCreatedTime().getTime());
        pred.setUpdateTimestamp(p.getUpdatedTime().getTime());
        if (p.getPlace() != null) {
          pred.setLocation(p.getPlace().getLocationAsString());
        }
        pred.setText(p.getMessage());
        String title = p.getCaption() == null ? "" : p.getCaption().trim();
        if (!title.isEmpty() && !title.startsWith("http") && !title.startsWith("www") && !title.endsWith(".com") && !title.endsWith(".org")) {
          pred.setTitle(p.getCaption());
        }
        probablePredictions.add(pred);
      }
    } catch (Exception e) {
      LOGGER.error("Error crawling facebook", e);
    }
    return probablePredictions;
  }

  public List<Post> getResults(String kw) {
    return fbClient.fetchConnection("search", Post.class, Parameter.with("q", kw), Parameter.with("type", "post"), Parameter.with("limit", 100))
        .getData();
  }
}
