package name.nirav.mp.utils;

import java.util.Random;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import name.nirav.mp.rest.dto.Errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ServerExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerExceptionMapper.class);
  private static final Random RANDOM = new Random();

  @Override
  public Response toResponse(E exception) {
    if (exception instanceof WebApplicationException) { return ((WebApplicationException) exception).getResponse(); }

    final long id = randomId();
    logException(id, exception);
    Errors errors = Errors.createPlain("Internal server error, please try again later.");
    return Response.serverError().type(MediaType.APPLICATION_JSON_TYPE).entity(errors).build();

  }

  protected void logException(long id, E exception) {
    LOGGER.error(formatLogMessage(id, exception), exception);
  }

  protected String formatLogMessage(long id, Throwable exception) {
    return String.format("Error handling a request: %016x", id);
  }

  protected static long randomId() {
    return RANDOM.nextLong();
  }
}
