package name.nirav.mp.tasks;

import java.io.File;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.SecurityContext;

import name.nirav.mp.service.PredictionService;
import name.nirav.mp.service.dto.Comment;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.User;
import name.nirav.mp.service.dto.Visitor;
import name.nirav.mp.utils.Fingerprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

/**
 * @author Nirav Thaker
 */
public class ImportDataTask extends Task {
  private static final int    BATCH_SIZE = 10;
  private static final Logger LOG        = LoggerFactory.getLogger(ImportDataTask.class);
  private PredictionService   service;

  public ImportDataTask(PredictionService service) {
    super("import");
    this.service = service;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String type = parameters.get("type").iterator().next();
    File file = new File(parameters.get("file").iterator().next());
    JsonFactory f = new JsonFactory();
    JsonParser parser = f.createJsonParser(file);
    parser.nextToken();

    if ("predictions".equalsIgnoreCase(type)) {
      List<Prediction> predictions = new ArrayList<>(BATCH_SIZE);
      int count = 0;
      while (parser.nextToken() == JsonToken.START_OBJECT) {
        try {
          Prediction prediction = mapper.readValue(parser, Prediction.class);
          predictions.add(prediction);
          if (predictions.size() >= BATCH_SIZE) {
            service.createPredictions(service.getAdminSecurityContext(), predictions);
            count += predictions.size();
            predictions.clear();
          }
        } catch (Exception e) {
          LOG.error("Error inserting predictions:", e);
          e.printStackTrace(output);
          predictions.clear();
        }
      }
      count += predictions.size();
      output.println("Imported " + count + " predictions.");
    } else if ("comments".equalsIgnoreCase(type)) {
      int count = 0;
      while (parser.nextToken() == JsonToken.START_OBJECT) {
        try {
          Comment comment = mapper.readValue(parser, Comment.class);
          service.createComment(service.getAdminSecurityContext(), comment.getPredictionId(), comment);
          count++;

        } catch (Exception e) {
          e.printStackTrace(output);
        }
      }
      output.println("Imported " + count + " comments.");
    } else if ("users".equalsIgnoreCase(type)) {
      int count = 0;
      while (parser.nextToken() == JsonToken.START_OBJECT) {
        try {
          User user = mapper.readValue(parser, User.class);
          try {
            final SecurityContext sec = new SecurityContext() {
              @Override
              public Principal getUserPrincipal() {
                return user;
              }

              @Override
              public boolean isUserInRole(String role) {
                return true;
              }

              @Override
              public boolean isSecure() {
                return true;
              }

              @Override
              public String getAuthenticationScheme() {
                return "User";
              }
            };
            service.createUser(sec, user);
            LOG.info("Created user {}", user.getEmail());
            count++;
          } catch (Exception e) {
            LOG.error("Error adding user {} {}", user.getEmail(), e.getMessage());
          }
        } catch (Exception e) {
          e.printStackTrace(output);
        }
      }
      output.println("Imported " + count + " " + type);
    } else if ("visitors".equalsIgnoreCase(type)) {
      int count = 0;
      while (parser.nextToken() == JsonToken.START_OBJECT) {
        try {
          Visitor visitor = mapper.readValue(parser, Visitor.class);
          service.createVisitor(visitor.getId());
          count++;
        } catch (Exception e) {
          e.printStackTrace(output);
        }
      }
      output.println("Imported " + count + " " + type);
    } else if ("fingerprints".equalsIgnoreCase(type)) {
      int count = 0;
      while (parser.nextToken() == JsonToken.START_OBJECT) {
        try {
          Fingerprint visitor = mapper.readValue(parser, Fingerprint.class);
          service.recordVisit(visitor);
          count++;
        } catch (Exception e) {
          e.printStackTrace(output);
        }
      }
      output.println("Imported " + count + " " + type);
    }
  }
}
