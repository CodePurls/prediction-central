package name.nirav.mp.service.search;

import java.util.EnumSet;

/**
 * @author Nirav Thaker
 */
public interface Schema {

  public enum PredictionDoc implements Schema {
    ID("id"), CREATED_ON("created_on"), CREATED_BY("created_by"), TIME("time"), TITLE("title"), TEXT("text"), LOCATION("location"), ACTOR("actor"), REASON(
        "reason"), SOURCE_AUTHOR("sourceAuthor"), SOURCE_REF("sourceRef");
    public static final EnumSet<PredictionDoc> SUGGEST_FIELDS = EnumSet.of(TITLE, TEXT, SOURCE_AUTHOR, LOCATION, REASON, ACTOR);
    public static final EnumSet<PredictionDoc> ALL            = EnumSet.allOf(PredictionDoc.class);
    private final String                       fieldName;

    PredictionDoc(String fieldName) {
      this.fieldName = fieldName;
    }

    public String fieldName() {
      return fieldName;
    }

    public static PredictionDoc fromFieldName(String fieldName) {
      for (PredictionDoc predictionDoc : ALL) {
        if (predictionDoc.fieldName.equals(fieldName)) { return predictionDoc; }
      }
      throw new IllegalArgumentException("no such field " + fieldName);
    }
  }

  public enum EntityDoc implements Schema {
    ID("id"), SOCIAL_TAG("socialTag"), TOPIC("topic"),
    /* Entities */
    PRODUCT("Product"), CITY("City"), COMPANY("Company"), COUNTRY("Country"), INDUSTRY_TERM("IndustryTerm"), MOVIE("Movie"), MUSIC_GROUP("MusicGroup"), ORGANIZATION(
        "Organization"), PERSON("Person"), STATE("ProvinceOrState"), SPORTS_EVENT("SportsEvent"), SPORTS_GAME("SportsGame"), TVSHOW("TVShow"), TECHNOLOGY(
        "Technology"), URL("URL")
    /**/
    ;
    public static final EnumSet<EntityDoc> ALL = EnumSet.allOf(EntityDoc.class);
    private final String                   fieldName;

    EntityDoc(String fieldName) {
      this.fieldName = fieldName;
    }

    public String fieldName() {
      return fieldName;
    }

    public static EntityDoc fromFieldName(String fieldName) {
      for (EntityDoc entityDoc : ALL) {
        if (entityDoc.fieldName.equals(fieldName)) { return entityDoc; }
      }
      throw new IllegalArgumentException("no such field " + fieldName);
    }
  }

  String fieldName();

  default boolean isDynamic() {
    return false;
  }
}
