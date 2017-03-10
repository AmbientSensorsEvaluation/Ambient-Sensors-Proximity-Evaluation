package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.content.Context;
import android.hardware.Sensor;

/**
 * Depending on the device this sensor can record cm or boolean whether it is close or not
 * <p/>
 * Boolean values (at least for Iakovos's device): 0.0: close and 5.0: far
 */
public class ProximitySensor extends AbstractSensor {

    public ProximitySensor(Context context) {
//        super(context, Sensor.TYPE_PROXIMITY, SensorEnum.Proximity.name());
        super(context, Sensor.TYPE_PROXIMITY, "Proximity");
    }
}
