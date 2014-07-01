package name.nirav.mp.service.analytics;

import java.io.IOException;

import name.nirav.mp.config.OpenCalaisConfig;
import name.nirav.mp.db.PredictionDB;

import org.junit.Test;

/**
 * @author Nirav Thaker
 */
public class EntityExatractionServiceTest {
  @Test
  public void testExtract() throws IOException {
    PredictionDB db = null;
    EntityExtractionService service = new EntityExtractionService(new OpenCalaisConfig(), db);
    service
        .getEntities(" I start from the supposition that the world is topsy-turvy, that things are all wrong, that the wrong people are in jail and the wrong people are out of jail, that the wrong people are in power and the wrong people are out of power, that the wealth is distributed in this country and the world in such a way as not simply to require small reform but to require a drastic reallocation of wealth. I start from the supposition that we don't have to say too much about this because all we have to do is think about the state of the world today and realize that things are all upside down. Daniel Berrigan is in jail-A Catholic priest, a poet who opposes the war-and J. Edgar Hoover is free, you see. David Dellinger, who has opposed war ever since he was this high and who has used all of his energy and passion against it, is in danger of going to jail. The men who are responsible for the My Lai massacre are not on trial; they are in Washington serving various functions, primary and subordinate, that have to do with the unleashing of massacres, which surprise them when they occur. At Kent State University four students were killed by the National Guard and students were indicted. In every city in this country, when demonstrations take place, the protesters, whether they have demonstrated or not, whatever they have done, are assaulted and clubbed by police, and then they are arrested for assaulting a police officer. \n"
            + "Now, I have been studying very closely what happens every day in the courts in Boston, Massachusetts. You would be astounded-maybe you wouldn't, maybe you have been around, maybe you have lived, maybe you have thought, maybe you have been hit-at how the daily rounds of injustice make their way through this marvelous thing that we call due process. Well, that is my premise.");
  }
}
