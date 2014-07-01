/**
 * 
 */
package name.nirav.mp.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author nt
 * 
 */
public class WithVotes extends Auditable implements Voted {
  @JsonProperty
  private int     upvotes, downvotes;
  @JsonProperty
  private boolean approved;

  public int getUpvotes() {
    return upvotes;
  }

  public void setUpvotes(int ups) {
    this.upvotes = ups;
  }

  public int getDownvotes() {
    return downvotes;
  }

  public void setDownvotes(int downs) {
    this.downvotes = downs;
  }

  public boolean isApproved() {
    return approved;
  }

  public void setApproved(boolean approved) {
    this.approved = approved;
  }
}
