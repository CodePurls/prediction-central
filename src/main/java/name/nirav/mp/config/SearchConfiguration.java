package name.nirav.mp.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.lucene.util.Version;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchConfiguration {
  @Valid
  @NotNull
  @JsonProperty
  private String indexDir;

  @Valid
  @NotNull
  @JsonProperty
  private String version = Version.LUCENE_44.name();

  public String getIndexDir() {
    return indexDir;
  }

  public void setIndexDir(String indexDir) {
    this.indexDir = indexDir;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

}
