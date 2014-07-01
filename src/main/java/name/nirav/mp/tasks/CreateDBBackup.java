package name.nirav.mp.tasks;

import java.io.PrintWriter;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

/**
 * @author Nirav Thaker
 */
public class CreateDBBackup extends Task {

  public CreateDBBackup() {
    super("Backup Database task");
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
  }
}
