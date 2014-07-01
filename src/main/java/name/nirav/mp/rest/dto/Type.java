package name.nirav.mp.rest.dto;

import name.nirav.mp.utils.Tuple;

public enum Type {
  required, invalid, duplicate;
  public static Tuple<Type, String> parse(String msg) {
    String[] kv = Errors.CSV.split(msg);
    String field = kv[0].trim();
    Type type = Type.valueOf(kv[1].trim());
    return Tuple.of(type, field);
  }
}