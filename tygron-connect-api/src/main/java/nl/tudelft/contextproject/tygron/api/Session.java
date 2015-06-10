package nl.tudelft.contextproject.tygron.api;

import nl.tudelft.contextproject.tygron.handlers.BooleanResultHandler;
import nl.tudelft.contextproject.tygron.objects.PopUpHandler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * TygronSession. General session handling to Tygron. A brief overview: First
 * you start a new session (START_NEW_SESSION) You'll get back an ID, but you
 * can also get the joinable session with GET_JOINABLE_SESSIONS You can now join
 * a session with JOIN_SESSION. You can either close your own session with
 * CLOSE_SESSION or kill the session with KILL_SESSION.
 */
public class Session {
  private static final Logger logger = LoggerFactory.getLogger(Session.class);

  // Session oriented
  private Environment environment;
  private String name;
  private String platform;
  private String state;
  private String type;
  private String clientToken;
  private String serverToken;
  private List<String> compatibleOperations;
  private int id;
  
  private int stakeholderId;

  /**
   * Tygron Session Object.
   */
  public Session() {
    name = "";
    clientToken = "";
    serverToken = "";

    environment = new Environment();
  }

  /**
   * Tygron Session Object with data.
   * @param data the json data
   */
  public Session(JoinableSession session, JSONObject data) {
    id = session.getId();
    type = session.getType();

    clientToken = data.getJSONObject("client").getString("clientToken");
    serverToken = data.getString("serverToken");
    name = data.getString("project");
    platform = data.getString("platform");
    state = data.getJSONObject("client").getString("connectionState");

    compatibleOperations = new ArrayList<>();
    JSONArray jsonArray = data.getJSONArray("lists");
    int len = jsonArray.length();
    for (int i = 0; i < len; i++) {
      compatibleOperations.add(jsonArray.get(i).toString());
    }

    environment = new Environment();
  }

  /**
   * Close a session (instead of killing it).
   *
   * @param keepAlive In keepAlive is true and you are the last one in the session,
   *                  should it be killed? False indicated it should. True indicated it
   *                  should be kept alive.
   * @return Whether the operations succeeded.
   */
  public boolean closeSession(boolean keepAlive) {
    logger.info("Closing session #" + this.id + " with clientToken " + this.clientToken + " (keepalive: " + keepAlive
        + ")");
    CloseSessionRequest closeSessionRequest = new CloseSessionRequest(this, keepAlive);
    boolean apiReturnValue = HttpConnection.getInstance().execute("services/event/IOServicesEventType/CLOSE_SESSION/",
            CallType.POST, new BooleanResultHandler(), closeSessionRequest);

    logger.info("Closing session result: " + apiReturnValue);

    return apiReturnValue;
  }

  /**
   * Hard kill a Tygron session and reply whether it was a success or not.
   *
   * @param slotId the id of the slot to load in
   * @return whether the kill was successful or not.
   */
  public boolean killSession(int slotId) {
    KillSessionRequest killSessionRequest = new KillSessionRequest(slotId);
    boolean apiCallResult = HttpConnection.getInstance().execute("services/event/IOServicesEventType/KILL_SESSION/",
            CallType.POST, new BooleanResultHandler(), killSessionRequest);
    logger.info("Killing session #" + slotId + " result: " + apiCallResult);

    return apiCallResult;
  }

  class KillSessionRequest extends JSONArray {
    public KillSessionRequest(int slotId) {
      this.put(slotId);
    }
  }

  /**
   * Set a new session name.
   *
   * @param newName The new session name.
   */
  public void setName(String newName) {
    this.name = newName;
  }

  /**
   * Get the session name.
   *
   * @return The session name.
   */
  public String getName() {
    return this.name;
  }

  public String getType() {
    return type;
  }

  /**
   * Set a new session type.
   *
   * @param newType The new session type.
   */
  public void setType(String newType) {
    this.type = newType;
  }

  /**
   * Set a new session id.
   *
   * @param newId id The new session id.
   */
  public void setId(int newId) {
    this.id = newId;
  }

  /**
   * Get the session id.
   *
   * @return The session id.
   */
  public int getId() {
    return this.id;
  }

  /**
   * Set a new server token.
   *
   * @param serverToken The server token.
   */
  public void setServerToken(String serverToken) {
    logger.debug("Session serverToken=" + serverToken);
    this.serverToken = serverToken;
  }

  /**
   * Get the server token.
   *
   * @return The server token.
   */
  public String getServerToken() {
    return this.serverToken;
  }

  /**
   * Set a client token.
   *
   * @param clientToken The client token.
   */
  public void setClientToken(String clientToken) {
    logger.debug("Session clientToken=" + clientToken);
    this.clientToken = clientToken;
  }

  /**
   * Get the client token.
   *
   * @return The client token.
   */
  public String getClientToken() {
    return this.clientToken;
  }

  /**
   * Get the environment.
   *
   * @return The environment.
   */
  public Environment getEnvironment() {
    return this.environment;
  }
  
  /**
   * Get the platform.
   *
   * @return The platform.
   */
  public String getPlatform() {
    return this.platform;
  }
  
  /**
   * Get the state.
   *
   * @return The state.
   */
  public String getState() {
    return this.state;
  }
  
  /**
   * Return a (string) array with all the possible operations/data that can be
   * loaded from the API.
   *
   * @return The compatible operations for this session.
   */
  public List<String> getCompatibleOperations() {
    return this.compatibleOperations;
  }

  class CloseSessionRequest extends JSONArray {
    public CloseSessionRequest(Session session, boolean keepAlive) {
      put(session.getId());
      put(session.getClientToken());
      put(keepAlive);
    }
  }
  
  
  /**
   * Select a stakeholder to play, can only be done once.
   * @param stakeholderId the stakeholder id to select.
   * @throws Exception if stakeholder fails
   */
  public void setStakeholder(int stakeholderId) {
    this.stakeholderId = stakeholderId;
    boolean retValue = HttpConnection.getInstance().execute("event/PlayerEventType/STAKEHOLDER_SELECT",
            CallType.POST, new BooleanResultHandler(), true,
            new StakeholderSelectRequest(stakeholderId, getClientToken()));
    logger.info("Setting stakeholder to #" + stakeholderId + ". Operation " 
        + ((retValue) ? "succes!" : "failed!" ));
    if (!retValue) {
      throw new RuntimeException("Stakeholder could not be selected!");
    } else {
      environment.setPopupHandler(new PopUpHandler(environment, stakeholderId));
    }
  }
  
  class StakeholderSelectRequest extends JSONArray {
    public StakeholderSelectRequest(int stakeholderId, String sessionToken) {
      this.put(stakeholderId);
      this.put(sessionToken);
    }
  }
  
  public int getStakeholderId() {
    return stakeholderId;
  }
  
  /**
   * Releases the stakeholder that is currently selected.
   */
  public void releaseStakeholder() {
    logger.info("Releasing stakeholder #" + stakeholderId);
    HttpConnection.getInstance().execute("event/LogicEventType/STAKEHOLDER_RELEASE/",
            CallType.POST, new BooleanResultHandler(), true,
            new StakeholderReleaseRequest(stakeholderId));
    stakeholderId = -1;
    environment.setPopupHandler(null);
  }
  
  class StakeholderReleaseRequest extends JSONArray {
    public StakeholderReleaseRequest(int stakeholderId) {
      this.put(stakeholderId);
    }
  }
}
