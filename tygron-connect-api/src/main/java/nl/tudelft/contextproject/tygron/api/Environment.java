package nl.tudelft.contextproject.tygron.api;

import com.esri.core.geometry.Polygon;

import nl.tudelft.contextproject.tygron.api.loaders.*;
import nl.tudelft.contextproject.tygron.handlers.BooleanResultHandler;
import nl.tudelft.contextproject.tygron.handlers.JsonObjectResultHandler;
import nl.tudelft.contextproject.tygron.objects.Action;
import nl.tudelft.contextproject.tygron.objects.ActionList;
import nl.tudelft.contextproject.tygron.objects.Building;
import nl.tudelft.contextproject.tygron.objects.BuildingList;
import nl.tudelft.contextproject.tygron.objects.LandMap;
import nl.tudelft.contextproject.tygron.objects.PopUpHandler;
import nl.tudelft.contextproject.tygron.objects.Stakeholder;
import nl.tudelft.contextproject.tygron.objects.indicators.Indicator;
import nl.tudelft.contextproject.tygron.objects.indicators.IndicatorFinance;
import nl.tudelft.contextproject.tygron.objects.indicators.IndicatorList;
import nl.tudelft.contextproject.util.PolygonUtil;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Contains all data relative to the session.
 */
public class Environment implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Environment.class);
  
  //The error margin for the amount of land
  private final double errorMargin = 0.10;

  // Environment oriented
  private Session session;
  private PopUpHandler popUpHandler;

  // Session data oriented
  Map<Class<?>, Loader<?>> loaderMap;
  
  private int mapWidth;

  private Thread environmentThread;

  private int stakeholderId;

  /**
   * Creates an environment that communicates with the session API.
   * @param session The session.
   */
  public Environment(Session session) {
    this.session = session;
    stakeholderId = -1;
    environmentThread = new Thread(this);

    loaderMap = new HashMap<>();
    putLoader(new BuildingListLoader(session));
    putLoader(new EconomyListLoader(session));
    putLoader(new FunctionMapLoader(session));
    putLoader(new IndicatorListLoader(session));
    putLoader(new LandMapLoader(session));
    putLoader(new StakeholderListLoader(session));
    putLoader(new ZoneListLoader(session));
    putLoader(new ServerWordsLoader(session));
  }

  public void putLoader(Loader<?> loader) {
    loaderMap.put(loader.getDataClass(), loader);
  }

  public <T> Loader<T> getLoader(Class<T> loader) {
    return (Loader<T>) loaderMap.get(loader);
  }

  /**
   * Starts the update loop for this environment.
   */
  public void start() {
    environmentThread.start();
  }

  /**
   * Main update run for the environment.
   */
  @Override
  public void run() {
    logger.debug("Running Environment update loop...");
    while (true) {
      reload();
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        logger.error("Environment crashed!");
        throw new RuntimeException(e);
      }
    }
  }
  
  /**
   * Allows or disables game interaction.
   * @param set Whether to allow game interaction.
   */
  public void allowGameInteraction(boolean set) {
    String allowInteraction = "event/LogicEventType/SETTINGS_ALLOW_GAME_INTERACTION/";
    JSONArray param = new JSONArray();
    param.put(set);
    HttpConnection.getInstance().execute(allowInteraction, CallType.POST,
            new JsonObjectResultHandler(), session, param);
  }
  
  /**
   * Reloads all data.
   */
  public void reload() {
    for (Map.Entry<Class<?>, Loader<?>> loaderEntry : loaderMap.entrySet()) {
      Loader<?> loader = loaderEntry.getValue();
      if (!loader.getRefreshInterval().equals(Loader.RefreshInterval.NEVER)) {
        loader.reload();
      }
    }
    if (popUpHandler != null) {
      popUpHandler.loadPopUps();
    }
  }

  public <T> T get(Class<T> dataClass) {
    return getLoader(dataClass).get();
  }
  
  /**
   * Select a stakeholder to play, can only be done once.
   * @param stakeholderId the stakeholder id to select.
   */
  public void setStakeholder(int stakeholderId) {
    this.stakeholderId = stakeholderId;
    boolean retValue = HttpConnection.getInstance().execute("event/PlayerEventType/STAKEHOLDER_SELECT",
            CallType.POST, new BooleanResultHandler(), session,
            new StakeholderSelectRequest(stakeholderId, session.getClientToken()));
    logger.info("Setting stakeholder to #" + stakeholderId + ". Operation " 
        + ((retValue) ? "success!" : "failed!" ));
    if (!retValue) {
      throw new RuntimeException("Stakeholder could not be selected!");
    } else {
      popUpHandler = new PopUpHandler(session, stakeholderId);
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
            CallType.POST, new BooleanResultHandler(), session,
            new StakeholderReleaseRequest(stakeholderId));
    stakeholderId = -1;
    popUpHandler = null;
  }
  
  class StakeholderReleaseRequest extends JSONArray {
    public StakeholderReleaseRequest(int stakeholderId) {
      this.put(stakeholderId);
    }
  }
  
  /**
   * Get all of the stakeholder's land that is free from buildings.
   * @param stakeholder The stakeholder.
   * @return The stakeholder's free land.
   */
  public Polygon getAvailableLand(Stakeholder stakeholder) {
    Polygon land = new Polygon();
    for (Integer landId : stakeholder.getOwnedLands()) {
      land = PolygonUtil.polygonUnion(land, get(LandMap.class).get(landId).getPolygon());
    }
      
    for (Building building : get(BuildingList.class)) {
      if (!building.demolished()) {
        land = PolygonUtil.polygonDifference(land, building.getPolygon());
      }
    }
    
    return land;
  }
  
  /**
   * Gets a piece of land of a certain surface from the available land.
   * @param availableLand The available land.
   * @param surface The desired surface of the land.
   * @return A piece of land with a certain surface.
   */
  public Polygon getSuitableLand(Polygon availableLand, double surface) {
    getMapWidth();
    Random random = new Random();
    Polygon selectedLand;
    Polygon intersection;
    
    double x1;
    double y1;
    double x2;
    double y2;
    // Select a random rectangle in the map and get the part of land that is available.
    do {
      x1 = random.nextDouble() * mapWidth;
      y1 = random.nextDouble() * mapWidth;
      x2 = random.nextDouble() * mapWidth;
      y2 = random.nextDouble() * mapWidth;
      selectedLand = PolygonUtil.makeRectangle(x1, y1, x2, y2);
      intersection = PolygonUtil.polygonIntersection(selectedLand, availableLand);
    } while (intersection.calculateArea2D() < surface);
    
    while (!withinMargin(intersection, surface) && intersection.calculateArea2D() != 0) {
      // Reduce the land to a square as much as possible
      if (Math.abs(x1 - x2) > Math.abs(y1 - y2)) {
        x1 = x2 > x1 ? x1 + 0.5 : x1 - 0.5;
      } else {
        y1 = y2 > y1 ? y1 + 0.5 : y1 - 0.5;
      }
      selectedLand = PolygonUtil.makeRectangle(x1, y1, x2, y2);
      intersection = PolygonUtil.polygonIntersection(selectedLand, availableLand);
    }

    // If the selected land is empty, try again
    return intersection.calculateArea2D() != 0 ? intersection :
      getSuitableLand(availableLand, surface);
  }
  
  private void getMapWidth() {
    if (mapWidth == 0) {
      mapWidth = HttpConnection.getInstance().execute("lists/settings/31/",
              CallType.GET, new JsonObjectResultHandler(), session).getInt("value");
    }
  }
  
  public boolean withinMargin(Polygon selectedLand, double surface) {
    return selectedLand.calculateArea2D() < surface * (1 + errorMargin) 
        && selectedLand.calculateArea2D() > surface * (1 - errorMargin);
  }
  
  /**
   * Returns true if the selected stakeholder can ask for money.
   * @return Can stakeholder ask for money.
   */
  public boolean canAskMoney() {
    for (Action action : get(ActionList.class)) {
      Map<Integer, Boolean> activeForStakeholder = action.getActiveForStakeholder();
      boolean active = activeForStakeholder.containsKey(stakeholderId) 
          ? activeForStakeholder.get(stakeholderId) : false;
      if (action.getSpecialOptions().contains("SHOW_SUBSIDY_PANEL") && active) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns true if the selected stakeholder can give money to other stakeholders.
   * @return Can stakeholder give money.
   */
  public boolean canGiveMoney() {
    for (Action action : get(ActionList.class)) {
      Map<Integer, Boolean> activeForStakeholder = action.getActiveForStakeholder();
      boolean active = activeForStakeholder.containsKey(stakeholderId) 
          ? activeForStakeholder.get(stakeholderId) : false;
      if (action.getSpecialOptions().contains("SHOW_MONEY_TRANSFER_PANEL") && active) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Gets the current budget of the given stakeholder.
   * @param stakeholderId The stakeholder's id.
   * @return The current budget.
   */
  public double getBudget(int stakeholderId) {
    for (Indicator indicator : get(IndicatorList.class)) {
      if (indicator.getType().equals("FINANCE")) {
        IndicatorFinance indicatorFinance = (IndicatorFinance) indicator;
        if (indicatorFinance.getActorId() == stakeholderId) {
          return indicatorFinance.getCurrent();
        }
      }
    }
    return -1;
  }
}

