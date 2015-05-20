package nl.tudelft.contextproject.tygron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.esri.core.geometry.Polygon;

import nl.tudelft.contextproject.democode.CachedFileReader;
import nl.tudelft.contextproject.util.PolygonUtil;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class BuildingTest {
  Building building;

  @Mock
  private Session sessionMock;

  /**
   * Creates a building from a cached file.
   */
  @Before
  public void setupEconomy() {
    String indicatorContents = CachedFileReader.getFileContents("/serverResponses/testmap/lists/building.json");
    JSONObject buildingResult = new JSONObject(indicatorContents);
    building = new Building(buildingResult);
  }

  @Test
  public void nameTest() {
    assertEquals("Delfgauwseweg", building.getName());
  }

  @Test
  public void floorTest() {
    assertEquals(1, building.getFloors());
  }

  @Test
  public void polygonTest() {
    PolygonUtil polyUtil = new PolygonUtil();
    Polygon polygon1 = building.getPolygon();
    try {
      Polygon polygon2 = polyUtil.createPolygonFromWkt("MULTIPOLYGON (((32.063 122.522"
          + ", 111.728 92.311, 166.819 69.234, 229.024 45.124, 226.367 38.006, 182 55.553"
          + ", 0 124.472, 0 134.572, 32.063 122.522)))");
      assertTrue(polyUtil.equals(polygon1, polygon2));
    } catch (Exception e) {
      fail();
      e.printStackTrace();
    }
  }
}
