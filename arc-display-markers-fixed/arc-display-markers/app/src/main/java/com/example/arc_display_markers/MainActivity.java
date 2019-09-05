package com.example.arc_display_markers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.FillSymbol;
import com.esri.arcgisruntime.symbology.LineSymbol;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.esri.arcgisruntime.symbology.SimpleFillSymbol.Style.FORWARD_DIAGONAL;

public class MainActivity extends AppCompatActivity {
    /**
     * 基本知识点
     * 1.图层  GraphicsOverlay 用来加载构建的点、线、面等几何图形，可以加载多层图层
     * 2.Graphic 用来表示 几何图形整体，包括gemetry 和 symbol 这一点跟 webgl 里面的 gemetry 和 material 非常像
     * 3.通过Graphic 的相关set方法可以直接改变 它显示出来的样式
     * 4.arcgis mapView 地图上的点的坐标系是 墨卡托坐标系，使用
     * mapView.screenToLocation 可以将android 屏幕坐标转换成墨卡托坐标
     * 而另一个方法 mapView.locationToScreen() 可以将 墨卡托坐标转换成  android 屏幕上的点
     * 5.其他symbol  一般都是点的符号
     * MarkerSymbol { SimpleMarkerSymbol、TextSymbol(绘制文字)、PictureSymbol（图片）}
     */


    private static final String TAG = MainActivity.class.getSimpleName();
    private MapView mapView;
    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
    private LocationDisplay locationDisplay;

    private GraphicsOverlay graphicsOverlay;

    private PointCollection pointCollection;

    private Symbol simpleMarkerSymbol;
    private LineSymbol lineSymbol;
    private FillSymbol fillSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMapView();
        initLocationDIsplay();
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkSelfPermissions();
        if (mapView != null) {
            mapView.resume();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (locationDisplay != null && locationDisplay.isStarted()) {
            locationDisplay.stop();
        }
        if (mapView != null) {
            mapView.pause();
        }
    }

