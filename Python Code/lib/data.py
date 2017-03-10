time_limit = 500

# Map of sensors to SI units
sensors = {'Accelerometer' : "ms^{-2}",
#           'GeomagneticRotationVector' : "No-Units",
           'Gyroscope' : "rads^{-1}",
           'Gravity' : "ms^{-2}",
           'MagneticField' : "\mu T",
#           'NetworkLocation' : "m",
           'LinearAcceleration' : 'ms^{-2}',
           'Light' : 'lux',
#           'Pressure' : "hPa",
#           'GPS' : 'm',
#           'Proximity' : 'cm',
#           'Bluetooth' : '<number of nearby devices>',
#           'WiFi' : '<number of nearby devices>',
#           'Sound' : 'dB (SPL)',
           'RotationVector' : "No-Units"}

# Tested places
places = {'BedfordLibrary',
          'FoundersDiningHall',
          'HubDiningHall',
          'QALab'}

# Sensors which produce only a single value per measurement
# (Efficiency purposes; we can skip redundant XML parsing
# with this)
single_vals = {'Pressure', 'Light', 'Sound'}
