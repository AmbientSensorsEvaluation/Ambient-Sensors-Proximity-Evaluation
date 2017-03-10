
import re
import sys
import sqlite3
import numpy as np
import multiprocessing as mp

from lib.data import *
from lib.utils import *
from lib.retriever import DataRetriever
from lib.grapher import *
from lib.stats import compute_eer, find_eer

usage = "Usage: python analyser.py <path to card DB> <path to reader db> <path to output file>"

seen = []
msg_sens = []
sensor_place_data = dict()
sensor_measures = dict()

def compute_threshold(place, sensor, c_ts, c_ms, r_ts, r_ms, loc=False):
    """ Returns the average threshold for a set of measurements, along with max and min vals """
    global seen
    global sensor_place_data
    
    # Process geolocations differently to rest
    if loc:
        # Almost always only a single location is retrieved in time.
        # In case more are returned, err on the side of caution by
        #
        if len(c_ts) > 1 and len(c_ms) > 1:
            del c_ms[1]
            del c_ts[1]
        if len(r_ts) > 1 and len(r_ms) > 1:
            del r_ms[1]
            del r_ts[1]
        sensor_place_data[sensor].append([c_ts, c_ms, r_ts, r_ms])
        return compute_avg_threshold_loc(mean_loc(c_ms), mean_loc(r_ms))
    # Remove dud data values not removed in the apps        
    if sensor in single_vals:
        c_ms = np.array([ m[0] for m in c_ms ])        
        r_ms = np.array([ m[0] for m in r_ms ])
    elif not loc:
        c_ms = np.array([ magnitude(m) for m in c_ms ])
        r_ms = np.array([ magnitude(m) for m in r_ms ])

    if not loc and not sensor in seen:
        # Graph sensor
        # UNCOMMENT!
        #if sensor == 'Accelerometer':
        #    graph_sensor(place, sensor, c_ts, c_ms, r_ts, r_ms)
        #    exit()
        seen.append(sensor)

    # Store card and reader values for future analysis
    sensor_place_data[sensor].append([c_ts, c_ms, r_ts, r_ms])
    t = compute_avg_threshold(c_ts, c_ms, r_ts, r_ms)
    # Get threshold, min and max values for card and reader
    return (t, c_ms.min(), c_ms.max(), r_ms.min(), r_ms.max())
        

def analyse(c_data, r_data, sensor, place):
    """ Analyses all card and reader transactions for a given sensor and place """
    # Analyse shared card and reader measurements if values exist
    # and track 'good' values: those containing measurements
    thresholds = []
    c_mins, c_maxs = [], []
    r_mins, r_maxs = [], []
    good = 0
    loc = (sensor == 'NetworkLocation')

    # For each transaction
    for c, r in zip(c_data, r_data):
        # Same shared id
        assert c[0] == r[0]
        # NetworkLocation is parsed differently to rest
        loc = (sensor == 'NetworkLocation')
        c_times, c_vals = parse_measurement_data(c[1], loc)
        r_times, r_vals = parse_measurement_data(r[1], loc)
        if len(c_vals) > 0 and len(r_vals) > 0:
            good += 1
            if loc:
                # Process locations differently
                thresholds.append(compute_threshold(place, sensor, c_times, c_vals, r_times, r_vals, loc))
            else:
                thresh, c_min, c_max, r_min, r_max = compute_threshold(place, sensor, c_times, c_vals, r_times, r_vals, loc)
                thresholds.append(thresh)
                c_mins = np.append(c_mins, c_min)
                c_maxs = np.append(c_maxs, c_max)
                r_mins = np.append(r_mins, r_min)
                r_maxs = np.append(r_maxs, r_max)

    return (good, np.array(thresholds), c_mins, c_maxs, r_mins, r_maxs)


def store_data(sensor, output_dir):
    """ % good values and sensor data in file """
    path_str = "{0}/{1}_full.txt".format(output_dir, sensor)
    print("Storing data at {0}...".format(path_str))
    msg = "\nUnits: {0}".format(sensors[sensor])

    global sensor_measures
    msg += "\n(Successful, total) measurements: {0}".format(sensor_measures[sensor])
    try:
        with open(path_str, 'w') as f:
            f.write(msg)
    except Exception as e:
        print("Failed to write to database!")
        print(e.message)
    print("Done!")
    print
    

