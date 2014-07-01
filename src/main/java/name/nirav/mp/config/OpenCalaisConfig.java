package name.nirav.mp.config;

import java.time.Duration;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Nirav Thaker
 */
public class OpenCalaisConfig {
  @NotNull
  public Boolean enabled     = Boolean.FALSE;
  @NotEmpty
  @JsonProperty
  public String  licenseKey;
  @NotNull
  @JsonProperty
  public Integer readTimeout = (int) Duration.ofMinutes(5).toMillis();
}
