
import sqlite3
class DataRetriever():
    def __init__(self, conn):
        self.conn = conn 
        self.c = conn.cursor()


    def get_measurement_ids(self, sensor):
        """ Get shared_ids for a given sensor """
        r = self.c.execute("SELECT shared_id FROM {0}".format(sensor))
        return set([ row[0] for row in r ])


    def get_measurements(self, sensor, place, common_ids):
        """ Get cancelled ids and measurements from DB for a given sensor and place """
        q = "SELECT * FROM {0} WHERE place='{1}'".format(sensor,place)
        try:
            results = self.c.execute(q)
        except Exception as e:
            print("SQL Execution Failed.  Now exiting.")
            exit()
        measurements = []
        total = 0
        for row in results:
            shared_id = row[4]
            if shared_id in common_ids and not shared_id in measurements:
                measurements.append((shared_id, row[5]))
            total += 1
        return (total, measurements)
