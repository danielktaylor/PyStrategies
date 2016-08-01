#!/usr/bin/python

import sys
import os
import gc
import numpy as np
import pandas as pd
import cPickle as pickle
import utils
import indicators as i

# Import PyLimitBook
sys.path.append(utils.get_pylimitbook_dir())
from researchBook import ResearchBook

# File format version number
FILE_FORMAT_VERSION = 1.4

data = {
    'time': [],
    'top_bid': [],
    'top_ask': [],
    'midpoint': [],
    'spread': [],
    'bid_volume': [],
    'ask_volume': [],
    'volume_imbalance': [],
    'midpoint_tick_delta': [],
    'order_count_bids': [],
    'order_count_asks': [],
    'order_count_total': [],
    'vwap': [],
    'inverse_vwap': [],
    ####TODO separate to different dataframe, or column per datapoint
    #'top_50_ask_levels': [],
    #'top_50_bid_levels': [],
    'last_trade_price_diff_bid': [],
    'last_trade_price_diff_ask': [],
    'last_trade_qty': [],
    'last_trade_age': [],
    }

def record(quotebook, last_tick):
    '''
    Point-in-time values
    '''
    global data

    data['time'].append(i.time(quotebook))
    data['top_bid'].append(i.top_bid(quotebook))
    data['top_ask'].append(i.top_ask(quotebook))
    data['midpoint'].append(i.midpoint(quotebook))
    data['spread'].append(i.spread(quotebook))
    data['bid_volume'].append(i.bid_volume(quotebook))
    data['ask_volume'].append(i.ask_volume(quotebook))
    data['volume_imbalance'].append(i.volume_imbalance(quotebook))
    data['midpoint_tick_delta'].append(i.midpoint_tick_delta(quotebook))
    data['order_count_bids'].append(i.order_count_bids(quotebook))
    data['order_count_asks'].append(i.order_count_asks(quotebook))
    data['order_count_total'].append(i.order_count_total(quotebook))
    data['vwap'].append(i.vwap(quotebook))
    data['inverse_vwap'].append(i.inverse_vwap(quotebook))
    ####TODO separate to different dataframe, or column per datapoint
    #data['top_50_ask_levels'].append(i.top_50_ask_levels(quotebook))
    #data['top_50_bid_levels'].append(i.top_50_bid_levels(quotebook))

    t_price, t_qty, t_age = i.last_trade_details(quotebook, last_tick)
    data['last_trade_price_diff_bid'].append(i.top_bid(quotebook) - t_price)
    data['last_trade_price_diff_ask'].append(i.top_ask(quotebook) - t_price)
    data['last_trade_qty'].append(t_qty)
    data['last_trade_age'].append(t_age)

def closest_forward_idx(idx, seconds, times):
    offsets = [times[idx] + (sec*1000) for sec in seconds]
    results = np.searchsorted(times, offsets, side='right')
    return [x-1 for x in results]

def compute():
    global data

    times = np.array(data['time'], dtype=int)

    # ---------- Offsets ----------

    # Calculate closest indexes of offsets
    # These are forward, not trailing, offsets
    desired_second_offsets = [1, 5, 10, 15, 30, 60, 120]

    # Create empty lists
    for sec in desired_second_offsets:
        key = "%i_sec_offset_idx" % sec
        data[key] = []

    # Calculate and append values
    last_time = -1
    last_offsets = None
    for idx, val in enumerate(times):
        # Caching of values
        if last_time == val:
            offsets = last_offsets
        else:
            offsets = closest_forward_idx(idx, desired_second_offsets, times)
            last_time = val
            last_offsets = offsets

        # Append points
        for i, sec in enumerate(desired_second_offsets):
            key = "%i_sec_offset_idx" % sec
            data[key].append(offsets[i])

    # ---------- Build Dataframe ----------

    return pd.DataFrame.from_dict(data, orient='columns')

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "usage: %s quotes.csv" % sys.argv[0]
        sys.exit(0)
    try:
        reader = open(sys.argv[1], 'r')
        quotebook = ResearchBook()
        for line in reader:
            if line[0] == 'B':
                last_tick = quotebook.bid(line.rstrip())
            elif line[0] == 'A':
                last_tick = quotebook.ask(line.rstrip())
            else:
                last_tick = quotebook.trade(line.rstrip())
            record(quotebook, last_tick)
        reader.close()
    except IOError:
        print 'Cannot open input file "%s"' % sys.argv[1]
        sys.exit(1)

    # Run computations
    df = compute()

    # Pack data and save to the cache directory
    gc.disable()
    packed = {
        'data' : df.to_msgpack(compress='blosc'),
        'format_version' : FILE_FORMAT_VERSION
        }

    filename = os.path.basename(sys.argv[1]).replace(".csv",".features")
    with open(os.path.join(utils.get_cache_dir(), filename), "wb") as outfile:
        pickle.dump(packed, outfile, 2)
    gc.enable()
