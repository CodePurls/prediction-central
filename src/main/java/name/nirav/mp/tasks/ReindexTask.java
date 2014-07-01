package name.nirav.mp.tasks;

import java.io.PrintWriter;

import name.nirav.mp.service.search.IndexingService;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

public class ReindexTask extends Task {
  private IndexingService indexingService;

  public ReindexTask(IndexingService indexingService) {
    super("reindex");
    this.indexingService = indexingService;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    output.print("Starting full re-index");
    int count = indexingService.reindex();
    output.print("Reindex complete, indexed " + count + " docs");
  }

}
