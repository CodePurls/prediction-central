package name.nirav.mp.utils;

import java.util.List;

import name.nirav.mp.service.dto.Comment;
import name.nirav.mp.service.dto.Prediction;

import org.apache.commons.lang.StringEscapeUtils;

public class InputSanitizer {
  public static Prediction sanitize(Prediction p) {
    if (p.getCreatedByUser() != null && p.getCreatedByUser().endsWith("@prediction-central.com")) return p;
    p.setText(escape(p.getText()));
    p.setTitle(escape(p.getTitle()));
    p.setLocation(escape(p.getLocation()));
    p.setAbout(escape(p.getAbout()));
    p.setReason(escape(p.getReason()));
    p.setSourceAuthor(escape(p.getSourceAuthor()));
    p.setSourceRef(escape(p.getSourceRef()));
    p.setTags(escape(p.getTags()));
    return p;
  }

  public static Comment sanitize(Comment comment) {
    comment.setComment(escape(comment.getComment()));
    comment.setAuthor(escape(comment.getAuthor()));
    return comment;
  }

  public static Comment desanitize(Comment comment) {
    comment.setComment(unescape(comment.getComment()));
    comment.setAuthor(unescape(comment.getAuthor()));
    return comment;
  }

  public static List<Prediction> desanitize(List<Prediction> list) {
    for (Prediction prediction : list) {
      desanitize(prediction);
    }
    return list;
  }

  public static Prediction desanitize(Prediction p) {
    if (p.getCreatedByUser() != null && p.getCreatedByUser().endsWith("Crawler")) return p;
    p.setText(unescape(p.getText()));
    p.setTitle(unescape(p.getTitle()));
    p.setLocation(unescape(p.getLocation()));
    p.setAbout(unescape(p.getAbout()));
    p.setReason(unescape(p.getReason()));
    p.setSourceAuthor(unescape(p.getSourceAuthor()));
    p.setSourceRef(unescape(p.getSourceRef()));
    p.setTags(unescape(p.getTags()));
    return p;
  }

  private static String escape(String in) {
    if (in == null) return in;
    String out = in;
    out = StringEscapeUtils.escapeHtml(out);
    return out;
  }

  private static String unescape(String in) {
    return StringEscapeUtils.escapeHtml(in);
  }

}
