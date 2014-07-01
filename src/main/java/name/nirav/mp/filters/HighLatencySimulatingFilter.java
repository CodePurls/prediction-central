package name.nirav.mp.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author Nirav Thaker
 */
public class HighLatencySimulatingFilter implements Filter {
  private final RateLimiter limiter;

  public HighLatencySimulatingFilter(double rps) {
    this.limiter = RateLimiter.create(rps);

  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    limiter.acquire();
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {

  }
}
