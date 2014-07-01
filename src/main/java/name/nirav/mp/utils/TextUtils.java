package name.nirav.mp.utils;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.nirav.mp.Main;

import com.google.common.base.Joiner;

public class TextUtils {
  private static final String  YEAR_DIGITS    = "\\d{4}";
  private static final String  COMMON_GROUP   = "(around|by|about|by about|in|in the|in the year|in the year of|)";
  private static final Pattern BD             = Pattern.compile(COMMON_GROUP + "\\s", Pattern.CASE_INSENSITIVE);
  private static final Pattern YEAR_FOLLOWING = Pattern.compile(COMMON_GROUP + "\\s" + YEAR_DIGITS + "?", Pattern.CASE_INSENSITIVE);
  private static final Pattern YEAR_BEGINNING = Pattern.compile(YEAR_DIGITS + "?", Pattern.CASE_INSENSITIVE);
  private static final Pattern WHITE_SPACE    = Pattern.compile("\\s");
  private static final Pattern CSV            = Pattern.compile(",|#|@");
  private static final Pattern QUOTES         = Pattern.compile("\"");

  public static String tag(String tags) {
    if (tags == null) return "";
    List<String> properTags = new ArrayList<>();
    String[] tokens = WHITE_SPACE.split(tags);
    for (String t : tokens) {
      String[] regTokens = CSV.split(t);
      for (String rt : regTokens) {
        rt = rt.trim();
        if (rt.isEmpty()) continue;
        properTags.add(rt.trim());
      }
    }
    return Joiner.on(',').join(properTags);
  }

  public static Map<String, Object> infer(String text) {
    Map<String, Object> properties = new HashMap<>();
    text = text.trim();
    if (text.startsWith("\"")) {
      String[] strings = QUOTES.split(text);
      properties.put("text", strings[1]);
      String[] split = CSV.split(strings[2]);
      for (String token : split) {
        if (token.trim().isEmpty()) continue;
        if (token.contains("--")) {
          properties.put("author", token.replace("--", "").trim());
          continue;
        }
        int year = getProbableYear(token);
        if (year != -1) {
          properties.put("year", year);
        } else {
          String string = (String) properties.get("tags");
          if (string == null) {
            string = token.trim();
            properties.put("tags", string);
          } else {
            properties.put("tags", string + ", " + token);
          }
        }
      }

    }
    return properties;
  }

  public static int getProbableYear(String text) {
    Matcher matcher = YEAR_FOLLOWING.matcher(text);
    while (matcher.find()) {
      String group = matcher.group();
      String[] strings = BD.split(group);
      for (String string : strings) {
        try {
          return Integer.parseInt(string);
        } catch (NumberFormatException e) {
        }
      }
    }
    matcher = YEAR_BEGINNING.matcher(text);
    while (matcher.find()) {
      String g = matcher.group();
      try {
        return Integer.parseInt(g);
      } catch (NumberFormatException e) {
      }
    }
    return -1;
  }

  public static long getProbablePredictionTime(String text) {
    Calendar calendar = TimeUtils.getCalendar();
    calendar.set(Calendar.YEAR, TextUtils.getProbableYear(text));
    return calendar.getTimeInMillis();
  }

  public static String getContentHash(String text) {
    if (text == null) return text;
    text = WHITE_SPACE.matcher(text).replaceAll(" ").trim();
    text = text.substring(0, Math.min(text.length(), 24));
    return getMD5(text);
  }

  public static String getMD5(String text) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(text.getBytes("UTF-8"));
      byte[] digest = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte aDigest : digest) {
        sb.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
      }
      return sb.toString();
    } catch (Exception e) {
      Main.log("error generating digest", e);
    }
    return Integer.toString(text.hashCode());
  }
}