    private void initMapView() {
        /**去除arcgis 本身的标识信息*/
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud8300236646,none,3M2PMD17JYEJ2B3TR202");
        mapView = findViewById(R.id.mapView);
        mapView.setAttributionTextVisible(false);

        ArcGISMap arcGISMap = new ArcGISMap();
        GaodeImageTiledLayer layer = GaodeImageTiledLayer.getInstance();
        layer.setmCurrentType(GaodeImageTiledLayer.VECTOR);
        Basemap basemap = new Basemap(layer);

     /*   GaodeImageTiledLayer road = GaodeImageTiledLayer.getInstance();
        layer.setmCurrentType(GaodeImageTiledLayer.ROAD_MARK);
        basemap.getBaseLayers().add(road);*/

        arcGISMap.setBasemap(basemap);

        mapView.setMap(arcGISMap);
        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                identityGraphic(event);
                return super.onSingleTapConfirmed(event);
            }
        });
        graphicsOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        pointCollection = new PointCollection(SpatialReferences.getWebMercator());
    }


    private void initLocationDIsplay() {
        locationDisplay = mapView.getLocationDisplay();
        locationDisplay.setLocationDataSource(new GcjLocationDataSource(this));
        locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
            @Override
            public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                Point point = locationChangedEvent.getLocation().getPosition();
                Log.i(TAG, "onLocationChanged: point = " + point.toString());

            }
        });
    }

    private void checkSelfPermissions() {
        boolean permit = ActivityCompat.checkSelfPermission(this, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED;
        if (permit) {
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            /**高德地图使用gcj02坐标系，地图整体做了偏移，而arcgis 是 gps 直接转成了 墨卡托，所以定位偏移很大
             * 需要将gps 转成gcj02 再映射到地图上
             * */
            locationDisplay.startAsync();
        } else {
            ActivityCompat.requestPermissions(this, reqPermissions, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            locationDisplay.startAsync();
        }

    }


    public void addPointMarker(Point point) {
        pointCollection.add(point);
        if (simpleMarkerSymbol == null) {
            simpleMarkerSymbol = new SimpleMarkerSymbol();
        }
        graphicsOverlay.getGraphics().add(new Graphic(point, simpleMarkerSymbol));
        addLine();
    }

    private int lineIndex = -1;

    /**
     * 构建polyline
     */
    public void addLine() {
        if (lineIndex < 0) {
            if (lineSymbol == null) {
                lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 5);
            }
            Polyline polyline = new Polyline(pointCollection);
            boolean add = graphicsOverlay.getGraphics().add(new Graphic(polyline, lineSymbol));
            if (add) {
                lineIndex = graphicsOverlay.getGraphics().size() - 1;
            }
        } else {
            Polyline polyline = new Polyline(pointCollection);
            Graphic graphic = graphicsOverlay.getGraphics().get(lineIndex);
            graphic.setGeometry(polyline);
        }
        addPolygon();
        addPictrueMarker();
    }


    private int lastPolyGoneIndex = -1;

    /**
     * 构建polygon
     */
    public void addPolygon() {
        if (lastPolyGoneIndex < 0) {
            Polygon polygon = new Polygon(pointCollection);
            if (fillSymbol == null) {
                fillSymbol = new SimpleFillSymbol(FORWARD_DIAGONAL, Color.YELLOW, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.YELLOW, 3));
            }
            boolean add = graphicsOverlay.getGraphics().add(new Graphic(polygon, fillSymbol));
            if (add) {
                lastPolyGoneIndex = graphicsOverlay.getGraphics().size() - 1;
            }
        } else {
            Graphic graphic = graphicsOverlay.getGraphics().get(lastPolyGoneIndex);
            Polygon polygon = new Polygon(pointCollection);
            graphic.setGeometry(polygon);
        }
    }


    private int mScreenWidth;
    private int mScreenHeight;

    private void addTextSymbol(Point point) {
        TextSymbol textSymbol = new TextSymbol();
        textSymbol.setColor(Color.GREEN);
        /**之前的版本是不支持汉字的，现在已经支持了*/
        textSymbol.setText("中国汉字+中国汉子");
        textSymbol.setSize(20);
        graphicsOverlay.getGraphics().add(new Graphic(point, textSymbol));
    }

    private void addPictrueMarker() {
        mScreenWidth = this.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;
        BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.beauty);
        try {
            PictureMarkerSymbol pictureMarkerSymbol = PictureMarkerSymbol.createAsync(bitmapDrawable).get();
            pictureMarkerSymbol.setWidth(32);
            pictureMarkerSymbol.setHeight(32);

            Point point = mapView.screenToLocation(
                    new android.graphics.Point(mScreenWidth / 2, mScreenHeight / 2));
            graphicsOverlay.getGraphics().add(new Graphic(point, pictureMarkerSymbol));
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void identityGraphic(MotionEvent event) {
        // 1.get the screen point
        android.graphics.Point screenPoint = new android.graphics.Point(Math.round(event.getX()),
                Math.round(event.getY()));
        final ListenableFuture<IdentifyGraphicsOverlayResult> identifyResultsFuture = mapView
                .identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 10, false);
        identifyResultsFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    IdentifyGraphicsOverlayResult identifyGraphicsOverlayResult = identifyResultsFuture.get();
                    List<Graphic> graphics = identifyGraphicsOverlayResult.getGraphics();
                    if (graphics.size() > 0) {
                        Point point = mapView.screenToLocation(screenPoint);
                        /**坐标转换*/
                        Point gps = (Point) GeometryEngine.project(point, SpatialReferences.getWgs84());
                        Log.i(TAG, "run: " + gps.toString());
                        longlatToAddress(point);
                        /**相对高德地图来说 poi 比较少*/
                        searchPlace("乐富电商集群03栋B座");
                    } else {
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        Point point = mapView.screenToLocation(new android.graphics.Point(x, y));
                        addPointMarker(point);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private void searchPlace(String placeName) {
        if (mLocatorTask == null) {
            mLocatorTask = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
        }
        ListenableFuture<List<GeocodeResult>> listenableFuture = mLocatorTask.geocodeAsync(placeName);
        listenableFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    List<GeocodeResult> list = listenableFuture.get();
                    if (list.size() > 0) {
                        GeocodeResult result = list.get(0);
                        Log.i(TAG, "result: " + result.toString());
                        Log.i(TAG, "getLabel: " + result.getLabel());
                        Log.i(TAG, "getDisplayLocation: " + result.getDisplayLocation().toString());
                        addPointMarker(result.getDisplayLocation());
                        Log.i(TAG, "getRouteLocation: " + result.getRouteLocation().toString());
                        Log.i(TAG, "getInputLocation: " + result.getInputLocation().toString());
                        Log.i(TAG, "Attributes: " + result.getAttributes().size());
                        Map<String, Object> map = result.getAttributes();
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            Log.i(TAG, "map: key = " + entry.getKey() + " = " + entry.getValue().toString());
                        }
                    }


                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private LocatorTask mLocatorTask;

    private void longlatToAddress(Point point) {
        if (mLocatorTask == null) {
            mLocatorTask = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
        }
        long start = System.currentTimeMillis();
        ListenableFuture<List<GeocodeResult>> listenableFuture = mLocatorTask.reverseGeocodeAsync(point);
        listenableFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    List<GeocodeResult> list = listenableFuture.get();
                    Log.i(TAG, "costTime: " + (System.currentTimeMillis() - start));
                    if (list.size() > 0) {
                        GeocodeResult result = list.get(0);
                        Log.i(TAG, "result: " + result.toString());
                        Log.i(TAG, "getLabel: " + result.getLabel());
                        Log.i(TAG, "getDisplayLocation: " + result.getDisplayLocation().toString());
                        Log.i(TAG, "getRouteLocation: " + result.getRouteLocation().toString());
                        Log.i(TAG, "getInputLocation: " + result.getInputLocation().toString());
                        Log.i(TAG, "Attributes: " + result.getAttributes().size());
                        Map<String, Object> map = result.getAttributes();
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            Log.i(TAG, "map: key = " + entry.getKey() + " = " + entry.getValue().toString());
                        }
                    }

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
