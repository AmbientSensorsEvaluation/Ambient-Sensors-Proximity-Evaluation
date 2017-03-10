
import numpy as np

from lib.grapher import graph_eer
from lib.utils import compute_avg_threshold


def find_eer(fpr, fnr, ts):
    """ Returns the threshold of the EER and the EER value """
    # Sanity check
    assert len(fpr) == len(fnr)
    for i, t in enumerate(ts):
        # Find FPR and FNR crossover
        if fpr[i] > fnr[i]:
            return (t, fpr[i])
    return "NONE"    


def compute_eer(t, pre_ts):
    true_pos = 0
    true_neg = 0
    false_neg = 0
    false_pos = 0
    for i, t1 in enumerate(pre_ts):
        for j, t2 in enumerate(pre_ts[i]):
            if np.isnan(pre_ts[i][j]):
                continue
            if i == j:
                # Legitimate transaction
                if pre_ts[i][j] <= t:
                    # True positive
                    true_pos += 1
                else:
                    # False negative
                    false_neg += 1
            else:
                # Different Transaction
                if pre_ts[i][j] <= t:
                    false_pos += 1
                else:
                    true_neg += 1
    tpr = true_pos / float(true_pos + false_neg)
    tnr = true_neg / float(true_neg + false_pos)
    fnr = 1.0 - tpr
    fpr = 1.0 - tnr
    return (tpr, tnr, fpr, fnr)
    
