package name.nirav.mp.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RateLimitConfiguration {
  @Valid
  @NotNull
  @JsonProperty
  private Integer postLimit;

  @Valid
  @NotNull
  @JsonProperty
  private Integer deleteLimit;

  @Valid
  @NotNull
  @JsonProperty
  private Integer putLimit;

  @Valid
  @NotNull
  @JsonProperty
  private Integer getLimit;

  @Valid
  @NotNull
  @JsonProperty
  private Boolean simulateHighLatency = Boolean.FALSE;

  public Integer getPostLimit() {
    return postLimit;
  }

  public void setPostLimit(Integer postLimit) {
    this.postLimit = postLimit;
  }

  public Integer getDeleteLimit() {
    return deleteLimit;
  }

  public void setDeleteLimit(Integer deleteLimit) {
    this.deleteLimit = deleteLimit;
  }

  public Integer getPutLimit() {
    return putLimit;
  }

  public void setPutLimit(Integer putLimit) {
    this.putLimit = putLimit;
  }

  public Integer getGetLimit() {
    return getLimit;
  }

  public void setGetLimit(Integer getLimit) {
    this.getLimit = getLimit;
  }

  public Boolean getSimulateHighLatency() {
    return simulateHighLatency;
  }

  public void setSimulateHighLatency(Boolean simulateHighLatency) {
    this.simulateHighLatency = simulateHighLatency;
  }
}
