package name.nirav.mp.views;

import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.User;

import com.yammer.dropwizard.views.View;

public class PredictionView extends View {

  public static class Composite {
    private final Prediction prediction;
    private final User       currentUser;

    public Composite(Prediction p, User currentUser) {
      this.prediction = p;
      this.currentUser = currentUser;
    }

    public Prediction getPrediction() {
      return prediction;
    }

    public User getCurrentUser() {
      return currentUser;
    }
  }

  private final Composite model;

  public PredictionView(Prediction p, User currentUser) {
    super("prediction.ftl");
    this.model = new Composite(p, currentUser);
  }

  public Composite getModel() {
    return model;
  }

}
