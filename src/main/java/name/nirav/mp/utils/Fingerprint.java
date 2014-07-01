package name.nirav.mp.utils;

import javax.servlet.http.HttpServletRequest;

public class Fingerprint {
  private String finderprint;
  private int    user_id;
  private int    visitor_id;

  public Fingerprint() {
  }

  public Fingerprint(HttpServletRequest request) {
    this(request.getRemoteAddr() + "," + request.getHeader("User-Agent"));
  }

  public Fingerprint(int id, String finderprint, int user_id, int visitor_id) {
    this.finderprint = finderprint;
    this.user_id = user_id;
    this.visitor_id = visitor_id;
  }

  public Fingerprint(String str) {
    this.finderprint = str;
  }

  public static Fingerprint of(HttpServletRequest req) {
    return new Fingerprint(req);
  }

  public void setId(int id) {
  }

  public String getFinderprint() {
    return finderprint;
  }

  public void setFinderprint(String finderprint) {
    this.finderprint = finderprint;
  }

  public int getUser_id() {
    return user_id;
  }

  public void setUser_id(int user_id) {
    this.user_id = user_id;
  }

  public int getVisitor_id() {
    return visitor_id;
  }

  public void setVisitor_id(int visitor_id) {
    this.visitor_id = visitor_id;
  }

  @Override
  public String toString() {
    return finderprint;
  }

  @Override
  public int hashCode() {
    return finderprint.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Fingerprint other = (Fingerprint) obj;
    if (finderprint == null) {
      if (other.finderprint != null) return false;
    } else if (!finderprint.equals(other.finderprint)) return false;
    return true;
  }

  public int getId() {
    return hashCode();
  }
}
