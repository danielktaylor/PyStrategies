#!/usr/bin/python

import random
from mock import PropertyMock
from testfixtures import Replacer
from basestrategy import BaseStrategy
from basestrategy import Order

def run_test(fills, midpoint, expected, test_num):
    b = BaseStrategy()

    result = None
    with Replacer() as replace:
        m = replace('basestrategy.BaseStrategy.midpoint_price', PropertyMock(return_value=midpoint*10000))
        for qty, price in fills:
            client_order_id = str(random.randrange(1, 10000000))
            b._open_orders[client_order_id] = Order(client_order_id, 0, 0, None, None)
            b._fill("GOOG", client_order_id, qty, 0, str(price) + ".0000", 10000000)
        result = b._unrealized_pnl

    if result != expected * 10000:
        print "FAILED: #%s (expected %s but got %s)" % (str(test_num), str(expected), str(result/10000.0))

    else:
        print "PASSED: #%s" % str(test_num)

run_test([[-100, 9]], 10, -100, 1)
run_test([[100, 9]], 10, 100, 2)
run_test([[-100, 10]], 9, 100, 3)
run_test([[100, 10]], 9, -100, 4)
run_test([[-100, 9], [-200, 11]], 10, 100, 5)
run_test([[200, 10], [-100, 10], [200, 10]], 10, 0, 6)
run_test([[200, 11], [-100, 15], [200, 11]], 10, 100, 7)
run_test([[200, 9], [100, 8], [200, 9]], 10, 600, 8)
run_test([[200, 9], [200, 8], [-100, 9]], 10, 500, 9)
run_test([[-200, 9], [-100, 8], [100, 11]], 10, -500, 10)
run_test([[-100, 9], [200, 8], [-300, 11]], 10, 600, 11)
