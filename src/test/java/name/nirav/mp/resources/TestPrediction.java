package name.nirav.mp.resources;

import java.util.List;

public class TestPrediction {

  public class Comment {

  }

  public int           id, ups, downs;
  public String        text;
  public List<Comment> comments;

  public TestPrediction() {
  }

  public TestPrediction(String t) {
    this(0, t);
  }

  public TestPrediction(int id, String string) {
    this.id = id;
    this.text = string;
  }
}