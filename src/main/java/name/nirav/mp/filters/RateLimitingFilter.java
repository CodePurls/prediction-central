package name.nirav.mp.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import name.nirav.mp.config.RateLimitConfiguration;

import com.google.common.util.concurrent.RateLimiter;

public class RateLimitingFilter implements Filter {

  private final Map<String, RateLimiter> rateLimiters;

  public RateLimitingFilter(RateLimitConfiguration rateLimits) {
    this.rateLimiters = new HashMap<>();
    this.rateLimiters.put("GET", RateLimiter.create(rateLimits.getGetLimit()));
    this.rateLimiters.put("POST", RateLimiter.create(rateLimits.getPostLimit()));
    this.rateLimiters.put("PUT", RateLimiter.create(rateLimits.getPutLimit()));
    this.rateLimiters.put("DELETE", RateLimiter.create(rateLimits.getDeleteLimit()));
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;
    String method = req.getMethod().toUpperCase();
    RateLimiter limiter = rateLimiters.get(method);
    if (limiter == null) {
      resp.sendError(HttpServletResponse.SC_PAYMENT_REQUIRED);
      return;
    }
    if (!limiter.tryAcquire()) {
      resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Too many requests");
      return;
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    this.rateLimiters.clear();
  }

}
