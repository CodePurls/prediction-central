package name.nirav.mp.utils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.nirav.mp.rest.dto.Errors;

import com.google.common.collect.ImmutableList;
import com.yammer.dropwizard.jersey.InvalidEntityExceptionMapper;
import com.yammer.dropwizard.validation.InvalidEntityException;

public class PredictionExceptionMapper extends InvalidEntityExceptionMapper {
  public static final int    UNPROCESSABLE_ENTITY = 422;

  @Context
  private HttpServletRequest request;

  @Override
  public Response toResponse(InvalidEntityException exception) {
    ImmutableList<String> list = exception.getErrors();
    Errors errors = new Errors();
    for (String msg : list) {
      errors.addError(msg);
    }
    return Response.status(UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON_TYPE).entity(errors).build();
  }
}
