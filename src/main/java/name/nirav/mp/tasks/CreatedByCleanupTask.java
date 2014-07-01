package name.nirav.mp.tasks;

import java.io.PrintWriter;

import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.search.IndexingService;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

public class CreatedByCleanupTask extends Task {
  private final PredictionService service;
  private IndexingService         indexingService;

  public CreatedByCleanupTask(PredictionService service, IndexingService indexingService) {
    super("createdby-cleanup");
    this.service = service;
    this.indexingService = indexingService;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    output.println("Starting createby-cleanup task");
    int twitterFixes = service.fixTwitterCreatedBy();
    output.println("Updated " + twitterFixes + " broken records for twitter");
    output.flush();
    output.println("Updated " + twitterFixes + " broken records for facebook");
    output.println("Triggering reindex");
    output.flush();
    indexingService.reindex();

  }
}
