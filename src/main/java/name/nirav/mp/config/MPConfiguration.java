package name.nirav.mp.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

public class MPConfiguration extends Configuration {

  @NotEmpty
  @JsonProperty
  private String                 liquibaseContext;

  @NotEmpty
  @JsonProperty
  private String                 appName;

  @Valid
  @NotNull
  @JsonProperty
  private DatabaseConfiguration  database     = new DatabaseConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private SearchConfiguration    search       = new SearchConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private RateLimitConfiguration rateLimits   = new RateLimitConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private OpenCalaisConfig       calaisConfig = new OpenCalaisConfig();

  @Valid
  @NotNull
  @JsonProperty
  private CrawlerConfig          crawlers     = new CrawlerConfig();

  public String getLiquibaseContext() {
    return liquibaseContext;
  }

  public String getAppName() {
    return appName;
  }

  public DatabaseConfiguration getDatabase() {
    return database;
  }

  public SearchConfiguration getSearch() {
    return search;
  }

  public RateLimitConfiguration getRateLimits() {
    return rateLimits;
  }

  public void setRateLimits(RateLimitConfiguration rateLimits) {
    this.rateLimits = rateLimits;
  }

  public CrawlerConfig getCrawlers() {
    return crawlers;
  }

  public void setCrawlers(CrawlerConfig crawlers) {
    this.crawlers = crawlers;
  }

  public OpenCalaisConfig getCalaisConfig() {
    return calaisConfig;
  }

  public void setCalaisConfig(OpenCalaisConfig calaisConfig) {
    this.calaisConfig = calaisConfig;
  }
}