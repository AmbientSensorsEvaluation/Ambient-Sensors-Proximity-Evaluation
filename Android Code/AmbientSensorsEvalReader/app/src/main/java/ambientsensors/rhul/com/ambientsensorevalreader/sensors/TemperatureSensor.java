package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class TemperatureSensor extends AbstractSensor {

    public TemperatureSensor(Context context) {
//        super(context, Sensor.TYPE_AMBIENT_TEMPERATURE, SensorEnum.Temperature.name());
        super(context, Sensor.TYPE_AMBIENT_TEMPERATURE, "Temperature");
    }
}
