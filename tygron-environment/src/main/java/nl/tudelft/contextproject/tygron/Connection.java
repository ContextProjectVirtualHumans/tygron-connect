package nl.tudelft.contextproject.tygron;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class Connection {
  
  protected String serverToken;
  
  public abstract String callGetEvent(String eventName);
  
  public JSONObject callGetEventObject(String eventName) {
    return new JSONObject(callGetEvent(eventName));
  }
  
  public JSONArray callGetEventArray(String eventName) {
    return new JSONArray(callGetEvent(eventName));
  }
  
  public abstract String callPostEvent(String eventName, JSONArray parameters);
 
  public String callPostEventRaw(String eventName, JSONArray parameters) {
    return callPostEvent(eventName, parameters);
  }
  
  public boolean callPostEventBoolean(String eventName, JSONArray parameters) {
    return Boolean.parseBoolean(callPostEvent(eventName, parameters));
  }
  
  public int callPostEventInt(String eventName, JSONArray parameters) {
    return Integer.parseInt(callPostEvent(eventName, parameters));
  }
  
  public JSONObject callPostEventObject(String eventName, JSONArray parameters) {
    return new JSONObject(callPostEvent(eventName, parameters));
  }
  
  public JSONArray callPostEventArray(String eventName, JSONArray parameters) {
    return new JSONArray(callPostEvent(eventName, parameters));
  }
  
  public void setServerToken(String serverToken) {
    this.serverToken = serverToken;
  }
}
