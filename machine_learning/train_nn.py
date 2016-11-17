import os, sys
import cPickle as pickle
import numpy as np
import pandas as pd
import utils
import preprocess

# File format version number
FILE_FORMAT_VERSION = 1.4

# Training configuration
BATCH_SIZE = 500
NUM_EPOCHS = 30

def load_data(basepath, filename):
    with open(os.path.join(basepath, filename), "rb") as p:
        packed = pickle.load(p)
        if packed['format_version'] != FILE_FORMAT_VERSION:
            print "Encountered unexpected cache file format version %s" % str(data['file_format_version'])
            sys.exit(1)
        return packed

def train_and_save(X_train, X_test, y_train, y_test, num_classes, basepath):
    global BATCH_SIZE, NUM_EPOCHS

    np.random.seed(1337) # for reproducibility
    from keras.utils import np_utils
    from keras.models import Sequential
    from keras.layers.core import Dense, Dropout, Activation

    print 'Convert class vector to binary class matrix (for use with categorical_crossentropy)'
    Y_train = np_utils.to_categorical(y_train, num_classes)
    Y_test = np_utils.to_categorical(y_test, num_classes)

    dimension = len(X_train[0])

    print 'Building model...'
    model = Sequential()
    model.add(Dense(dimension, input_shape=(dimension,)))
    model.add(Activation('relu'))
    model.add(Dropout(0.2))
    model.add(Dense(512))
    model.add(Activation('relu'))
    model.add(Dense(512))
    model.add(Activation('relu'))
    model.add(Dense(num_classes))
    model.add(Activation('softmax'))

    model.compile(loss='categorical_crossentropy',
                  optimizer='adam',
                  metrics=['accuracy'])

    history = model.fit(X_train, Y_train,
                        nb_epoch=NUM_EPOCHS, batch_size=BATCH_SIZE,
                        verbose=1, validation_split=0.1, shuffle=True)
    score = model.evaluate(X_test, Y_test,
                           batch_size=BATCH_SIZE, verbose=1)
    print 'Test score:', score[0]
    print 'Test accuracy:', score[1]

    print 'Saving model to cache directory...'
    json_string = model.to_json()
    open(os.path.join(basepath, 'model.json'), 'w').write(json_string)
    model.save_weights(os.path.join(basepath, 'weights.h5'), overwrite=True)

def learn(basepath, features_file, labels_file):
    # Load the data
    print 'Loading data...'
    features_data = pd.read_msgpack(load_data(basepath, features_file)['data'])
    labels_data = pd.read_msgpack(load_data(basepath, labels_file)['data'])
    df = pd.concat([features_data,labels_data], axis=1)

    # Process features
    samples, labels = preprocess.process_data(df)

    # How many samples are we going to leave out for the test set?
    nb_test = int(len(labels) * 0.2)
    split = len(labels) - nb_test

    # Prepare training and test sets
    X_train = np.array(samples[:split])
    y_train = labels[:split]
    X_test = np.array(samples[split+1:])
    y_test = labels[split+1:]
    print len(X_train), 'train sequences'
    print len(X_test), 'test sequences'

    # How many classes?
    num_classes = np.max(labels)+1
    print num_classes, 'classes'

    # Train Model
    train_and_save(X_train, X_test, y_train, y_test, num_classes, basepath)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "usage: %s quotes.csv" % sys.argv[0]
        sys.exit(0)

    features_file = os.path.basename(sys.argv[1]).replace(".csv", ".features")
    labels_file = os.path.basename(sys.argv[1]).replace(".csv", ".labels")
    learn(utils.get_cache_dir(), features_file, labels_file)
