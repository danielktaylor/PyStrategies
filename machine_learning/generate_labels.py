#!/usr/bin/python

import sys, os, gc
import numpy as np
import pandas as pd
import cPickle as pickle
import utils

# Input file version
FILE_FORMAT_VERSION = 1.4

def bucket_price_delta(pt):
    if pt == None:
        print "Null value encountered -- Aborting!"
        sys.exit(1)
    elif pt < -200:
        return 1
    elif pt < -100:
        return 2
    elif pt < 0:
        return 3
    elif pt == 0:
        return 4 # no change
    elif pt < 100:
        return 5
    elif pt < 200:
        return 6
    else:
        return 7

def calculate(df):
    # We are going to predict midpoints 10 seconds in the future
    predictions = { 'label': [] }
    for idx, row in df.iterrows():
        offset_idx = row['10_sec_offset_idx']
        pt = df['midpoint'][offset_idx] - row['midpoint']
        predictions['label'].append(bucket_price_delta(pt))

    # Print distribution of classes
    computed = pd.DataFrame.from_dict(predictions, orient='columns')
    utils.print_distribution_graph(computed['label'], 'Distribution of Label Buckets')
    return computed

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "usage: %s quotes.csv" % sys.argv[0]
        sys.exit(0)
    try:
        # Load file from ./cache directory
        filename = os.path.basename(sys.argv[1]).replace(".csv",".features")
        gc.disable()
        packed = pickle.load(open(os.path.join(utils.get_cache_dir(), filename), 'rb'))
        df = pd.read_msgpack(packed['data'])
        gc.enable()
        if packed['format_version'] != FILE_FORMAT_VERSION:
            print "Encountered unexpected cache file format version %s" % str(data['file_format_version'])
            sys.exit(1)
    except IOError:
        print 'Cannot open features cache file "%s"' % cache
        sys.exit(1)

    # Calculate labels
    labels = calculate(df)

    # Pack data and save to the cache directory
    gc.disable()
    packed = {
        'data' : labels.to_msgpack(compress='blosc'),
        'format_version' : FILE_FORMAT_VERSION
        }

    filename = os.path.basename(sys.argv[1]).replace(".csv",".labels")
    with open(os.path.join(utils.get_cache_dir(), filename), "wb") as outfile:
        pickle.dump(packed, outfile, 2)
    gc.enable()
