package name.nirav.mp.service.dto;

/**
 * @author Nirav Thaker
 */
public class PredictionIndexStatus {
  public enum Status {
    UNINDEXED('U'), INDEXED_WITHOUT_ENTITIES('W'), INDEXED('I');
    private final char code;

    Status(char code) {
      this.code = code;
    }

    public static Status fromChar(char ch) {
      switch (ch) {
        case 'W':
          return INDEXED_WITHOUT_ENTITIES;
        case 'I':
          return INDEXED;
      }
      return UNINDEXED;
    }

    public char getCode() {
      return code;
    }
  }

  private int    predictionId;
  private Status indexingStatus;

  public Status getIndexingStatus() {
    return indexingStatus;
  }

  public void setIndexingStatus(Status indexingStatus) {
    this.indexingStatus = indexingStatus;
  }

  public int getPredictionId() {
    return predictionId;
  }

  public void setPredictionId(int predictionId) {
    this.predictionId = predictionId;
  }

}
