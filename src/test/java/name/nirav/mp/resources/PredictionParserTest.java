package name.nirav.mp.resources;

import java.util.Scanner;

import name.nirav.mp.utils.TextUtils;

import org.junit.Test;

public class PredictionParserTest {

  @Test
  public void test() {
    Scanner scanner = new Scanner(getClass().getResourceAsStream("sample.txt"));
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine().trim();
      if (line.isEmpty()) continue;
      System.out.println("Line: " + line);
      System.out.println("Inferences: " + TextUtils.infer(line));
    }
    scanner.close();

  }

}
