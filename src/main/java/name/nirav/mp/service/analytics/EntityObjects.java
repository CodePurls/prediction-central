package name.nirav.mp.service.analytics;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nirav Thaker
 */
public class EntityObjects {
  public static class SocialTag {
    public String tag;

    public SocialTag(String tag) {
      this.tag = tag;
    }
  }

  public static class Entity {
    public String type, name;

    public Entity(String type, String name) {
      this.type = type;
      this.name = name;
    }
  }

  public static class Topic extends SocialTag {
    public Topic(String tag) {
      super(tag);
    }
  }

  private List<SocialTag> socialTags = new ArrayList<>();
  private List<Entity>    entities   = new ArrayList<>();
  private List<Topic>     topics     = new ArrayList<>();

  public List<SocialTag> getSocialTags() {
    return socialTags;
  }

  public void setSocialTags(List<SocialTag> socialTags) {
    this.socialTags = socialTags;
  }

  public List<Entity> getEntities() {
    return entities;
  }

  public void setEntities(List<Entity> entities) {
    this.entities = entities;
  }

  public List<Topic> getTopics() {
    return topics;
  }

  public void setTopics(List<Topic> topics) {
    this.topics = topics;
  }

}
