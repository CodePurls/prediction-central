package name.nirav.mp.tasks;

import java.io.PrintWriter;

import name.nirav.mp.service.PredictionService;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

/**
 * @author Nirav Thaker
 */
public class DropUnapprovedPostsTask extends Task {
  private final PredictionService predictionService;

  public DropUnapprovedPostsTask(PredictionService predictionService) {
    super("drop-unapproved");
    this.predictionService = predictionService;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    predictionService.dropUnapprovedPredictions();
  }
}
