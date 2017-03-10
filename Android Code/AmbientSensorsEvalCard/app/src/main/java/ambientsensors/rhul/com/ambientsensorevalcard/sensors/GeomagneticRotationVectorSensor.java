package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class GeomagneticRotationVectorSensor extends AbstractSensor {

    public GeomagneticRotationVectorSensor(Context context) {
//        super(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, SensorEnum.GeomagneticRotationVector.name());
        super(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, "GeomagneticRotationVector");
    }
}
