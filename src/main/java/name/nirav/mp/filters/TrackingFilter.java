package name.nirav.mp.filters;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import name.nirav.mp.security.Roles;
import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.User;
import name.nirav.mp.service.dto.Visitor;
import name.nirav.mp.utils.Fingerprint;
import name.nirav.mp.utils.NumberUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackingFilter implements Filter {
  private static final Logger     LOGGER            = LoggerFactory.getLogger(TrackingFilter.class.getSimpleName());
  private static final String     UserCookieName    = "pump";
  private static final String     VisitorCookieName = "pvmp";
  private static final int        COOKIE_AGE        = (int) TimeUnit.DAYS.toSeconds(30);
  private final PredictionService predictionService;

  public static class SecuredHttpRequest extends HttpServletRequestWrapper {
    private User user;

    public SecuredHttpRequest(HttpServletRequest request, User user) {
      super(request);
      this.user = user;
    }

    public Principal getUserPrincipal() {
      return user;
    }

    public boolean isUserInRole(String role) {
      Roles roles = Roles.valueOf(role);
      if (user.isRegistered()) return true;
      else {
        switch (roles) {
          case SEARCH:
            return true;
          case VIEW_PREDICTION:
            return true;
          default:
            return false;
        }
      }
    }
  }

  public TrackingFilter(PredictionService predictionService) {
    this.predictionService = predictionService;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;
    Fingerprint fingerprint = Fingerprint.of(req);
    try {
      User u = Arrays
          .stream(req.getCookies())
          .filter((c) -> c.getName().equals(UserCookieName))
          .map((c) -> predictionService.getUser(NumberUtils.decode(c.getValue())))
          .findFirst()
          .orElseGet(
              () -> {
                Visitor v = Optional.ofNullable(predictionService.recordVisit(fingerprint)).orElseGet(
                    () -> predictionService.createVisitor(fingerprint.getId()));
                createCookie(VisitorCookieName, NumberUtils.encode(v.getId()), COOKIE_AGE, resp);
                return v;
              });
      chain.doFilter(new SecuredHttpRequest(req, u), response);
    } catch (Throwable e) {
      LOGGER.error("Error processing request.", e);
      if (!resp.isCommitted()) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.setContentType(MediaType.APPLICATION_JSON);
        resp.getWriter().print("{\"errors\" : [{\"message\" : \"Internal server error, try again later.\"}] }");
      }
    }
  }

  public static void createUserCookie(User verifiedUser, HttpServletResponse resp) {
    createCookie(UserCookieName, NumberUtils.encode(verifiedUser.getId()), COOKIE_AGE, resp);
    expireCookie(VisitorCookieName, resp);
  }

  public static void expireCookie(String name, HttpServletResponse resp) {
    createCookie(name, null, 0, resp);
  }

  private static void createCookie(String name, String value, int age, HttpServletResponse resp) {
    Cookie c = new Cookie(name, value);
    c.setPath("/");
    c.setMaxAge(age);
    c.setHttpOnly(true);
    resp.addCookie(c);
  }

  @Override
  public void destroy() {

  }

}
