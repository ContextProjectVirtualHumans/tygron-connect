package nl.tudelft.contextproject.tygron;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class HttpConnection extends Connection {

  private Settings settings;
  private HttpClient client;
  private String authString;
 
  private static final String API_URL_BASE = "https://server2.tygron.com:3022/api/";
  private static final String API_JSON_SUFFIX = "?f=JSON";

  /**
   * Creates a Tygron connection using some settings.
   * 
   * @param tygronSettings
   *          the settings that should be used.
   */
  public HttpConnection(Settings tygronSettings) {
    settings = tygronSettings;
    client = HttpClients.custom().build();
    String headerValue = settings.getUserName() + ":" + settings.getPassword();
    authString = Base64.encodeBase64String(headerValue.getBytes());
  }

  /**
   * Add default headers to a request for authentication/json responses.
   * 
   * @param request
   *          A HttpGet / HttpPost request
   */
  public void addDefaultHeaders(HttpRequestBase request) {
    request.setHeader("Accept", "application/json");
    request.setHeader("Content-Type", "application/json");
    request.setHeader("Authorization", "Basic " + authString);
    
    if(serverToken != null){
      request.setHeader("serverToken", serverToken);
    }
  }

  private String getApiUrl(String eventName) {
    return API_URL_BASE + eventName + API_JSON_SUFFIX;
  }

  @Override
  public String callGetEvent(String eventName) {
    HttpGet request = new HttpGet(getApiUrl(eventName));
    addDefaultHeaders(request);
    try {
      HttpResponse response = client.execute(request);
      return new BasicResponseHandler().handleResponse(response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String callPostEvent(String eventName, JSONArray parameters) {
    HttpPost request = new HttpPost(getApiUrl(eventName));
    addDefaultHeaders(request);

    // adds parameters
    if (parameters != null) {
      try {
        request.setEntity(new StringEntity(parameters.toString()));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    try {
      HttpResponse response = client.execute(request);
      return new BasicResponseHandler().handleResponse(response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
