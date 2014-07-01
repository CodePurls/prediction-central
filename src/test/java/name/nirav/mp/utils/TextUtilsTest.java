package name.nirav.mp.utils;

import static name.nirav.mp.utils.TextUtils.getProbableYear;

import org.junit.Test;

/**
 * @author Nirav Thaker
 */
public class TextUtilsTest {
  @Test
  public void testProbableYear() {
    int y = getProbableYear("In 1980, machines less sophisticated than your cellphone filled entire rooms. We can expect similarmind-boggling advances in thecoming decades");
    System.out.println(y);
    y = getProbableYear("Storage and recording technologies will increase in capacity, speed, and affordability, "
        + "such that the major cost associated with storage will be the intelligent effort required to organize or digest it. "
        + "Humans will by 2100 be able to digitally record, archive, and transcribe as much as they want of what they see, hear, and say over their entire lifetimes. "
        + "An ever-increasing majority of existing text, audio, video, and images will be digitally archived into what will be in effect a library of humanity searchable from anywhere on the global network. Existing automated translation technology will make archived texts available in any major human language. Real-time voice recognition will by 2010 be combined with automatic translation and speech generation to produce a crude but effective \"universal translator\" that will allow a monolingual human to converse (at least slowly and simply) with any speaker of any major human language.");
    System.out.println(y);
    y = getProbableYear("What can be more palpably absurd than the prospect held out of locomotives traveling twice as fast as stagecoaches?The Quarterly Review, March, 1825.");
    System.out.println(y);
    y = getProbableYear("In the year 2021, this will happen");
    System.out.println(y);
    y = getProbableYear("In the year of 1999, this will happen");
    System.out.println(y);
  }
}
