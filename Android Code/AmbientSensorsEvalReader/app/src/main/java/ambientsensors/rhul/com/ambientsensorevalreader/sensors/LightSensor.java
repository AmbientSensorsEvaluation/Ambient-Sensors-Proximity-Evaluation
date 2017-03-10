package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class LightSensor extends AbstractSensor {

    public LightSensor(Context context) {
//        super(context, Sensor.TYPE_LIGHT, SensorEnum.Light.name());
        super(context, Sensor.TYPE_LIGHT, "Light");
    }
}
