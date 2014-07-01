package name.nirav.mp.security;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import name.nirav.mp.service.dto.Captcha;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.utils.Tuple;

public class CaptchaManager {
  private static final int                  MAX          = 50;
  private static final Map<String, Integer> captcha_pool = new HashMap<>();
  private static final Map<Integer, String> inverse_map  = new HashMap<>();
  private static final String[]             operators    = { "+", "-"/* , "multiplied by" */};
  private static final Random               rand         = new Random(System.nanoTime());

  static {
    for (int i = 1; i < MAX; i++) {
      int op1 = rand.nextInt(MAX);
      int op2 = rand.nextInt(MAX);
      String op = operators[rand.nextInt(operators.length)];
      int result = 0;
      switch (op) {
        case "+":
          result = op1 + op2;
          break;
        case "-":
          result = op1 - op2;
          break;
        case "multiply":
          result = op1 * op2;
          break;
      }
      String key = format("%d %s %d = ?", op1, op, op2);
      captcha_pool.put(key, result);
      inverse_map.put(key.hashCode(), key);
    }
  }

  public static Tuple<Integer, String> random() {
    int r = rand.nextInt(captcha_pool.size());
    Set<Entry<String, Integer>> set = captcha_pool.entrySet();
    int i = 0;
    for (Entry<String, Integer> entry : set) {
      if (i == r) { return Tuple.of(entry.getKey().hashCode(), entry.getKey()); }
      i++;
    }
    throw new AssertionError("Random ran out of pool: " + r);
  }

  public static boolean verify(String expr, String result) {
    Integer integer = captcha_pool.get(expr);
    if (integer == null) return false;
    try {
      return integer == Integer.parseInt(result);
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean verify(int id, String captcha) {
    return verify(inverse_map.get(id), captcha);
  }

  public static Prediction encode(Prediction p) {
    Tuple<Integer, String> random = random();
    p.setCaptcha(random.getValue());
    p.setCaptchaId(random.getKey());
    return p;
  }

  public static Captcha getNextCaptcha() {
    Tuple<Integer, String> tuple = random();
    return new Captcha(tuple.getKey(), tuple.getValue());
  }
}