def run(card_conn, reader_conn, output_dir):
    """ Fetch and pre-process data from card and reader databases """
    # Get database objects for card and reader
    card_db = DataRetriever(card_conn)
    reader_db = DataRetriever(reader_conn)

    skip_sensors = []
    card_ids, reader_ids = {}, {}
    common_ids = {}
    # Get shared IDs for all DBs
    for sensor in sensors.keys():
        card_ids[sensor] = card_db.get_measurement_ids(sensor)
        reader_ids[sensor] = reader_db.get_measurement_ids(sensor)
        common_ids[sensor] = find_shared_ids(card_ids[sensor], reader_ids[sensor])

    

    # Initialise
    global sensor_place_data
    global sensor_measures
    for sensor in sensors.keys():
        if not sensor in skip_sensors:
            sensor_place_data[sensor] = []
            sensor_measures[sensor] = [0, 0]
    
    for place in places:
        for sensor in sensors.keys():
            # Skip unavailable sensors
            if sensor in skip_sensors:
                print("Skipping {0} in {1}".format(sensor, place))
                continue

            c_total, c_data = card_db.get_measurements(sensor, place, common_ids[sensor])
            r_total, r_data = reader_db.get_measurements(sensor, place, common_ids[sensor])
               
            if not c_data and not r_data:
                print("Skipping {0} in {1}".format(sensor, place))
                continue

            print("Processing data for {0} in {1}...".format(sensor, place))

            # Remove cancelled data if necessary
            #if c_canc_ids or r_canc_ids:
            #    c_data, r_data = remove_cancelled_data(c_canc_ids, c_data, r_canc_ids, r_data)
            
            # Sanity check: check number of transactions are same across devices
            assert len(c_data) == len(r_data)

            # Analyse all measurements for a given sensor and place
            good, thresholds, c_mins, c_maxs, r_mins, r_maxs = analyse(c_data, r_data, sensor, place)
            
            #if thresholds != []:
                #place_sensor_thresholds[sensor][place] = thresholds
            
            # Tuple/ratio of successful vals:total vals
            new_fst = sensor_measures[sensor][0] + good
            new_snd = sensor_measures[sensor][1] + len(c_data)
            sensor_measures[sensor] = [new_fst, new_snd]

    for sensor in sensors.keys():
        if not sensor in skip_sensors:
            store_data(sensor, output_dir)
    do_eers()            

def pre_compute_ts(data, sensor, lock):
    """ Pre-computes thresholds for a given sensor and its associated transactions """
    pre_ts = np.zeros((len(data), len(data)))
    print("Pre-computing thresholds...")
    # Pre compute thresholds
    # Data is in the form [card_xs, card_ys, reader_xs, reader_ys]
    if sensor == 'NetworkLocation':
        for i, t1 in enumerate(data):
            for j, t2 in enumerate(data):
                pre_ts[i][j] = compute_avg_threshold_loc(
                    mean_loc(t2[1]),
                    mean_loc(t1[3]))
            lock.acquire()
            print("{0}: {1}".format(sensor, i))
            lock.release()
    else:
        for i, t1 in enumerate(data):
            flag = False
            if np.all(t1[1]==0) or np.all(t1[3]==0):
                flag = True
            for j, t2 in enumerate(data):
                if flag:
                    #Undefined
                    pre_ts[i][j] = np.nan
                    continue
                else:
                    pre_ts[i][j] = compute_corr(
                        t2[0], t2[1],
                        t1[2], t1[3])
            lock.acquire()
            print("{0}: {1}".format(sensor, i))
            lock.release()
    print("Done!")
    lock.acquire()
    np.save('pre/{0}_corr_pre.npy'.format(sensor), pre_ts)
    lock.release()


def start_pre_compute():
    global sensor_place_data
    l = mp.Lock()
    pool = []
    for sensor in sensor_place_data.keys():
        data = sensor_place_data[sensor]
        if data != []:
            p = mp.Process(target=pre_compute_ts, args=(data, sensor, l),)
            pool.append(p)
    for p in pool: p.start()
    for p in pool: p.join()


def do_eers():
    # Graph EERSs
    print("Computing EERs...")

    # Erase existing EER file
    with open('eers/eer_corr.csv', 'w') as f:
        pass

    # Pre-compute thresholds
    #print("Starting pre-compute...")
    #start_pre_compute()

    global sensor_place_data
    for sensor in sensor_place_data.keys():
        false_negs, false_poss = [], []
        true_negs, true_poss = [], []
        if sensor_place_data[sensor] != []:
            data = sensor_place_data[sensor]
            # Load pre_ts
            print("Loading pre-computed matrix...")
            pre_ts = np.load('pre/{0}_corr_pre.npy'.format(sensor))
                              
            # Data stored as [card_xs, card_ys, reader_xs, reader_ys]
            # for each transaction
            ts = np.linspace(-1, 1, num=50)
            print("Computing EER for {0}...".format(sensor))

            for i, t in enumerate(ts):
                tpr, tnr, fpr, fnr = compute_eer(t, pre_ts)
                true_poss.append(tpr)
                true_negs.append(tnr)
                false_poss.append(fpr)
                false_negs.append(fnr)

            # Graph
            print("Finding EER for {0}...".format(sensor))
            eer = find_eer(false_poss, false_negs, ts)
            eer_str = '{0},{1},{2}\n'.format(sensor, eer[1], eer[0])
            with open('eers/eer_corr.csv', 'a') as f:
                f.write(eer_str)
            #graph_eer(sensor, ts, false_negs, false_poss, corr=True)
            print("Done!")


if __name__ == '__main__':
    # Process args and run
    args = sys.argv
    if len(args) < 4:
        print("Insufficient arguments!")
        print(usage)
        exit(1)
    else:
        card_path = sys.argv[1]
        reader_path = sys.argv[2]
        output_dir = sys.argv[3]
        if not ("Card" in sys.argv[1] and "Reader" in sys.argv[2]):
            print("Incorrect arguments: are you sure they're in the right order?")
            print(usage)
            exit(1)
        else:
            print("Running...")
            # Establish database connections
            card_conn = sqlite3.connect(card_path)
            reader_conn = sqlite3.connect(reader_path)
            run(card_conn, reader_conn, output_dir)
