import os, sys
import cPickle as pickle
import numpy as np
import pandas as pd
import preprocess
import utils

def print_results(labels, predictions):
    total = len(labels)
    num_correct = total - np.count_nonzero(np.subtract(predictions,labels))
    print "\n***** ACCURACY *****"
    print "Overall Accuracy: %.3f percent\n" % ((float(num_correct)/float(total)) * 100.0)

    results = pd.DataFrame()
    results['real'] = labels
    results['predicted'] = predictions

    for label in np.unique(labels):
        data = results[results['real'] == label]
        num_correct = len(data) - np.count_nonzero(data['real'].sub(data['predicted']))
        acc = ((float(num_correct)/float(len(data))) * 100.0)
        print "Total class label '%s' accuracy: %f percent" % (label, acc)
    print ""

    # Distribution graphs
    utils.print_distribution_graph(labels, 'Actual Distribution of Classes')
    utils.print_distribution_graph(predictions, 'Distribution of Predictions')

    # Distribution graphs for each class label
    for label in np.unique(labels):
        data = results[results['predicted'] == label]['real'].tolist()
        title = "When class label '%s' was predicted, the actual class was:" % label
        utils.print_distribution_graph(data, title)

def load_data(basepath, filename):
    print 'Loading data...'
    with open(os.path.join(basepath, filename), "rb") as p:
        return pickle.load(p)

def load_model(basepath):
    np.random.seed(1337)  # for reproducibility
    from keras.models import model_from_json

    model = model_from_json(open(os.path.join(basepath, 'model.json')).read())
    model.load_weights(os.path.join(basepath, 'weights.h5'))
    model.compile(loss='categorical_crossentropy',
                  optimizer='adam',
                  metrics=['accuracy'])
    return model

def analyze(basepath, filename):
    global LOOKBACK, MAX_MIDPOINT_DELTA

    # Load the data & model
    df = load_data(basepath, filename)
    model = load_model(basepath)

    # Process data
    samples, labels = preprocess.process_data(df)

    # Test model and print results
    print "Running analysis..."
    predictions = model.predict_classes(samples, batch_size=32)
    print_results(labels, predictions)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "usage: %s quotes.csv" % sys.argv[0]
        sys.exit(0)

    filename = os.path.basename(sys.argv[1]).replace(".csv", ".labels")
    analyze(utils.get_cache_dir(), filename)
