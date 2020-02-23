package de.blau.android.imageryoffset;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.GeoMath;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OffsetModeTest {

    public static final int TIMEOUT         = 90;
    Splash                  splash          = null;
    Main                    main            = null;
    UiDevice                device          = null;
    ActivityMonitor         monitor         = null;
    Instrumentation         instrumentation = null;
    Preferences             prefs           = null;
    Map                     map             = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class, false, false);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();

        device = UiDevice.getInstance(instrumentation);
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        splash = mActivityRule.launchActivity(intent);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000); // wait for main
        Assert.assertNotNull(main);

        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        prefs = new Preferences(main);
        // allow downloading tiles here
        prefs.setBackGroundLayer(TileLayerServer.LAYER_MAPNIK);
        map = main.getMap();
        map.setPrefs(main, prefs);
        main.invalidateOptionsMenu(); // to be sure that the menu entry is actually shown
        TestUtils.resetOffsets(main.getMap());
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        if (main != null) {
            TestUtils.zoomToLevel(main, 18);
            TestUtils.resetOffsets(main.getMap());
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }

        instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync();
    }

    /**
     * Start offset mode and drag the screen
     */
    @Test
    public void offsetMode() {
        TestUtils.zoomToLevel(main, 18);
        try {
            BoundingBox bbox = GeoMath.createBoundingBoxForCoordinates(47.390339D, 8.38782D, 50D, true);
            App.getLogic().getViewBox().setBorders(map, bbox);
            map.setViewBox(App.getLogic().getViewBox());
            map.invalidate();
            try {
                Thread.sleep(5000); // NOSONAR
            } catch (InterruptedException e) {
            }
        } catch (OsmException e) {
            Assert.fail(e.getMessage());
        }

        if (!TestUtils.clickMenuButton("Tools", false, true)) {
            TestUtils.clickOverflowButton();
            TestUtils.clickText(device, false, "Tools", true);
        }
        Assert.assertTrue(TestUtils.clickText(device, false, "Align background", true));
        Assert.assertTrue(TestUtils.findText(device, false, "Align background"));
        TileLayerServer tileLayerConfiguration = map.getBackgroundLayer().getTileLayerConfiguration();
        tileLayerConfiguration.setOffset(0, 0);
        TestUtils.zoomToLevel(main, tileLayerConfiguration.getMaxZoom());
        int zoomLevel = map.getZoomLevel();
        Offset offset = tileLayerConfiguration.getOffset(zoomLevel);
        Assert.assertEquals(0D, offset.getDeltaLat(), 0.1E-4);
        Assert.assertEquals(0D, offset.getDeltaLon(), 0.1E-4);
        TestUtils.drag(map, 8.38782, 47.390339, 8.388, 47.391, true, 50);
        TestUtils.clickOverflowButton();
        TestUtils.clickText(device, false, "Save to database", true);
        // 74.22 m
        TestUtils.clickText(device, false, "Cancel", true);
        TestUtils.clickOverflowButton();
        TestUtils.clickText(device, false, "Apply", true);
        TestUtils.clickHome(device);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        zoomLevel = map.getZoomLevel();
        offset = tileLayerConfiguration.getOffset(zoomLevel);
        Assert.assertNotNull(offset);
        Assert.assertEquals(6.462E-4, offset.getDeltaLat(), 0.1E-4);
        Assert.assertEquals(1.773E-4, offset.getDeltaLon(), 0.1E-4);
    }
}