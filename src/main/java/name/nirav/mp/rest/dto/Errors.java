package name.nirav.mp.rest.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import name.nirav.mp.utils.Tuple;

public class Errors {
  static final Pattern CSV    = Pattern.compile(",");
  private List<Error>  errors = new ArrayList<>();

  public void addError(String str) {
    Tuple<Type, String> tuple = Type.parse(str);
    Error e = new Error();
    e.setField(tuple.getValue());
    e.setType(tuple.getKey());
    errors.add(e);
  }

  public void addError(Type type, String field) {
    Error e = new Error();
    e.setField(field);
    e.setType(type);
    errors.add(e);
  }

  public void addPlainError(String msg) {
    Error e = new Error();
    e.setMessage(msg);
    errors.add(e);
  }

  public List<Error> getErrors() {
    return errors;
  }

  public static Errors create(String msg) {
    Errors e = new Errors();
    e.addError(msg);
    return e;
  }

  public static Errors createPlain(String msg) {
    Errors e = new Errors();
    e.addPlainError(msg);
    return e;
  }

  public static Errors create(Type type, String field) {
    Errors e = new Errors();
    e.addError(type, field);
    return e;
  }
}
