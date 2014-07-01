package name.nirav.mp.service.monitoring;

import java.util.HashMap;
import java.util.Map;

import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import com.yammer.metrics.reporting.JmxReporter;

/**
 * @author Nirav Thaker
 */
public class MonitoringService {
  private static final MetricsRegistry     registry = new MetricsRegistry();
  static {
    JmxReporter.startDefault(registry);
  }
  private static final Map<Integer, Timer> timerMap = new HashMap<>();

  public static TimerContext time(Class<?> klass, String name) {
    int hash = (klass.toString() + "#" + name).hashCode();
    Timer timer = timerMap.get(hash);
    if (timer == null) {
      timer = registry.newTimer(klass, name);
      timerMap.put(hash, timer);
    }
    return timer.time();
  }
}
