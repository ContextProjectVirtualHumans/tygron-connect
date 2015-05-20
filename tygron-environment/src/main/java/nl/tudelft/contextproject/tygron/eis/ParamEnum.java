package nl.tudelft.contextproject.tygron.eis;

public enum ParamEnum {
  STAKEHOLDER("stakeholder"), 
  MAP("map"),
  SLOT("slot");
  
  private String param;

  private ParamEnum(String name) {
    this.param = name;
  }

  public String getParam() {
    return param;
  }
}
