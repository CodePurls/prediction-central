package name.nirav.mp.health;

import com.yammer.metrics.core.HealthCheck;

public class MPHealthCheck extends HealthCheck {

  public MPHealthCheck(String template) {
    super(template);
  }

  @Override
  protected Result check() throws Exception {
    // final String saying = String.format(getName(), "TEST");
    // if (!saying.contains("TEST")) {
    // return Result.unhealthy("template doesn't include a name");
    // }
    return Result.healthy();
  }
}