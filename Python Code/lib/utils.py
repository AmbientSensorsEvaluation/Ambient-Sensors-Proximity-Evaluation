
import numpy as np
import re
import math

from haversine import haversine
from data import sensors
from scipy.interpolate import spline, interp1d
from scipy.stats import spearmanr
from itertools import izip
from lxml import etree


def remove_cancelled_data(c_cancelled_ids, c_data, r_cancelled_ids, r_data):
    r_data = [ r for r in r_data
               if not r[0] in c_cancelled_ids
               and not r[0] in r_cancelled_ids ]
    c_data = [ c for c in c_data
               if not c[0] in c_cancelled_ids
               and not c[0] in r_cancelled_ids ]
    return (c_data, r_data)


def find_shared_ids(card_ids, reader_ids):
    """ Find differening shared_ids from two sets """
    card_ids, reader_ids = set(card_ids), set(reader_ids)
    return card_ids & reader_ids
    
def compute_percentage_similarity(x1, y1, x2, y2):
    """ Percentage similarity between y1 and y2 """
    _, y1_in, y2_in = interpolate(x1, y1, x2, y2)
    total = 0
    for y1, y2 in izip(y1_in, y2_in):
        if y1 > y2:
            total += y2 / y1
        else:
            total += y1 / y2
    return 100. * (total / len(y1_in))


def magnitude(arr):
    """ Compute magnitude of an array """
    total = 0
    for val in arr:
        total += (val * val)
    return math.sqrt(total)


def compute_avg_threshold_loc(c_loc, r_loc):
    """ Return haversine distance between two locations in meters """
    return haversine(c_loc, r_loc) * 1000


def mean_loc(ms):
    """ Compute mean lat and lon from set of coordinates """
    total_lat = 0.0
    total_lon = 0.0
    len_ms = float(len(ms))
    for m in ms:
        total_lat += m[1][1]
        total_lon += m[2][1]
    return ( total_lat/len_ms, total_lon/len_ms )


def compute_corr(x1, y1, x2, y2):
    """ Computes Pearson's correlation coefficient """
    # Correlation is undefined for single points
    if (len(x1) <= 1 or len(x2) <= 1
    or len(y1) <= 1 or len(y2) <= 1):
        return np.nan
    xnew, y1_in, y2_in = interpolate(x1, y1, x2, y2)
    return np.corrcoef(y1_in, y2_in)[0][1]


def interpolate_straight_line(x, y, xmin, xmax, xlen):
    xnew1 = np.linspace(xmin, xmax, num=xlen)
    y1 = np.repeat(y[0], xlen)


def delete_invalid_indices(x, y):
    indices = [ i for i, ele in enumerate(x)
                if ele > 500 ]
    return (np.delete(x, indices),
            np.delete(y, indices))
    

def compute_avg_threshold(x1, y1, x2, y2, a=False):
    """ Mean absolute error; cx, cy, rx, ry """
    if len(x1) == 1 and len(x2) == 1:
        return np.abs(y2[0] - y1[0])

    if np.any(x2>500):
        # Fix invalid data from Android app
        # Delete any values over time limit
        x2, y2 = delete_invalid_indices(x2, y2)
    elif np.any(x1>500):
        x1, y1 = delete_invalid_indices(x1, y1)

    if len(x1) == 1 and len(x2) > 1:
        # Draw straight line for x1,y1
        pass
    elif len(x1) > 1 and len(x2) > 1:
        _, y1_in, y2_in = interpolate(x1, y1, x2, y2)
    else:
        # Bug with light
        # Interpolate only y1    
        xnew1 = np.linspace(x1.min(), x1.max())
        y1_in = spline(x1, y1, xnew1)
        x2 = xnew1
        y2 = np.repeat(y2[0], len(y1_in))
        y2_in = y2
    return np.mean(np.abs(y2_in - y1_in))


def remove_duplicates(x1, y1):
    seen = []
    delete_indices = []
    to_remove_y = []
    for i, x in enumerate(zip(x1, y1)):
        if not x[0] in seen:
            seen.append(x[0])
        else:
            delete_indices.append(i)

    x1_new = np.delete(x1, delete_indices)
    y1_new = np.delete(y1, delete_indices)
    return (x1_new, y1_new)


def interpolate(x1, y1, x2, y2, a=False):
    # Generate 50 measurements equally between 0-500ms
    # Only measure from the min to max recorded points

    # On very rare occasions, two measurements are produced
    # at the same time interval for the same device.
    # Delete these to prevent incorrect results later on
    x1, y1 = remove_duplicates(x1, y1)
    x2, y2 = remove_duplicates(x2, y2)
    x_min = max(x1.min(), x2.min())
    x_max = min(x1.max(), x2.max())
    xnew1 = np.linspace(x_min, x_max)
    xnew2 = np.linspace(x_min, x_max)

    if len(x2) == 2:
        x2 = np.linspace(x2.min(), x2.max())
        y2 = np.linspace(y2.min(), y2.max())

    y1_smooth = spline(x1, y1, xnew1)
    if len(x2) == 2:
        y2_smooth = y2
    else:
        y2_smooth = spline(x2, y2, xnew2)
    assert len(y1_smooth) == len(y2_smooth)
    return (xnew1, y1_smooth, y2_smooth)

    
def parse_measurement_data(xml, loc=False, isfile=False):
    """ Returns a list containing timestamps and a list of data vals """
    # Remove XML declaration
    timestamps, data = [], []
    if isfile:
        root = etree.parse(xml).getroot()
    else:
        xml = re.sub('\<\?xml.*\?\>', '', xml)
        root = etree.fromstring(xml)

    for m in root.findall('measurement'):
        data_temp = []
        for child in m:
            if child.tag == 'timestamp':
                timestamps.append(child.text)
            elif child.tag.startswith('data'):
                data_temp.append(child.text)
            elif loc and (child.tag.startswith('altitude')
                          or child.tag.startswith('latitude')
                          or child.tag.startswith('longitude')
                          or child.tag.startswith('accuracy')):
                data_temp.append((child.tag, float(child.text)))
        if data_temp and not loc:
            # Convert data from str -> float
            data.append(map(float, data_temp))
        elif data_temp and loc:
            data.append(data_temp)
            
    timestamps = map(float, timestamps)
    if timestamps and data and not loc:
        # Timestamps: str -> float
        return (np.array(timestamps),
                np.array(data))
    else:
        return (timestamps, data)

