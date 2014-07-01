package name.nirav.mp.service.dto;

public class Captcha {
  private int    captchaId;
  private String captcha;

  public Captcha(int captchaId, String captcha) {
    this.captchaId = captchaId;
    this.captcha = captcha;
  }

  public int getCaptchaId() {
    return captchaId;
  }

  public void setCaptchaId(int captchaId) {
    this.captchaId = captchaId;
  }

  public String getCaptcha() {
    return captcha;
  }

  public void setCaptcha(String captcha) {
    this.captcha = captcha;
  }
}
