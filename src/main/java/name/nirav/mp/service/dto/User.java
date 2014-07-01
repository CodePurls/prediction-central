package name.nirav.mp.service.dto;

import java.security.Principal;
import java.util.List;

import javax.security.auth.Subject;

public class User extends Auditable implements Principal {
  private int           id;
  private String        userName;
  private String        fullName;
  private String        email;
  private String        pass;
  private List<Integer> votedUp;
  private List<Integer> votedDown;
  private boolean       registered = true;

  public User() {
  }

  public User(String uname, String fullName) {
    this.userName = uname;
    this.fullName = fullName;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUserName() {
    return userName == null ? email : userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPass() {
    return pass;
  }

  public void setPass(String pass) {
    this.pass = pass;
  }

  public List<Integer> getVotedUp() {
    return votedUp;
  }

  public void setVotedUp(List<Integer> votedUp) {
    this.votedUp = votedUp;
  }

  public List<Integer> getVotedDown() {
    return votedDown;
  }

  public void setVotedDown(List<Integer> votedDown) {
    this.votedDown = votedDown;
  }

  @Override
  public String getName() {
    return getUserName();
  }

  @Override
  public boolean implies(Subject subject) {
    throw new UnsupportedOperationException();
  }

  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }
}
