package name.nirav.mp.service.dto;

import org.apache.commons.lang.StringUtils;

/**
 * @author Nirav Thaker
 */
public enum Source {
  TWITTER('T', 2), FACEBOOK('F', 3), GOOGLEPLUS('G', 4), WEB('W', -1);

  private final char code;
  private final int  id;

  Source(char code, int id) {
    this.code = code;
    this.id = id;
  }

  public String getPrettyName() {
    return StringUtils.capitalize(name().toLowerCase());
  }

  public int getId() {
    return id;
  }

  public char getCode() {
    return code;
  }
}
