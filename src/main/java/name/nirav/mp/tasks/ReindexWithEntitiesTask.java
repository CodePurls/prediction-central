package name.nirav.mp.tasks;

import java.io.PrintWriter;

import name.nirav.mp.service.search.IndexingService;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

/**
 * @author Nirav Thaker
 */
public class ReindexWithEntitiesTask extends Task {

  private final IndexingService indexingService;

  public ReindexWithEntitiesTask(IndexingService indexingService) {
    super("reindex-entities");
    this.indexingService = indexingService;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    indexingService.reindexEntities();
    output.println("Reindex with entities scheduled.");
  }
}
