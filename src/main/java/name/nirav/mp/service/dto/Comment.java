/**
 * 
 */
package name.nirav.mp.service.dto;

import java.util.List;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author nt
 * 
 */
public class Comment extends WithVotes implements Voted {

  @JsonProperty
  private String        author;

  @JsonProperty
  @NotEmpty(message = ",required,")
  private String        comment;

  @JsonProperty
  private int           id;

  @JsonProperty
  private int           predictionId;

  @JsonProperty
  private List<Comment> children;

  public String getAuthor() {
    return author;
  }

  public List<Comment> getChildren() {
    return children;
  }

  public String getComment() {
    return comment;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getPredictionId() {
    return predictionId;
  }

  public void setPredictionId(int predictionId) {
    this.predictionId = predictionId;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setChildren(List<Comment> children) {
    this.children = children;
  }

}
