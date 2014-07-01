package name.nirav.mp.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import name.nirav.mp.Main;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource.Builder;

public class MPServiceIntegrationTest {

  private static final String HOST     = "http://localhost:8080/";
  private static final String BASE_URI = HOST + "predictions/";
  private static Client       client;

  @BeforeClass
  public static void init() throws Exception {
    Main.main(new String[] { "server", Thread.currentThread().getContextClassLoader().getResource("mp-dev.yaml").getPath() });
    client = Client.create();
  }

  @AfterClass
  public static void halt() {
    client.destroy();
  }

  @Test
  public void createPrediction() {
    int pid = createInternal("Test prediction");
    assertTrue(pid > 0);
  }

  @Test
  public void updatePrediction() {
    int id = createInternal("Prediction for update testing");
    String expectedPred = "Updated";
    TestPrediction tp = new TestPrediction(id, expectedPred);
    ClientResponse response = getResponse(BASE_URI + id, tp).put(ClientResponse.class);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    TestPrediction entity = response.getEntity(TestPrediction.class);
    assertEquals(expectedPred, entity.text);
    assertEquals(id, entity.id);
  }

  @Test
  public void deletePrediction() {
    int id = createInternal("Prediction to be deleted");
    ClientResponse response = client.resource(BASE_URI + id).delete(ClientResponse.class);
    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  public void getPrediction() {
    String txt = "Prediction to getPrediction";
    int id = createInternal(txt);
    ClientResponse response = client.resource(BASE_URI + id).get(ClientResponse.class);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    TestPrediction entity = response.getEntity(TestPrediction.class);
    assertEquals(txt, entity.text);
  }

  @Test
  public void voteUpPrediction() {
    TestPrediction entity = internalCreateAndGet("/up");
    assertEquals(1, entity.ups);
    assertEquals(0, entity.downs);
  }

  @Test
  public void voteDownPrediction() {
    TestPrediction entity = internalCreateAndGet("/down");
    assertEquals(1, entity.downs);
    assertEquals(0, entity.ups);
  }

  @Test
  public void getTop() {
    ClientResponse response = client.resource(BASE_URI + "top/1").get(ClientResponse.class);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    List<TestPrediction> list = response.getEntity(new GenericType<List<TestPrediction>>(List.class) {
    });
    assertEquals(1, list.size());
  }

  private TestPrediction internalCreateAndGet(String uri) {
    int id = createInternal(UUID.randomUUID().toString());
    ClientResponse response = client.resource(BASE_URI + id + uri).put(ClientResponse.class);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    TestPrediction entity = response.getEntity(TestPrediction.class);
    return entity;
  }

  private int createInternal(String text) {
    TestPrediction pred = new TestPrediction(text);
    ClientResponse resp = getResponse(pred).post(ClientResponse.class);
    assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());
    int pid = Integer.parseInt(resp.getHeaders().getFirst("Location").replace(BASE_URI, ""));
    return pid;
  }

  private Builder getResponse(Object pred) {
    return getResponse(BASE_URI, pred);
  }

  private Builder getResponse(String baseURI, Object pred) {
    return client.resource(baseURI).type(MediaType.APPLICATION_JSON).entity(pred);
  }
}
