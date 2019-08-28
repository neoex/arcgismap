package com.example.display_device_location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MapView mapView;
    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
    private LocationDisplay locationDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**这个只用调用一次，只要在使用arcgis 之前调用就好了，注册一个开发者账号*/
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud8300236646,none,3M2PMD17JYEJ2B3TR202");

        mapView = findViewById(R.id.mapview);

        ArcGISMap arcGISMap = new ArcGISMap();
        GaodeImageTiledLayer layer = GaodeImageTiledLayer.getInstance();
        arcGISMap.setBasemap(new Basemap(layer));
        mapView.setMap(arcGISMap);

        clearArcgisTitles();
        checkPermissions();
    }


    /**
     * 隐藏下面的那些文字
     */
    private void clearArcgisTitles() {
        mapView.setAttributionTextVisible(false);
    }


    private void checkPermissions() {
        boolean grantPermission =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (grantPermission) {
            startLocation();
        } else {
            ActivityCompat.requestPermissions(this, reqPermissions, requestCode);
        }
    }

    private void startLocation() {
        locationDisplay = mapView.getLocationDisplay();
        locationDisplay.setShowPingAnimation(false);
//        locationDisplay.setShowAccuracy(true);

        locationDisplay.addAutoPanModeChangedListener(new LocationDisplay.AutoPanModeChangedListener() {
            @Override
            public void onAutoPanModeChanged(LocationDisplay.AutoPanModeChangedEvent autoPanModeChangedEvent) {
                Log.i(TAG, "onAutoPanModeChanged: " + autoPanModeChangedEvent.getAutoPanMode().name());
          /*      if (!autoPanModeChangedEvent.getAutoPanMode().name().equalsIgnoreCase(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION.name())) {
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                }*/

            }
        });
        locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
            @Override
            public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
//                mapView.setViewpointCenterAsync(locationChangedEvent.getLocation().getPosition());
                Log.i(TAG, "onLocationChanged: " + locationChangedEvent.getLocation().getPosition().toString());
            }
        });
        /**这里本来是两种定位，网络定位和 gps定位，(怎么用代码开启gps网上应该很多)
         * 但是不支持单独的网络定位
         * 只有gps 或者gps 和网络定位同时工作可以显示位置
         * 单独的网络定位是不显示也不更新定位的
         * 需要单独支持网络定位的需要重写AndroidDataSource
         *  调用locationDisplay.setLocationDataSource();
         * */
        locationDisplay.startAsync();
        /**当触碰移动地图之后，那个地图的定位的symbol 会变*/
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            boolean grant = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    grant = false;
                    Toast.makeText(this, "请开启定位权限", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            if (grant) {
                startLocation();
            }
        }
    }
}
