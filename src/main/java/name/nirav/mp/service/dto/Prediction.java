/**
 * 
 */
package name.nirav.mp.service.dto;

import java.util.List;

import name.nirav.mp.service.analytics.EntityObjects;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author nt
 * 
 */
public class Prediction extends WithVotes implements Voted {

  public enum PredictionType {
    quote, prediction
  }

  @JsonProperty
  private int           id;

  @JsonProperty
  private String        title;

  @NotEmpty(message = ",required,")
  @JsonProperty
  private String        text;

  @JsonProperty
  private String        type;

  @JsonProperty
  private long          time;

  @JsonProperty
  private String        location;

  @JsonProperty
  private String        about;

  @JsonProperty
  private String        reason;

  @JsonProperty
  private String        tags;

  @JsonProperty(value = "original_author")
  private String        sourceAuthor;

  @JsonProperty
  private String        sourceId;

  @JsonProperty(value = "original_source")
  private String        sourceRef;

  @JsonProperty
  private List<Comment> comments;

  @JsonProperty
  private int           commentCount;

  @JsonProperty
  private boolean       hasMore = false;

  private Integer       random;

  private Source        source;

  private EntityObjects entityObjects;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long predictionTimestamp) {
    this.time = predictionTimestamp;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getAbout() {
    return about;
  }

  public void setAbout(String actor) {
    this.about = actor;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public List<Comment> getComments() {
    return comments;
  }

  public void setComments(List<Comment> comments) {
    this.comments = comments;
  }

  public String getSourceAuthor() {
    return sourceAuthor;
  }

  public void setSourceAuthor(String originalAuthor) {
    this.sourceAuthor = originalAuthor;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public void setSourceRef(String originalSource) {
    this.sourceRef = originalSource;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getRandom() {
    return random;
  }

  public void setRandom(Integer random) {
    this.random = random;
  }

  public Integer getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(Integer commentCount) {
    this.commentCount = commentCount;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source source) {
    this.source = source;
  }

  public EntityObjects getEntityObjects() {
    return entityObjects;
  }

  public void setEntityObjects(EntityObjects entityObjects) {
    this.entityObjects = entityObjects;
  }
}
