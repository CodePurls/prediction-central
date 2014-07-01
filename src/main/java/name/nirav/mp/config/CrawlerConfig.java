package name.nirav.mp.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Nirav Thaker
 */
public class CrawlerConfig {
  public GooglePlusCrawlerConfig getGoogleplus() {
    return googleplus;
  }

  public void setGoogleplus(GooglePlusCrawlerConfig googleplus) {
    this.googleplus = googleplus;
  }

  public interface ICrawlerConfig {
    Boolean getEnabled();
  }

  @Valid
  @NotNull
  @JsonProperty
  private TwitterCrawlerConfig    twitter;

  @Valid
  @NotNull
  @JsonProperty
  private FacebookCrawlerConfig   facebook;

  @Valid
  @NotNull
  @JsonProperty
  private GooglePlusCrawlerConfig googleplus;

  public TwitterCrawlerConfig getTwitter() {
    return twitter;
  }

  public void setTwitter(TwitterCrawlerConfig twitter) {
    this.twitter = twitter;
  }

  public FacebookCrawlerConfig getFacebook() {
    return facebook;
  }

  public void setFacebook(FacebookCrawlerConfig facebook) {
    this.facebook = facebook;
  }

  public static class AbstractCrawlerConfig implements ICrawlerConfig {
    @NotNull
    @JsonProperty
    protected Boolean enabled = Boolean.FALSE;

    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean val) {
      this.enabled = val;
    }

  }

  public static class FacebookCrawlerConfig extends AbstractCrawlerConfig implements ICrawlerConfig {
    @NotEmpty
    @JsonProperty
    private String[] searchQueries = { "prediction", "predictions" };

    @NotEmpty
    @JsonProperty
    private String   appId;
    @NotEmpty
    @JsonProperty
    private String   appSecret;

    @NotEmpty
    @JsonProperty
    private String   oauthToken;

    public String getOauthToken() {
      return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
      this.oauthToken = oauthToken;
    }

    public String[] getSearchQueries() {
      return searchQueries;
    }

    public void setSearchQueries(String[] searchQueries) {
      this.searchQueries = searchQueries;
    }

    public String getAppSecret() {
      return appSecret;
    }

    public void setAppSecret(String appSecret) {
      this.appSecret = appSecret;
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }
  }

  public static class TwitterCrawlerConfig extends AbstractCrawlerConfig implements ICrawlerConfig {
    @NotEmpty
    @JsonProperty
    private String searchQuery = "prediction OR predictions OR #prediction";
    @NotEmpty
    @JsonProperty
    private String consumerKey;
    @NotEmpty
    @JsonProperty
    private String consumerSercret;

    public String getConsumerSercret() {
      return consumerSercret;
    }

    public void setConsumerSercret(String consumerSercret) {
      this.consumerSercret = consumerSercret;
    }

    public String getConsumerKey() {
      return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
      this.consumerKey = consumerKey;
    }

    public String getSearchQuery() {
      return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
      this.searchQuery = searchQuery;
    }
  }

  public class GooglePlusCrawlerConfig extends AbstractCrawlerConfig implements ICrawlerConfig {
    @NotEmpty
    @JsonProperty
    private String searchQuery = "prediction OR predictions";
    @NotEmpty
    @JsonProperty
    private String serviceAccountEmail;
    @NotEmpty
    @JsonProperty
    private String serviceAccountPrivateKeyFile;

    public String getSearchQuery() {
      return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
      this.searchQuery = searchQuery;
    }

    public String getServiceAccountEmail() {
      return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
      this.serviceAccountEmail = serviceAccountEmail;
    }

    public String getServiceAccountPrivateKeyFile() {
      return serviceAccountPrivateKeyFile;
    }

    public void setServiceAccountPrivateKeyFile(String serviceAccountPrivateKeyFile) {
      this.serviceAccountPrivateKeyFile = serviceAccountPrivateKeyFile;
    }
  }
}
