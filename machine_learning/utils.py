import os
import numpy as np
from ascii_graph import Pyasciigraph

def print_distribution_graph(data, title):
    y = np.bincount(data)
    ii = np.nonzero(y)[0]
    dist = [(str(x[0]), x[1]) for x in zip(ii,y[ii])]
    graph = Pyasciigraph(human_readable='si')
    for line in graph.graph(title, dist):
        print line
    print ""

def get_quotes_dir():
    script_dir = os.path.dirname(os.path.realpath(__file__))
    return os.path.join(script_dir, "../quotes/")

def get_cache_dir():
    script_dir = os.path.dirname(os.path.realpath(__file__))
    return os.path.join(script_dir, "cache/")
