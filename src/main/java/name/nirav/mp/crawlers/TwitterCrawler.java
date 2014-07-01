package name.nirav.mp.crawlers;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import name.nirav.mp.config.CrawlerConfig.TwitterCrawlerConfig;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.Source;

import org.apache.commons.lang.StringUtils;

import twitter4j.HashtagEntity;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;

/**
 * @author Nirav Thaker
 */
public class TwitterCrawler extends AbstractCrawler<TwitterCrawlerConfig> {
  private final Twitter twitter;
  private final String  query;

  private long          lastSeenTweetId = -1;

  public TwitterCrawler(TwitterCrawlerConfig config, PredictionService predictionService) throws TwitterException {
    super(config, predictionService);
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.setDebugEnabled(false);
    builder.setUseSSL(true);
    builder.setApplicationOnlyAuthEnabled(true);
    builder.setOAuthConsumerKey(config.getConsumerKey()).setOAuthConsumerSecret(config.getConsumerSercret());
    this.query = config.getSearchQuery();
    TwitterFactory tf = new TwitterFactory(builder.build());
    this.twitter = tf.getInstance();
    OAuth2Token token = twitter.getOAuth2Token();
    if (token != null) {
      LOGGER.info("[TwitterClientUtils] Token type = " + token.getTokenType());
    }
  }

  public List<Prediction> search() {
    List<Prediction> probablePredictions = Collections.emptyList();
    try {
      Query query = new Query(this.query);
      query.setCount(100);
      if (lastSeenTweetId > 0) query.setSinceId(lastSeenTweetId);
      query.setResultType("recent");
      QueryResult result;
      result = twitter.search(query);
      List<Status> tweets = result.getTweets();
      probablePredictions = new ArrayList<>(tweets.size());
      for (Status tweet : tweets) {
        lastSeenTweetId = Math.max(lastSeenTweetId, tweet.getId());
        if (tweet.isRetweet() || tweet.getInReplyToStatusId() > 0 || tweet.getText().length() < 100) continue;
        final Prediction p = new Prediction();
        p.setSourceId(Long.toString(tweet.getId()));
        p.setSource(Source.TWITTER);
        p.setSourceAuthor(tweet.getUser().getScreenName());
        p.setSourceRef(format("https://twitter.com/%s/status/%d", p.getSourceAuthor(), tweet.getId()));
        p.setCreatedByUser("TwitterCrawler");
        p.setCreateTimestamp(tweet.getCreatedAt().getTime());
        if (tweet.getPlace() != null) {
          p.setLocation(tweet.getPlace().getFullName());
        }
        p.setText(tweet.getText());
        Arrays.stream(tweet.getURLEntities()).forEach((u) -> {
          p.setText(p.getText().replace(u.getURL(), u.getExpandedURL()).replace("#", ""));
        });
        Arrays.stream(tweet.getHashtagEntities()).forEach(t -> p.setText(p.getText().replace("#" + t.getText(), "")));
        Optional<String> tags = Arrays.stream(tweet.getHashtagEntities()).map(HashtagEntity::getText).reduce((x, y) -> x + "," + y);
        p.setTags(tags.orElse(""));

        p.setTitle(StringUtils.abbreviate(p.getText(), 64));
        probablePredictions.add(p);
      }
    } catch (TwitterException te) {
      LOGGER.error("Error crawling twitter", te);
    }
    return probablePredictions;
  }

  public String getCrawlerUserId() {
    return "twitter-crawler@prediction-central.com";
  }

  public char getCrawlerId() {
    return 'T';
  }
}
