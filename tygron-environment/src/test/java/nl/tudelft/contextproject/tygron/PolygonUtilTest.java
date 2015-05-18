package nl.tudelft.contextproject.tygron;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.esri.core.geometry.Polygon;

import nl.tudelft.contextproject.util.PolygonUtil;

import org.codehaus.jackson.JsonParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class PolygonUtilTest {
  PolygonUtil polyutil;
  
  /**
   * Sets up a polygonUtil object.
   */
  @Before
  public void setupPolygonTest() {
    polyutil = new PolygonUtil();
  }
  
  /**
   * Tests the wkt reader.
   */
  @Test
  public void readerTest() {
    try {
      final Polygon polygon1 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((1 1, 2 2"
          + ", 3 3, 1 1)))");
      Polygon polygon2 = new Polygon();
      polygon2.startPath(1, 1);
      polygon2.lineTo(2, 2);
      polygon2.lineTo(3, 3);
      polygon2.lineTo(1, 1);
      polyutil.equals(polygon1,polygon2);
    } catch (JsonParseException e) {
      fail();
      e.printStackTrace();
    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }
  }
  
  /**
   * Tests the equals function.
   */
  @Test
  public void equalsTrueTest() {
    try {
      Polygon polygon1 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((1 1, 2 2, 3 3, 1 1)))");
      Polygon polygon2 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((1 1, 2 2, 3 3, 1 1)))");
      assertTrue(polyutil.equals(polygon1,polygon2));
    } catch (JsonParseException e) {
      fail();
      e.printStackTrace();
    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }
  }
  
  /**
   * Tests the equals function.
   */
  @Test
  public void equalsFalseTest() {
    try {
      Polygon polygon1 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((1 1, 2 2, 3 3, 1 1)))");
      Polygon polygon2 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((4 4, 2 2, 3 3, 4 4)))");
      assertFalse(polyutil.equals(polygon1,polygon2));
    } catch (JsonParseException e) {
      e.printStackTrace();
      fail();
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }
  
  /**
   * Tests the contains function.
   */
  @Test
  public void containsTrueTest() {
    try {
      Polygon polygon1 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((0 0, 0 16, 16 16"
          + ", 16 0, 0 0)))");
      Polygon polygon2 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((4 4, 4 8, 8 8, 8 4"
          + ", 4 4)))");
      assertTrue(polyutil.contains(polygon1, polygon2));
    } catch (JsonParseException e) {
      e.printStackTrace();
      fail();
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }
  
  /**
   * Tests the contains function.
   */
  @Test
  public void containsFalseTest() {
    try {
      Polygon polygon1 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((0 0, 0 16, 16 16"
          + ", 16 0, 0 0)))");
      Polygon polygon2 = polyutil.createPolygonFromWkt("MULTIPOLYGON (((4 4, 4 8, 8 8, 8 4"
          + ", 4 4)))");
      assertFalse(polyutil.contains(polygon2, polygon1));
    } catch (JsonParseException e) {
      e.printStackTrace();
      fail();
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }
}
