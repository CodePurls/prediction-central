package name.nirav.mp.service.dto;

public class Visitor extends User {
  private int     id;
  private int     number;
  private boolean registered;

  public Visitor() {
  }

  public Visitor(int id, int number) {
    this.id = id;
    this.number = number;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String getFullName() {
    return "Anonymous#" + getNumber();
  }

  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }
}
