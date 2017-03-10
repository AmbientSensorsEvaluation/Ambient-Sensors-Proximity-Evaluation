package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class LocationSensor extends AbstractSensor {

    public static final int TYPE_GPS = 0;
    public static final int TYPE_NETWORK = 1;
    private Context context;
    private LocationManager mLocationManager;
    private List<Location> values;

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            values.add(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
    private String type;

    public LocationSensor(Context context, int type) {
//        super(context, -1, (type == TYPE_GPS) ? SensorEnum.GPS.name() : SensorEnum.NetworkLocation.name());
        super(context, -1, (type == TYPE_GPS) ? "GPS" : "NetworkLocation");
        this.context = context;
        this.values = new ArrayList<>();

        if (type == TYPE_GPS)
            this.type = LocationManager.GPS_PROVIDER;
        else
            this.type = LocationManager.NETWORK_PROVIDER;
    }

    @Override
    protected void stopSensor() {
        try {
            mLocationManager.removeUpdates(locationListener);
        } catch (SecurityException e) {
            // App will be pre-installed
        }
    }

    @Override
    public void generateData(String rnd_id) {
        if (!mLocationManager.isProviderEnabled(this.type))
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainAct.noGPS();
                }
            });
        else {
            long initT = 0;

            if (values.size() > 0)
                initT = values.get(0).getTime();

            String data = "<measurements>";

            for (Location i : values) {
                data += "<measurement>";
                data += "<timestamp>" + (i.getTime() - initT) + "</timestamp>";
                data += "<altitude>" + i.getAltitude() + "</altitude>";
                data += "<latitude>" + i.getLatitude() + "</latitude>";
                data += "<longitude>" + i.getLongitude() + "</longitude>";
                data += "<accuracy>" + i.getAccuracy() + "</accuracy>";
                data += "</measurement>";
            }

            data += "</measurements>";

            db.writeToDB(mainAct.getLocation().name(), startTime, rnd_id, data);
        }
    }

    @Override
    protected void initSensor() {
        mLocationManager = (LocationManager) mainAct.getSystemService(Context.LOCATION_SERVICE);
        try {
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (mainAct.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && mainAct.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                    mLocationManager.requestLocationUpdates(type, 0, 0, locationListener);
                }
            });
        } catch (SecurityException e) {
            // App will be pre-installed
        }
    }
}
