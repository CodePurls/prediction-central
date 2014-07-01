package name.nirav.mp.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Auditable {
  @JsonProperty("created_on")
  private long   createTimestamp;
  @JsonProperty("updated_on")
  private long   updateTimestamp;
  private int    createdByUserId;
  private String createdByUser;
  private int    captchaId;
  private String captcha;

  public Auditable() {
    setCreateTimestamp(System.currentTimeMillis());
    setUpdateTimestamp(getCreateTimestamp());
  }

  public long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(long createTimestamp) {
    this.createTimestamp = createTimestamp;
  }

  public long getUpdateTimestamp() {
    return updateTimestamp;
  }

  public void setUpdateTimestamp(long updateTimestamp) {
    this.updateTimestamp = updateTimestamp;
  }

  public String getCaptcha() {
    return captcha;
  }

  public void setCaptcha(String captcha) {
    this.captcha = captcha;
  }

  public int getCaptchaId() {
    return captchaId;
  }

  public void setCaptchaId(int captcha_id) {
    this.captchaId = captcha_id;
  }

  public int getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(int createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public String getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(String createdByUser) {
    this.createdByUser = createdByUser;
  }
}
