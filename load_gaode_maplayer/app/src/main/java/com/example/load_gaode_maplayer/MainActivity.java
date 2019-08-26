package com.example.load_gaode_maplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.esri.arcgisruntime.arcgisservices.TileInfo;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArcGISMap arcGISMap = new ArcGISMap();
        TileInfo tileInfo = GaodeImageTiledLayer.getCommonTileInfo();
        GaodeImageTiledLayer gaodeImageTiledLayer = new GaodeImageTiledLayer(tileInfo, new Envelope(-2.2041257773878E7D, -3.26739396727517E7D, 2.2041257773878E7D, 2.08513500432886E7D, SpatialReferences.getWebMercator()));
        arcGISMap.setBasemap(new Basemap(gaodeImageTiledLayer));
        MapView mapView = findViewById(R.id.mapView);
        mapView.setMap(arcGISMap);
    }
}
