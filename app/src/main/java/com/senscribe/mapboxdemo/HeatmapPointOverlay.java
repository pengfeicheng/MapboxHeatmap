package com.senscribe.mapboxdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.RotateEvent;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.TilesOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.util.GeometryMath;
import com.mapbox.mapboxsdk.util.TileLooper;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.safecanvas.ISafeCanvas;
import com.mapbox.mapboxsdk.views.util.Projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 热力图层
 *
 * Created by chengpengfei on 15/6/1.
 */
public class HeatmapPointOverlay extends TilesOverlay implements MapListener {

    private final String TAG = "Heat";

    private final Rect mViewPort = new Rect();
    private final Rect mClipRect = new Rect();
    private final Rect mTileRect = new Rect();
    private int mHalfWorldSize_2;
    private float mCurrentZoomFactor;
    private MapView mMapView;

    private ArrayList<LatLng> mHeatmapLatlngList = new ArrayList<>();   // 热点位置数据
    private int mRadius = 80;       // 渲染半径
    private int[] mColorsSpectrum;  // 色谱

    private Object mAccessLock = new Object();
    private HashMap<String,Bitmap> mHeatmapBitmapCache = new HashMap<>();
    private HashMap<String,Object> mHeatmapCreatorRecord = new HashMap<>();

