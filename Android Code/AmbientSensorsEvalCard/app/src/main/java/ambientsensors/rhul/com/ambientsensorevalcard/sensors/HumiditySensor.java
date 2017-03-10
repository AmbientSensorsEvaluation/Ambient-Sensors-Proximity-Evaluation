package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class HumiditySensor extends AbstractSensor {

    public HumiditySensor(Context context) {
        // super(context, Sensor.TYPE_RELATIVE_HUMIDITY, SensorEnum.Humidity.name());
        super(context, Sensor.TYPE_RELATIVE_HUMIDITY, "Humidity");
    }
}
