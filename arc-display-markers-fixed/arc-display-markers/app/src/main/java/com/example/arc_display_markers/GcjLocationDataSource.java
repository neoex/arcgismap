package com.example.arc_display_markers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.internal.p.d;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class GcjLocationDataSource extends LocationDataSource implements LocationDisplay.AutoPanModeChangedListener {
    private final Context mContext;
    private float mMinimumUpdateDistance;
    private long mMinimumUpdateTime;
    private LocationManager mLocationManager;
    private final List<String> mSelectedLocationProviders;
    private b mInternalLocationListener;
    private SensorManager mSensorManager;
    private a mInternalHeadingListener;
    private Criteria mCriteria;
    private String mProvider;
    private LocationDataSource.Location mLastLocation;
    private volatile LocationDisplay.AutoPanMode mAutoPanMode;
    private static final double ACCURACY_THRESHOLD_FACTOR = 2.0D;
    private static final String EXCEPTION_MSG = "No location provider found on the device";
    private static final String NO_STARTED_MSG = "The location data source is not started yet";
    private static final String NO_PROVIDER_MSG = "No provider found for the given name : %s";

    public GcjLocationDataSource(Context context) {
        this.mMinimumUpdateDistance = 0.0F;
        this.mMinimumUpdateTime = 100L;
        this.mSelectedLocationProviders = new ArrayList();
        this.mContext = context.getApplicationContext();
    }

    public GcjLocationDataSource(Context context, Criteria criteria, long minTime, float minDistance) {
        this(context);
        this.mCriteria = criteria;
        this.a(minTime, minDistance);
    }

    public GcjLocationDataSource(Context context, String provider, long minTime, float minDistance) {
        this(context);
        this.mProvider = provider;
        this.a(minTime, minDistance);
    }

    public void requestLocationUpdates(Criteria criteria, long minTime, float minDistance) {
        if (!this.isStarted()) {
            throw new IllegalStateException("The location data source is not started yet");
        } else {
            this.mSelectedLocationProviders.clear();
            this.a(minTime, minDistance);
            this.a(criteria);
            if (this.mSelectedLocationProviders.isEmpty()) {
                throw new IllegalStateException("No location provider found on the device");
            } else {
                this.d();
            }
        }
    }

    public void requestLocationUpdates(String provider, long minTime, float minDistance) {
        if (!this.isStarted()) {
            throw new IllegalStateException("The location data source is not started yet");
        } else {
            this.a(minTime, minDistance);
            this.a(provider);
            if (this.mSelectedLocationProviders.isEmpty()) {
                throw new IllegalArgumentException(String.format("No provider found for the given name : %s", provider));
            } else {
                this.d();
            }
        }
    }

    protected void onStart() {
        Handler var1 = new Handler(this.mContext.getMainLooper());
        var1.post(new Runnable() {
            public void run() {
                Exception var1 = null;

                try {
                    GcjLocationDataSource.this.b();
                    if (GcjLocationDataSource.this.mCriteria != null) {
                        GcjLocationDataSource.this.a(GcjLocationDataSource.this.mCriteria);
                    } else if (GcjLocationDataSource.this.mProvider != null) {
                        GcjLocationDataSource.this.a(GcjLocationDataSource.this.mProvider);
                    } else {
                        GcjLocationDataSource.this.c();
                    }

                    if (GcjLocationDataSource.this.mSelectedLocationProviders.isEmpty()) {
                        throw new IllegalStateException(String.format("No provider found for the given name : %s", "selectedLocationProviders"));
                    }

                    GcjLocationDataSource.this.a();
                } catch (Exception var3) {
                    var1 = var3;
                }

                GcjLocationDataSource.this.onStartCompleted(var1);
            }
        });
    }

    private void a() {
        @SuppressLint("MissingPermission")
        android.location.Location var1 = this.mLocationManager.getLastKnownLocation((String) this.mSelectedLocationProviders.get(0));
        if (var1 != null) {
            var1.setSpeed(0.0F);
            var1.setBearing(0.0F);
            this.setLastKnownLocation(b(var1, true));
        }

        this.d();
        if (this.mAutoPanMode == LocationDisplay.AutoPanMode.COMPASS_NAVIGATION) {
            this.e();
        }

    }

    protected void onStop() {
        this.mLocationManager.removeUpdates(this.mInternalLocationListener);
        this.mInternalLocationListener = null;
        this.f();
    }

    @SuppressLint("WrongConstant")
    private void b() {
        if (this.mContext == null) {
            throw new IllegalArgumentException(String.format("Parameter %s must not be null", "context"));
        } else {
            this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        }
    }

    private void a(long var1, float var3) {
        if (var1 < 0L) {
            throw new IllegalArgumentException(String.format("Parameter %s is out of bounds", "minTime"));
        } else {
            this.mMinimumUpdateTime = var1;
            if (var3 < 0.0F) {
                throw new IllegalArgumentException(String.format("Parameter %s is out of bounds", "minDistance"));
            } else {
                this.mMinimumUpdateDistance = var3;
            }
        }
    }

    private void c() {
        if (this.mLocationManager.isProviderEnabled("network")) {
            this.mSelectedLocationProviders.add("network");
        }

        if (this.mLocationManager.isProviderEnabled("gps")) {
            this.mSelectedLocationProviders.add("gps");
        }

    }

    public final void onAutoPanModeChanged(LocationDisplay.AutoPanModeChangedEvent autoPanModeEvent) {
        this.mAutoPanMode = autoPanModeEvent.getAutoPanMode();
        if (this.isStarted()) {
            if (autoPanModeEvent.getAutoPanMode() == LocationDisplay.AutoPanMode.COMPASS_NAVIGATION) {
                this.e();
            } else {
                this.f();
            }

        }
    }

    @SuppressLint("MissingPermission")
    private void d() {
        if (this.mInternalLocationListener == null) {
            this.mInternalLocationListener = new b();
        }

        Iterator var1 = this.mSelectedLocationProviders.iterator();

        while (var1.hasNext()) {
            String var2 = (String) var1.next();
            this.mLocationManager.requestLocationUpdates(var2, this.mMinimumUpdateTime, this.mMinimumUpdateDistance, this.mInternalLocationListener);
        }

    }

    private void a(android.location.Location var1, boolean var2) {
        if (var1 != null) {
            if (this.mLastLocation != null) {
                double var3 = this.mLastLocation.getHorizontalAccuracy() * 2.0D;
                if ((double) var1.getAccuracy() > var3) {
                    return;
                }
            }
            Point point = GpsConvertUtils.gps84_To_Gcj02(var1.getLatitude(), var1.getLongitude());
            var1.setLatitude(point.getX());
            var1.setLongitude(point.getY());
            Location var5 = b(var1, var2);

            this.updateLocation(var5);
            this.mLastLocation = var5;
        }

    }

    private void a(Criteria var1) {
        if (var1 == null) {
            throw new IllegalArgumentException(String.format("Parameter %s must not be null", "criteria"));
        } else {
            String var2 = this.mLocationManager.getBestProvider(var1, true);
            if (var2 != null) {
                this.mSelectedLocationProviders.add(var2);
            }

        }
    }

    private void a(String var1) {
        if (this.mLocationManager.getAllProviders().contains(var1)) {
            this.mSelectedLocationProviders.add(var1);
        }

    }

    @SuppressLint("WrongConstant")
    private void e() {
        if (this.mSensorManager == null) {
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        }

        if (this.mInternalHeadingListener == null) {
            this.mInternalHeadingListener = new a();
        }

        this.mSensorManager.registerListener(this.mInternalHeadingListener, this.mSensorManager.getDefaultSensor(1), 2);
        this.mSensorManager.registerListener(this.mInternalHeadingListener, this.mSensorManager.getDefaultSensor(2), 2);
    }

    private void f() {
        if (this.mSensorManager != null && this.mInternalHeadingListener != null) {
            this.mSensorManager.unregisterListener(this.mInternalHeadingListener);
            this.mInternalHeadingListener = null;
        }

        this.updateHeading(0.0D / 0.0);
    }

    private static final Location b(android.location.Location var0, boolean var1) {
        if (var0 == null) {
            throw new IllegalArgumentException(String.format("Parameter %s must not be null", "location"));
        } else {
            Point var2 = new Point(var0.getLongitude(), var0.getLatitude(), SpatialReference.create(4326));
            double var3 = 0.0D / 0.0;
            Calendar var5 = d.a(var0.getTime());
            if (Build.VERSION.SDK_INT >= 26) {
                var3 = (double) var0.getVerticalAccuracyMeters();
            }

            return new Location(var2, (double) var0.getAccuracy(), var3, (double) var0.getSpeed(), (double) var0.getBearing(), var1, var5);
        }
    }

    private final class a implements SensorEventListener {
        private final float[] mGravity;
        private final float[] mGeomagnetic;
        private final float[] mR;
        private final float[] mI;
        private final float[] mOrientation;
        private float mHeading;
        private float mLastHeading;
        private final float RAD_2_DEG;
        private final float UPDATE_TOLERANCE;

        private a() {
            this.mGravity = new float[3];
            this.mGeomagnetic = new float[3];
            this.mR = new float[9];
            this.mI = new float[9];
            this.mOrientation = new float[3];
            this.RAD_2_DEG = 57.29578F;
            this.UPDATE_TOLERANCE = 7.5F;
        }

        public void onSensorChanged(SensorEvent event) {
            int var2 = event.sensor.getType();
            int var3;
            if (var2 == 1) {
                for (var3 = 0; var3 < 3; ++var3) {
                    this.mGravity[var3] = event.values[var3];
                }
            } else if (var2 == 2) {
                for (var3 = 0; var3 < 3; ++var3) {
                    this.mGeomagnetic[var3] = event.values[var3];
                }
            }

            boolean var5 = SensorManager.getRotationMatrix(this.mR, this.mI, this.mGravity, this.mGeomagnetic);
            if (var5) {
                SensorManager.getOrientation(this.mR, this.mOrientation);
                this.mHeading = this.mOrientation[0] * 57.29578F;
                if (this.mHeading < 0.0F) {
                    this.mHeading += 360.0F;
                }

                float var4 = Math.abs(this.mHeading - this.mLastHeading);
                if (var4 > 7.5F && var4 < 352.5F) {
                    this.mLastHeading = this.mHeading;
                    GcjLocationDataSource.this.updateHeading((double) this.mHeading);
                }
            }

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private final class b implements LocationListener {
        private android.location.Location mInnerAndroidLocation;

        private b() {
        }

        public void onLocationChanged(android.location.Location location) {
            GcjLocationDataSource.this.a(location, false);
            this.mInnerAndroidLocation = location;
        }

        public void onProviderEnabled(String provider) {
            if (GcjLocationDataSource.this.mSelectedLocationProviders.contains(provider)) {
                GcjLocationDataSource.this.d();
            }

        }

        public void onProviderDisabled(String provider) {
            if (GcjLocationDataSource.this.mSelectedLocationProviders.contains(provider) && GcjLocationDataSource.this.mSelectedLocationProviders.size() == 1) {
                GcjLocationDataSource.this.a(this.mInnerAndroidLocation, true);
            }

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (GcjLocationDataSource.this.mSelectedLocationProviders.contains(provider)) {
                if (status == 2) {
                    GcjLocationDataSource.this.d();
                } else {
                    GcjLocationDataSource.this.a(this.mInnerAndroidLocation, true);
                }
            }

        }
    }

}
