package ambientsensors.rhul.com.ambientsensorevalreader.enums;

public enum SensorEnum {
    // Accelerometer,
    // Bluetooth,
    // GeomagneticRotationVector,
    // GPS,
    Gravity, // TODO: not working only on tablets
    // Gyroscope,
    // Humidity,
    Light, // TODO: to be included on second phase
    LinearAcceleration, // TODO: not working only on tablets
    // MagneticField,
    // NetworkLocation,
    // Pressure,
    Proximity, // TODO: to be included on second phase
    // RotationVector,
    Sound,
    // Temperature,
    // WiFi
    ;

    private static SensorEnum[] vals = values();

    public SensorEnum next() {
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
