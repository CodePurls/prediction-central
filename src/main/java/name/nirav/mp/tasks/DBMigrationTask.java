package name.nirav.mp.tasks;

import static java.lang.String.format;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

public class DBMigrationTask extends Task {

  private String dbURL;
  private String context;

  protected DBMigrationTask(String name) {
    super(name);
  }

  public DBMigrationTask(String dbURL, String context) {
    this("migrate-db");
    this.dbURL = dbURL;
    this.context = context;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    try {
      Connection c = DriverManager.getConnection(dbURL, "sa", "");
      Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
      String changeLogLocation = format("db/%s/changelog.xml", db.getShortName());
      Liquibase lq = new Liquibase(changeLogLocation, new ClassLoaderResourceAccessor(DBMigrationTask.class.getClassLoader()), db);
      lq.update(context);
      db.commit();
      c.close();
    } catch (LiquibaseException | SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