    public HeatmapPointOverlay(MapView mapView) {
        super(mapView.getTileProvider());

        if(mapView == null)
        {
            try
            {
                throw new Exception("mapView is null");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        mMapView = mapView;

        initialColorMap();
    }

    /**
     * 设置热力图数据
     * @param data 地理坐标信息
     */
    public void setHeatmapData(final List<LatLng> data)
    {
        synchronized (mAccessLock)
        {
            mHeatmapLatlngList.clear();
            mHeatmapLatlngList.addAll(data);
        }

        recycleBitmapCache();
    }

    /**
     * 初始化热力图颜色值信息
     */
    private void initialColorMap()
    {
        mColorsSpectrum = new int[120];
        float fromH = 120.0f;
        float sat = 1.0f;
        float lum = 1.0f;

        int alpha = 0;
        int color = android.graphics.Color.HSVToColor(alpha,new float[]{fromH,sat,lum});
        mColorsSpectrum[0] = color;

        // 透明
        for(int i = 1;i < 120;i++)
        {
            alpha = i * 5;
            if(alpha > 160) alpha = 160;
            color = android.graphics.Color.HSVToColor(alpha,new float[]{fromH - i,sat,lum});
            mColorsSpectrum[i] = color;
        }
    }

    /**
     * 热力图生成器
     *
     * 传入参数 Object...:
     *        Rect -> 当前范围矩形
     *        Radius[int]  -> 渲染半径
     */
    class HeatmapTileCreator extends AsyncTask<Object,Integer,Boolean>
    {
        /**
         *
         * @param params 传入参数:
         *               Rect -> 当前范围矩形
         *               Radius[int]  -> 渲染半径
         * @return true -> 热力图生成成功
         *         false -> 生成失败
         */
        @Override
        protected Boolean doInBackground(Object... params) {

            if(params.length < 2) return false;
            if(!(params[0] instanceof Rect) || !(params[1] instanceof Integer)) return false;

            // 考虑到渲染半径的问题，要适当的扩大范围矩阵
            Rect bounds = (Rect)params[0];
            int radius = (int)params[1];
            int offSet = radius * 4;
            int left = bounds.left - offSet;
            int top = bounds.top - offSet;
            int right = bounds.right + offSet;
            int bottom = bounds.bottom + offSet;
            Rect newBounds = new Rect(left,top,right,bottom);

            int newWidth = newBounds.width();
            int newHeight = newBounds.height();

            // 将在当前的范围内的热点地理位置信息转换到对应的地图像素信息
            Projection pj = mMapView.getProjection();
            ArrayList<android.graphics.Point> mapPosList = new ArrayList<>();
            synchronized (mAccessLock)
            {
                for (LatLng latLng : mHeatmapLatlngList)
                {
                    PointF mapPos = new PointF();
                    pj.toMapPixels(latLng,mapPos);

                    int x = (int)Math.floor(mapPos.x);
                    int y = (int)Math.floor(mapPos.y);
                    if(newBounds.contains(x,y)) mapPosList.add(new android.graphics.Point(x,y));
                }
            }

            String cacheKey = bounds.toString();
            Bitmap bitmapCache = null;

            if(mapPosList.size() > 0)
            {
                // 计算渲染数据
                int spreadSize = radius * 2 + 1;
                double[][] radiusSpread = new double[spreadSize][spreadSize];
                double div = radius + 1;
                for(int y = 0; y <= radius; y++)
                {
                    for(int x = 0; x <= radius; x++)
                    {
                        double d = (div - Math.hypot(x, y)) / div;
                        if (d < 0) d = 0;
                        radiusSpread[radius + y][radius + x] = d;
                        radiusSpread[radius - y][radius + x] = d;
                        radiusSpread[radius + y][radius - x] = d;
                        radiusSpread[radius - y][radius - x] = d;
                    }
                }

                // 计算热点权重
                int[][] input = new int[newHeight][newWidth];
                for(int y = 0;y < input.length;y++) Arrays.fill(input[y],0);

                for (android.graphics.Point pos : mapPosList)
                {
                    input[pos.y - top][pos.x - left] += 20;
                }

                // 计算热点位图数据
                double[][] output = new double[newHeight][newWidth];
                for(int y = 0;y < output.length;y++) Arrays.fill(output[y],0);

                double dataMin = Integer.MAX_VALUE;
                double dataMax = Integer.MIN_VALUE;
                for (int y = 0; y < newHeight; y++)
                {
                    for (int x = 0; x < newWidth; x++)
                    {
                        if (input[y][x] == 0) continue;

                        for(int sy = 0; sy < spreadSize; sy++)
                        {
                            int dy = y - radius + sy;
                            if (dy < 0 || dy >= newHeight) continue;

                            for(int sx = 0; sx < spreadSize; sx++)
                            {
                                double iv = input[y][x] * radiusSpread[sy][sx];
                                int dx = x - radius + sx;

                                if (dx < 0 || dx >= newWidth) continue;

                                double nv = output[dy][dx] + iv;

                                if(nv > dataMax) {
                                    dataMax = nv;
                                }else if (nv < dataMin) {
                                    dataMin = nv;
                                }

                                output[dy][dx] = nv;
                            }
                        }
                    }
                }

                //生成位图数据
                int oldWidth = bounds.width();
                int oldHeight = bounds.height();
                double dataRange = dataMax - dataMin;
                double colorRange = mColorsSpectrum.length - 1;
                if(dataRange > 0)
                {
                    bitmapCache = Bitmap.createBitmap(oldWidth,oldHeight, Bitmap.Config.ARGB_8888);
                    for(int y = 0;y < oldHeight;y++)
                    {
                        for (int x = 0;x < oldWidth;x++)
                        {
                            double range = (output[y + offSet][x + offSet] - dataMin)/dataRange;
                            int index = (int)(range * colorRange);
                            bitmapCache.setPixel(x,y, mColorsSpectrum[index]);
                        }
                    }
                }
            }

            synchronized (mAccessLock){
                mHeatmapBitmapCache.put(cacheKey,bitmapCache);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                mMapView.invalidate();
            }
        }
    }


    @Override
    protected void drawSafe(ISafeCanvas canvas, MapView mapView, boolean shadow) {

        if (shadow)  return;

        canvas.getClipBounds(mClipRect);

        Projection pj = mapView.getProjection();
        mHalfWorldSize_2 = pj.getHalfWorldSize();
        GeometryMath.viewPortRectForTileDrawing(pj, mViewPort);

        int tileSize = Projection.getTileSize();
        if(tileSize > 0)
        {
            float zoomLevel = pj.getZoomLevel();
            String cacheKey = "";
            mTileLooper.loop(canvas.getSafeCanvas(), cacheKey, zoomLevel, tileSize, mViewPort, mClipRect);
        }
    }

    private final TileLooper mTileLooper = new TileLooper() {
        @Override
        public void initializeLoop(final float pZoomLevel, final int pTileSizePx) {

            final int roundedZoom = (int) Math.floor(pZoomLevel);
            if (roundedZoom != pZoomLevel)
            {
                final int mapTileUpperBound = 1 << roundedZoom;
                mCurrentZoomFactor = (float) Projection.mapSize(pZoomLevel) / mapTileUpperBound / pTileSizePx;
            }
            else
            {
                mCurrentZoomFactor = 1.0f;
            }
        }

        @Override
        public void handleTile(final Canvas pCanvas, final String pCacheKey, final int pTileSizePx,
                               final MapTile pTile, final int pX, final int pY, final Rect pClipRect) {

            final double factor = pTileSizePx * mCurrentZoomFactor;
            double x = pX * factor - mHalfWorldSize_2;
            double y = pY * factor - mHalfWorldSize_2;

            mTileRect.set((int) x, (int) y, (int) (x + factor), (int) (y + factor));
            if (!Rect.intersects(mTileRect, pClipRect)) {
                return;
            }

            // 在热力图缓存中寻找当前的热力图块，
            // 如果没有找到则尝试生成
            Rect newTile = new Rect(mTileRect);
            String cacheKey = newTile.toString();
            if(mHeatmapBitmapCache.containsKey(cacheKey))
            {
                Bitmap bitmap = mHeatmapBitmapCache.get(cacheKey);
                if(bitmap != null)
                {
                    Drawable drawable = new BitmapDrawable(null,bitmap);
                    drawable.setBounds(newTile);
                    drawable.draw(pCanvas);
                }
            }
            else
            {
                if(!mHeatmapCreatorRecord.containsKey(cacheKey))
                {
                    mHeatmapCreatorRecord.put(cacheKey, null);        // 记录创建信息
                    new HeatmapTileCreator().execute(newTile, mRadius);
                }
            }
        }
    };

    @Override
    public void onRotate(RotateEvent rotateEvent) {}

    @Override
    public void onScroll(ScrollEvent scrollEvent) {}

    @Override
    public void onZoom(ZoomEvent zoomEvent) {
        // 地图放大或者缩小的时候要重新计算生成热力图
        recycleBitmapCache();
    }

    /**
     * 释放热力图缓存资源
     */
    private void recycleBitmapCache()
    {
        synchronized (mAccessLock)
        {
            mHeatmapCreatorRecord.clear();
            Iterator<String> it = mHeatmapBitmapCache.keySet().iterator();
            while (it.hasNext())
            {
                String key = it.next();
                Bitmap bitmap = mHeatmapBitmapCache.get(key);
                if(bitmap != null) bitmap.recycle();
                it.remove();
            }
        }
    }
}
