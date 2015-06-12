package com.senscribe.mapboxdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chengpengfei on 15/5/20.
 */
public class MainActivity extends Activity{

    private MapView mapView = null;

    private ITileLayer mMapTileLayer = null;
    private final String mMapPid = "openstreetmap";
    private final String mMapUrl = "http://tile.openstreetmap.org/{z}/{x}/{y}.png";
    private final String mMapTileLayerName = "OpenStreetMap";
    private final String mMapAttribution = "© OpenStreetMap Contributors";
    private final int MIN_ZOOM_LEVEL = 10;
    private final int MAX_ZOOM_LEVEL = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MapView)findViewById(R.id.view_map);
        mapView.setVisibility(View.VISIBLE);

        mMapTileLayer = new WebSourceTileLayer(mMapPid,mMapUrl)
                .setName(mMapTileLayerName)
                .setAttribution(mMapAttribution)
                .setMinimumZoomLevel(MIN_ZOOM_LEVEL)
                .setMaximumZoomLevel(MAX_ZOOM_LEVEL);
        mapView.setTileSource(mMapTileLayer);
        mapView.setScrollableAreaLimit(mMapTileLayer.getBoundingBox());
        mapView.setMinZoomLevel(mMapTileLayer.getMinimumZoomLevel());
        mapView.setMaxZoomLevel(mMapTileLayer.getMaximumZoomLevel());
        mapView.setCenter(new LatLng(32.0584, 118.797));
        mapView.setZoom(13);

        // 热力图层数据测试
        HeatmapPointOverlay heatmapOverlay = new HeatmapPointOverlay(mapView);
        List<LatLng> data = new ArrayList<>();
        data.add(new LatLng(32.0584,118.797));
        data.add(new LatLng(32.0587,118.768));
        data.add(new LatLng(32.0458,118.790));
        data.add(new LatLng(32.0589,118.795));
        data.add(new LatLng(32.0482,118.737));
        data.add(new LatLng(32.0481,118.755));
        heatmapOverlay.setHeatmapData(data);
        mapView.addOverlay(heatmapOverlay);

        mapView.addMarker(new Marker("", "", new LatLng(32.0584, 118.797)));
        mapView.addMarker(new Marker("", "", new LatLng(32.0587, 118.768)));
        mapView.addMarker(new Marker("", "", new LatLng(32.0458, 118.790)));
        mapView.addMarker(new Marker("", "", new LatLng(32.0589, 118.795)));
        mapView.addMarker(new Marker("", "", new LatLng(32.0482, 118.737)));
        mapView.addMarker(new Marker("","",new LatLng(32.0481,118.755)));

        // 热力图生成测试
//        int length = 1000;
//        double[][] derp = new double[length][length];
//        for(int i = 0;i < length;i += 50)
//        {
//            int x = (int)(Math.random()*length);
//            int y = (int)(Math.random()*length);
//            derp[y][x] += Math.random()*20 + 10;
//        }
//        Hotmap hm = new Hotmap(derp, 100,this);
//        Bitmap bitmap = hm.makeImage(Hotmap.ColourRange.BLUE_TO_RED);
//        ImageView image = (ImageView)findViewById(R.id.img_heatmap);
//        image.setImageBitmap(bitmap);
//        mapView.setVisibility(View.GONE);


        // 热力图生成到文件
//        String basePath = Environment.getExternalStorageDirectory() + File.separator + "sample.png";
//        image.setVisibility(View.GONE);
//        try {
//            Bitmap bitmap = hm.makeImage(Hotmap.ColourRange.BLUE_TO_RED);
//            File file = new File(basePath);
//            if(file.exists()) file.delete();
//            FileOutputStream output = new FileOutputStream(file);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 90, output);
//            output.flush();
//            output.close();
//        } catch (IOException e) {
//
//        }
    }
}
