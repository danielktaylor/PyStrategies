# --- Data to track at each tick
# --- Must call most indicator functions at every tick for them to be accurate

t = {
    'last_midpoint_price' : None,
    'last_trade' : None,
}

# --- Utilities

empty_price_band_details = [[[0, 0, 0, 0]] * 50][0]

def price_band_details(level, curr_timestamp):
    '''
    Takes in a price band from the quotebook
    Returns: [quantity, number of orders, age of newest order, age of oldest order]
    '''
    oldest = curr_timestamp - level.head_order.tick.timestamp
    newest = curr_timestamp - level.tail_order.tick.timestamp
    return [level.volume, level.length, newest, oldest]

# --- Indicator functions

def time(quotebook):
    '''
    Time in milliseconds from midnight
    '''
    return quotebook.last_timestamp

def top_bid(quotebook):
    '''
    Top of bid book price
    '''
    return quotebook.top_bid_price

def top_ask(quotebook):
    '''
    Top of ask book price
    '''
    return quotebook.top_ask_price

def midpoint(quotebook):
    '''
    Bid-Ask midpoint price
    '''
    midpoint = quotebook.midpoint_price
    if midpoint == None:
        if quotebook.top_bid_price != None:
            return quotebook.top_bid_price
        elif quotebook.top_ask_price != None:
            return quotebook.top_ask_price
        else:
            return 0
    return midpoint

def spread(quotebook):
    '''
    Bid-Ask Spread
    '''
    spread = quotebook.spread
    if spread == None:
        return 0
    return spread

def bid_volume(quotebook):
    '''
    Unfiltered bid volume
    '''
    return quotebook.bid_volume

def ask_volume(quotebook):
    '''
    Unfiltered ask volume
    '''
    return quotebook.ask_volume

def volume_imbalance(quotebook):
    '''
    Bid-ask volume imbalance
    Positive value indicates bid volume > ask volume
    Negative value indicates ask volume > bid volume
    Zero value indicates bid volume == ask volume
    '''
    return bid_volume(quotebook) - ask_volume(quotebook)

def midpoint_tick_delta(quotebook):
    '''
    Difference between midpoint price now vs at last tick
    Positive value indicates midpoint price increased
    Negative value indicates midpoint price decreased
    Zero value indicates no change in midpoint price
    '''
    m = midpoint(quotebook)
    if t['last_midpoint_price'] == None:
        delta = 0
    else:
        delta = m - t['last_midpoint_price']
    t['last_midpoint_price'] = m
    return delta

def order_count_bids(quotebook):
    '''
    Count of total number of orders in the bid book
    '''
    return quotebook.bids_order_count

def order_count_asks(quotebook):
    '''
    Count of total number of orders in the ask book
    '''
    return quotebook.asks_order_count

def order_count_total(quotebook):
    '''
    Count of total number of orders on both sides of the book
    '''
    return order_count_bids(quotebook) + order_count_asks(quotebook)

def vwap(quotebook):
    '''
    Volume-weighted midpoint price
    '''
    bid_vol = bid_volume(quotebook)
    ask_vol = ask_volume(quotebook)
    vol = bid_vol + ask_vol

    if bid_vol == 0 or ask_vol == 0:
        return midpoint(quotebook)

    ratio = bid_vol / float(vol)
    return top_bid(quotebook) + (spread(quotebook) * ratio)

def inverse_vwap(quotebook):
    '''
    Inverse-volume-weighted midpoint price
    Weight bid price by ask volume, and vice versa
    '''
    bid_vol = bid_volume(quotebook)
    ask_vol = ask_volume(quotebook)
    vol = bid_vol + ask_vol

    if bid_vol == 0 or ask_vol == 0:
        return midpoint(quotebook)

    ratio = ask_vol / float(vol)
    return top_bid(quotebook) + (spread(quotebook) * ratio)

def top_50_ask_levels(quotebook):
    '''
    Return an ordered list of the top 50 price levels in the ask book
    First index is top of the book. Each level represents a 1-cent change; some levels be empty.
    Data returned at each level: [quantity, number of orders, age of newest order, age of oldest order]
    '''
    levels = empty_price_band_details[:]
    current_time = time(quotebook)
    best_price = top_ask(quotebook)
    worst_price = top_ask(quotebook) + (49 * 100)
    for price, orders in quotebook.asks.price_map.iteritems():
        if price > worst_price:
            continue
        idx = (price - best_price)/100
        levels[idx] = price_band_details(orders, current_time)
    return levels

def top_50_bid_levels(quotebook):
    '''
    Return an ordered list of the top 50 price levels in the bid book
    First index is top of the book. Each level represents a 1-cent change; some levels be empty.
    Data returned at each level: [quantity, number of orders, age of newest order, age of oldest order]
    '''
    levels = empty_price_band_details[:]
    current_time = time(quotebook)
    best_price = top_bid(quotebook)
    worst_price = top_bid(quotebook) - (49 * 100)
    for price, orders in quotebook.bids.price_map.iteritems():
        if price < worst_price:
            continue
        idx = (best_price - price)/100
        levels[idx] = price_band_details(orders, current_time)
    return levels

def last_trade_details(quotebook, last_tick):
    '''
    Returns last trade price, volume, age in milliseconds
    '''
    trade = t['last_trade']
    if str(type(last_tick)) == "<class 'tick.Trade'>":
        t['last_trade'] = last_tick
        trade = last_tick

    if trade == None:
        return 0, 0, 0
    return trade.price, abs(trade.qty), time(quotebook) - trade.timestamp
