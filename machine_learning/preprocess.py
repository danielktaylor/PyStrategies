import numpy as np
import pandas as pd

OPEN_TIME  = 34200000  # 9:30 am
CLOSE_TIME = 57600000  # 4:00 pm

def process_data(df):
    # Configuration
    lookback = 100
    max_midpoint_delta = 7

    # Scale data to a good format for RELU activation function
    df['midpoint_delta_scaled'] = scale_zero_to_one(df['midpoint_tick_delta'], \
            -1 * max_midpoint_delta, max_midpoint_delta)
    df['time_scaled'] = scale_zero_to_one(df['time'], min(df.time), max(df.time))

    # Calculate rolling windows
    df['rolling_midpoint'] = rolling_window(df['midpoint_delta_scaled'], lookback).tolist()
    df['rolling_time'] = rolling_window(df['time_scaled'], lookback).tolist()

    # Discard rows outside of normal trading hours
    df = discard_non_market_hours(df, 'time')

    # Ensure even distribution of class labels
    # We do this even in testing for easier analysis of the results
    df = select_samples(df, 'label')

    # Shuffle data
    df = shuffle(df)

    # Create master feature array
    columns = ['rolling_midpoint','rolling_time']
    labels = df['label'].values
    samples = concat_df_columns(df, columns)

    # Return samples and labels
    return samples, labels

def concat_df_columns(df, columns):
    samples = pd.Series([[[]]*len(df)][0]) # series of empty arrays
    for column in columns:
        if isinstance(df[column][0], list):
            # lists
            samples = samples.add(df[column])
        else:
            # not lists
            print "********** ERROR: Non list values not supported: please store as arrays ********"
    return samples.tolist()

def rolling_window(data, window):
    '''
    Generate a rolling window
    '''
    d = data.values
    shape = d.shape[:-1] + (d.shape[-1] - window + 1, window)
    strides = d.strides + (d.strides[-1],)
    x = np.lib.stride_tricks.as_strided(d, shape=shape, strides=strides)
    return np.vstack((np.zeros((window-1,window)), x))

def discard_non_market_hours(df, time_column):
    '''
    Discard rows that are pre-open and post-close
    '''
    return df[(df[time_column] >= OPEN_TIME) & (df[time_column] <= CLOSE_TIME)]

def cap_extreme_values(data, minimum, maximum):
    '''
    Cap extreme values
    '''
    data[data < minimum] = minimum
    data[data > maximum] = maximum

def scale_zero_to_one(data, data_min, data_max):
    '''
    Scale numeric data to range [0,1]
    Data min/max give the possible range of the data (might not be same as sample min/max)
    '''
    return data.subtract(data_min).divide(data_max - data_min).astype("float32")

def shuffle(*argv):
    '''
    Randomly shuffle 1 or more dataframes.
    All dataframes get the same permutation, and they should be the same length
    '''
    ret = []
    order = np.random.permutation(argv[0].index)
    for df in argv:
        shuffled = df.reindex(order, copy=True)
        shuffled.index = range(len(df))
        ret.append(shuffled)
    # Return items
    if len(ret) == 1:
        return ret[0]
    return ret

def select_samples(df, class_column, *argv):
    '''
    Drop rows from dataframe so that each class label has an equal number of samples.
    If any other numpy arrays are passed in, the same indexes will be dropped
    '''
    # Make sure indexes start at zero, otherwise this fails
    df.index = range(len(df))

    # Get the count for the class with the least number of samples
    labels, counts = np.unique(df[class_column], return_counts=True)
    minimum_count = min(counts)

    # Calculate indexes to drop
    drop = []
    grouped = df.groupby(class_column)
    for idx, label in enumerate(labels):
        all_indexes = grouped.get_group(label).index.values
        num_drop = counts[idx] - minimum_count
        drop += np.random.choice(all_indexes, num_drop, replace=False).tolist()

    # Drop the indexes
    new = df.drop(df.index[drop], inplace=False)
    new.index = range(len(new))
    if len(argv) == 0:
        return new

    # Extra numpy arrays
    ret = [new]
    for arg in argv:
        ret.append(np.delete(arg, drop))
    return ret
