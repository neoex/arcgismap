package com.example.load_gaode_maplayer;

import android.support.annotation.IntDef;
import android.util.Log;

import com.esri.arcgisruntime.arcgisservices.LevelOfDetail;
import com.esri.arcgisruntime.arcgisservices.TileInfo;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileKey;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.internal.b.i;
import com.esri.arcgisruntime.internal.jni.CoreRequest;
import com.esri.arcgisruntime.internal.jni.CoreServiceImageTiledLayer;
import com.esri.arcgisruntime.internal.jni.CoreTileKey;
import com.esri.arcgisruntime.internal.jni.CoreTileRequest;
import com.esri.arcgisruntime.internal.l.b;
import com.esri.arcgisruntime.layers.ServiceImageTiledLayer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class GaodeImageTiledLayer extends ServiceImageTiledLayer {

    public static final int IMAGE = 0;
    public static final int VECTOR = 1;
    public static final int ROAD_MARK = 2;

    private int mCurrentType = VECTOR;

    private static final double[] scales = new double[]{2.958293554545656E8D, 1.479146777272828E8D, 7.39573388636414E7D, 3.69786694318207E7D, 1.848933471591035E7D, 9244667.357955175D, 4622333.678977588D, 2311166.839488794D, 1155583.419744397D, 577791.7098721985D, 288895.85493609926D, 144447.92746804963D, 72223.96373402482D, 36111.98186701241D, 18055.990933506204D, 9027.995466753102D, 4513.997733376551D, 2256.998866688275D, 1128.4994333441375D};
    private static final double[] resolutions = new double[]{78271.51696402048D, 39135.75848201024D, 19567.87924100512D, 9783.93962050256D, 4891.96981025128D, 2445.98490512564D, 1222.99245256282D, 611.49622628141D, 305.748113140705D, 152.8740565703525D, 76.43702828517625D, 38.21851414258813D, 19.109257071294063D, 9.554628535647032D, 4.777314267823516D, 2.388657133911758D, 1.194328566955879D, 0.5971642834779395D, 0.29858214173897D};

    private CacheUtils cacheUtils;

    protected GaodeImageTiledLayer(TileInfo tileInfo, Envelope fullExtent) {
        super(tileInfo, fullExtent);
        /**这里如果写到磁盘里面就要考虑动态权限申请了
         * 有点讨厌，就不写了
         * */
        cacheUtils = CacheUtils.getInstance("GaoDeMapCache");
    }

    protected GaodeImageTiledLayer(CoreServiceImageTiledLayer coreServiceImageTiledLayer, boolean addToCache) {
        super(coreServiceImageTiledLayer, addToCache);
        cacheUtils = CacheUtils.getInstance("GaoDeMapCache");
    }


    /**
     * TileKey 是唯一用来标识切片的key
     */
    @Override
    protected String getTileUrl(TileKey tileKey) {
        String url = "http://webst0" + (tileKey.getColumn() % 4 + 1) + ".is.autonavi.com/appmaptile?style=6&x=" + tileKey.getColumn() + "&y=" + tileKey.getRow() + "&z=" + tileKey.getLevel();
        if (this.mCurrentType == IMAGE) {
            url = "http://webst0" + (tileKey.getColumn() % 4 + 1) + ".is.autonavi.com/appmaptile?style=6&x=" + tileKey.getColumn() + "&y=" + tileKey.getRow() + "&z=" + tileKey.getLevel();
        } else if (this.mCurrentType == VECTOR) {
            url = "http://webrd0" + (tileKey.getColumn() % 4 + 1) + ".is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=" + tileKey.getColumn() + "&y=" + tileKey.getRow() + "&z=" + tileKey.getLevel();
        } else if (this.mCurrentType == ROAD_MARK) {
            url = "http://webst0" + (tileKey.getColumn() % 4 + 1) + ".is.autonavi.com/appmaptile?style=8&x=" + tileKey.getColumn() + "&y=" + tileKey.getRow() + "&z=" + tileKey.getLevel();
        }
        Log.d("", " url=" + url);
        return url;
    }


    /**
     * 这个是识别码
     */
    @Override
    public String getUri() {
        return "GaodeImageTiledLayer";
    }


    @IntDef({IMAGE, VECTOR, ROAD_MARK})
    public @interface MapType {
    }

    public static TileInfo getCommonTileInfo() {
        Point iPoint = new Point(-2.0037508342787E7D, 2.0037508342787E7D, SpatialReferences.getWebMercator());
        List<LevelOfDetail> levelOfDetails = new ArrayList<>();
        LevelOfDetail levelOfDetail;
        for (int i = 1; i <= 18; ++i) {
            levelOfDetail = new LevelOfDetail(i, resolutions[i - 1], scales[i - 1]);
            levelOfDetails.add(levelOfDetail);
        }
        return new TileInfo(96, TileInfo.ImageFormat.JPG, levelOfDetails, iPoint, SpatialReferences.getWebMercator(), 256, 256);
    }

    @Override
    protected ListenableFuture<?> onRequestRequired(CoreRequest request) {
        return getFromCache(request);
    }

    private ListenableFuture<?> getFromCache(CoreRequest request) {
        //这里投机取巧，看源码他也是这样使用线程池返回future对象，懒得去找网络请求库了
        @SuppressWarnings("unchecked")
        com.esri.arcgisruntime.internal.b.c future = new com.esri.arcgisruntime.internal.b.c(new Callable<byte[]>() {
            public byte[] call() {
                CoreTileKey coreTileKey = ((CoreTileRequest) request).n();
                TileKey tileKey = TileKey.createFromInternal(coreTileKey);
                String requestUrl = request.h();
                byte[] bytes = getImageBytes(tileKey, requestUrl);
                if (bytes == null) {
                    bytes = new byte[0];
                }
                return bytes;
            }
        });
        i.a().execute(future);
        //这一步是必须的，虽然我也不知道它究竟做了什么，
        // 如果没有底图是空白的，代码是抄的父类的
        loadBytesToMap(future, request);
        return future;
    }

    private String getFileName(TileKey tileKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("R").append(tileKey.getRow());
        sb.append("C").append(tileKey.getColumn());
        sb.append("L").append(tileKey.getLevel());
        sb.append(".jpg");
        return sb.toString();
    }

    private byte[] getImageBytes(final TileKey tileKey, final String requestUrl) {
        byte[] result = cacheUtils.getBytes(getFileName(tileKey));
        if (result == null || result.length == 0) {
            ByteArrayOutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(requestUrl);
                outputStream = new ByteArrayOutputStream();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                if (connection.getResponseCode() == 200) {
                    inputStream = connection.getInputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while (((len = inputStream.read(buffer)) != -1)) {
                        outputStream.write(buffer, 0, len);
                        outputStream.flush();
                    }
                    result = outputStream.toByteArray();
                    cacheUtils.put(getFileName(tileKey), result);
                }

            } catch (Exception e) {
                //ignore
            } finally {
                CloseUtils.closeIOQuietly(inputStream);
                CloseUtils.closeIOQuietly(outputStream);
            }
        }
        return result;
    }

    private static void loadBytesToMap(final ListenableFuture<byte[]> var0, final CoreRequest var1) {
        var0.addDoneListener(new Runnable() {
            public void run() {
                Throwable var1x = null;
                byte[] var2 = null;

                try {
                    var2 = (byte[]) var0.get();
                } catch (Exception var4) {
                    var1x = com.esri.arcgisruntime.internal.b.a.a(var4);
                }

                b.a(var1, var2, var1x);
            }
        });
    }

}
