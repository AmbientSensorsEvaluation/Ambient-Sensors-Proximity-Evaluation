import matplotlib
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
import numpy as np
import scipy.stats as stats

from lib.data import sensors, places
from scipy.interpolate import spline
from scipy.stats import histogram

def graph_eer(sensor, ts, false_negs, false_poss, corr=False):
    if corr:
        path = "eers/{0}_corr.eps".format(sensor)
    else:
        path = "eers/{0}.eps".format(sensor)
    plt.plot(ts, false_negs, 'b', label="False Negative", aa=True)
    plt.plot(ts, false_poss, 'r--', label="False Positive", aa=True)
    plt.title("{0}".format(sensor))
    if corr:
        xlabel = "Threshold"
    else:
        xlabel = "Threshold (${0}$)".format(sensors[sensor])
    plt.xlabel(xlabel)
    plt.ylabel("Rate")
    plt.xlim(ts.min(), ts.max())
    plt.legend()
    plt.grid(True)
    plt.savefig(path, format='eps', bbox_inches='tight')
    plt.close()


def graph_sensor(place, sensor_name, cx, cy, rx, ry):
    path = "graphs/{0}".format(sensor_name)
    print("Saving {0} graph to {1}".format(sensor_name,path))

    # Plot continuous graph via interpolation
    #xnew1 = np.linspace(cx.min(), cx.max(), num=100)
    #xnew2 = np.linspace(rx.min(), rx.max(), num=100)
    #vals_smooth1 = spline(cx, cy, xnew1)
    #vals_smooth2 = spline(rx, ry, xnew2)

    # Plot continuous curves
    #plt.plot(xnew1, vals_smooth1, 'k--', label="Card (Continuous)")
    #plt.plot(xnew2, vals_smooth2, 'r--', label="Reader (Continuous)")
    # Plot discrete points
    plt.plot(cx, cy, 'r+', markeredgewidth=3, markersize=12, label='Card (Sampled)')
    plt.plot(rx, ry, 'b^', markeredgewidth=3, markersize=12, label='Reader (Sampled)')

    fontP = FontProperties()
    fontP.set_size('small')
    plt.legend(prop=fontP, loc=9, bbox_to_anchor=(0.5, -0.1), ncol=2)
    plt.xlim(0, 500)
    plt.xlabel("Time (milliseconds)")
    # Get units
    plt.ylabel("Values (${0}$)".format(sensors[sensor_name]))
    plt.grid(True)
    #plt.title("Plot of {0} Sensor in {1}".format(sensor_name, place))
    plt.savefig(path, bbox_inches='tight')
    plt.close()

def graph(threshs):
    plt.plot(threshs)
    plt.show()
    plt.close()
