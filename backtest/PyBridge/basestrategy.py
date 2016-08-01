import sys
sys.path.append('../PyLimitBook')

from researchBook import ResearchBook
from tick import convert_price

class Order(object):
    def __init__(self, client_order_id, price, qty, order_type, placed_time):
        self.client_order_id = client_order_id
        self.price = price
        self.remaining_qty = qty
        self.original_qty = qty
        self.order_type = order_type
        self.placed_time = placed_time

    def got_fill(self, qty):
        self.remaining_qty -= qty

class Fill(object):
    def __init__(self, client_order_id, qty, remaining_qty, price, timestamp):
        self.client_order_id = client_order_id
        self.qty = qty                      # filled qty, positive (buy) or negative (sell)
        self.remaining_qty = remaining_qty  # remaining qty, positive (buy) or negative (sell)
        self.price = price
        self.timestamp = timestamp

class BaseStrategy(object):
    def __init__(self):
        self._log_signals = True
        self._log_location = "./signals_log.csv"
        self._reset_state()

    def __del__(self):
        if self._log_signals:
            self._log.close()

    def _reset_state(self):
        self.cents_multiplier = 1000
        self.dollars_multiplier = 10000

        # State Tracking
        self._actions = []
        self._book = ResearchBook()
        self._client_order_id = 0
        self._last_ask = None
        self._last_bid = None
        self._last_trade = None
        self._last_time = None
        self._shares_held = 0
        self._pending_orders = {}     # Orders sent to the market but not yet ack'd
        self._open_orders = {}        # Orders that are ack'd but not fully filled
        self._pending_cr = {}         # Cancel-replace requests sent but not yet ack'd
        self._fills = []
        self._unrealized_pnl = 0
        self._running_avg_price = 0   # Used for calculating unrealized PnL
        self._running_qty = 0         # Used for calculating unrealized PnL

        # Metrics
        self._metrics_enabled = False  # Disable expensive updates
        self._orders_placed = 0
        self._max_drawdown = 0
        self._current_pnl = 0

        # Signal Log
        if self._log_signals:
            self._log = open(self._log_location, 'w')
            self._log.write("timestamp,signal\n")

    # --- Handlers ---

    def _config(self, config_str):
        # Parse the config string
        # e.g. "var1=1.0;var2=5.0"
        config_dict = {}
        all_vars = config_str.split(";")

        for v in all_vars:
            if v == "":
                continue
            key, value = v.split("=")
            config_dict[key] = value

        # Dynamically update the strategy configuration variables
        for key, value in config_dict.iteritems():
            if key in self.all_configs:
                t = type(getattr(self, key))
                if t == long or t == int:
                    # ignore decimals
                    value = value.split(".")[0]
                setattr(self, key, t(value))

    def _bid(self, symbol, id_num, qty, price_str, timestamp):
        # Internal state updates
        self._last_bid = self._book.bid_split(symbol, id_num, qty, price_str, timestamp)
        self._last_time = timestamp

        # Call strategy
        self.on_bid()

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _ask(self, symbol, id_num, qty, price_str, timestamp):
        # Internal state updates
        self._last_ask = self._book.ask_split(symbol, id_num, qty, price_str, timestamp)
        self._last_time = timestamp

        # Call strategy
        self.on_ask()

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _trade_tick(self, symbol, qty, price_str, timestamp):
        # Internal state updates
        self._last_trade = self._book.trade_split(symbol, qty, price_str, timestamp)
        self._last_time = timestamp

        # Call strategy
        self.on_trade()

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _fill(self, symbol, client_order_id, qty, remaining_qty, price_str, timestamp):
        # Internal state updates
        self._last_time = timestamp
        self._shares_held += qty
        price = convert_price(price_str, False)

        # Fix the fact that remaining_qty is always positive
        if qty > 0:
            self._fills.append(Fill(client_order_id, qty, remaining_qty, price, timestamp))
        else:
            self._fills.append(Fill(client_order_id, qty, -1 * remaining_qty, price, timestamp))

        self._open_orders[client_order_id].got_fill(qty)
        if remaining_qty == 0:
            self._open_orders.pop(client_order_id, None)

        # Update unrealized PnL
        if self._shares_held == 0:
            self._unrealized_pnl = 0
            self._running_avg_price = 0
            self._running_qty = 0
        else:
            self._running_avg_price = ((self._running_qty * self._running_avg_price) + \
                (qty * price)) / float(qty + self._running_qty)
            self._running_qty += qty
            self._unrealized_pnl = long(round(self._shares_held * (self.midpoint_price - self._running_avg_price), 4))

        # Update metrics
        if self._metrics_enabled:
            # update PnL using average cost method
            sell_qty = 0
            buy_qty = 0
            buy_total_cost = 0
            sell_total_cost = 0
            for f in self._fills:
                if f.qty > 0:
                    buy_qty += f.qty
                    buy_total_cost += (f.qty * 1.0 * f.price)
                if f.qty < 0:
                    sell_qty += f.qty
                    sell_total_cost += (f.qty * 1.0 * f.price)

            if sell_qty != 0 and buy_qty != 0:
                avg_sell_price = sell_total_cost / (sell_qty * 1.0)
                avg_buy_price = buy_total_cost / (buy_qty * 1.0)

                smaller = buy_qty
                if abs(sell_qty) < buy_qty:
                    smaller = abs(sell_qty)
                self._current_pnl = (smaller * 1.0 * avg_sell_price) - \
                        (smaller * 1.0 * avg_buy_price)

            # Update max drawdown
            if self._current_pnl < self._max_drawdown:
                self._max_drawdown = self._current_pnl

        # Call strategy
        self.on_fill()

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _cancel_accepted(self, client_order_id, orig_client_order_id):
        # Internal state updates
        self._open_orders.pop(orig_client_order_id, None)

        # Call strategy
        self.on_cancel_accepted()

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _cancel_rejected(self, client_order_id):
        # Call strategy
        self.on_cancel_rejected()

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _cancel_replace_accepted(self, client_order_id, orig_client_order_id):
        # Internal state updates
        self._open_orders.pop(orig_client_order_id, None)
        order = self._pending_cr.pop(client_order_id, None)
        self._open_orders[client_order_id] = order

        # Call strategy
        self.on_cancel_replace_accepted()

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _cancel_replace_rejected(self, client_order_id):
        # Internal state updates
        self._pending_cr.pop(client_order_id, None)

        # Call strategy
        self.on_cancel_replace_rejected(client_order_id)

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _new_order_rejected(self, client_order_id):
        # Internal state updates
        self._pending_orders.pop(client_order_id, None)

        # Call strategy
        self.on_new_order_rejected(client_order_id)

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _new_order_accepted(self, client_order_id):
        # Internal state updates
        order = self._pending_orders.pop(client_order_id, None)
        self._open_orders[client_order_id] = order

        # Update metrics
        self._orders_placed += 1

        # Call strategy
        self.on_new_order_accepted(client_order_id)

        # Check for actions
        actions = self._actions
        self._actions = []
        return actions

    def _playback_end(self):
        if self._metrics_enabled:
            self.print_metrics();

    def _reset(self):
        if self._log_signals:
            self._log.close()
        self._reset_state()
        self.reset()


    # --- Actions ---

    def new_order(self, price, qty, order_type):
        # Order types: ['buy', 'sell', 'cover', 'short']
        client_order_id = self.act("NO", "", self.format_price(price), qty, order_type)
        self._pending_orders[client_order_id] = Order(client_order_id, price, qty, order_type, self._last_time)
        return client_order_id

    def new_market_order(self, qty, order_type):
        # Order types: ['buy', 'sell', 'cover', 'short']
        return self.new_order(0, qty, order_type)

    def cancel(self, orig_client_order_id):
        client_order_id = self.act("CO", orig_client_order_id, "", "", "")
        return client_order_id

    def cancel_replace(self, orig_client_order_id, price, qty, order_type):
        # Order type: [buy, sell, cover, short]
        client_order_id = self.act("CR", orig_client_order_id, self.format_price(price), qty, order_type)
        self._pending_cr[client_order_id] = Order(client_order_id, price, qty, order_type, self._last_time)
        return client_order_id

    def cancel_all(self):
        client_order_id = self.act("CA", "", "", "", "")
        return client_order_id


    # --- Properties ---

    @property
    def book(self):
        return self._book

    @property
    def last_bid(self):
        return self._last_bid

    @property
    def last_ask(self):
        return self._last_ask

    @property
    def last_trade(self):
        return self._last_trade

    @property
    def last_fill(self):
        return self._fills[-1]

    @property
    def last_time(self):
        return self._last_time

    @property
    def shares_held(self):
        return self._shares_held

    @property
    def open_orders(self):
        return self._open_orders

    @property
    def unrealized_pnl(self):
        return self._unrealized_pnl

    @property
    def midpoint_price(self):
        return self._book.midpoint_price


    # --- Utilities

    def debugger(self):
        import sys
        sys.stdout = sys.__stdout__
        from pudb import set_trace; set_trace()

    def log_signal(self, signal):
        if self._log_signals:
            self._log.write("%s,%s\n" % (self.format_time(self.last_time), str(signal)))

    def print_metrics(self):
        if self._metrics_enabled == False:
            print "Metrics disabled."
            return

        shares_traded = 0
        total_cost = 0
        net_shares = 0
        for s in self._fills:
            shares_traded += abs(s.qty)
            net_shares += s.qty
            total_cost += s.price * abs(s.qty)

        NEWLINE = "\n"
        metrics = "" + NEWLINE
        metrics += "----- Python Report -----" + NEWLINE
        metrics += "Orders Placed: " + str(self._orders_placed) + NEWLINE
        metrics += "Fill Count: " + str(len(self._fills)) + NEWLINE
        metrics += "Shares Traded: " + str(shares_traded) + NEWLINE
        metrics += "Open Order Count: " + str(len(self._open_orders)) + NEWLINE
        metrics += "Shares Held @ Close: " + str(net_shares) + NEWLINE
        if shares_traded == 0:
            metrics += "Avg Fill Price: 0.00" + NEWLINE
        else:
            metrics += "Avg Fill Price: " + str((total_cost / shares_traded) / 10000.0) + NEWLINE
        metrics += "Max Drawdown: " + str(self._max_drawdown / 10000.0) + NEWLINE
        metrics += "Total PnL: " + str(self._current_pnl / 10000.0) + NEWLINE
        print metrics

    def search_unfilled_orders(self, client_order_id):
        if client_order_id in self._open_orders:
            return self._open_orders[client_order_id]
        elif client_order_id in self._pending_orders:
            return self._pending_orders[client_order_id]
        elif client_order_id in self._pending_cr:
            return self._pending_cr[client_order_id]
        else:
            return None

    # --- Override These ---

    def on_ask(self):
    	pass

    def on_bid(self):
    	pass

    def on_trade(self):
    	pass

    def on_fill(self):
    	pass

    def on_cancel_accepted(self):
    	pass

    def on_cancel_rejected(self):
    	pass

    def on_cancel_replace_accepted(self):
    	pass

    def on_cancel_replace_rejected(self, client_order_id):
    	pass

    def on_new_order_rejected(self, client_order_id):
    	pass

    def on_new_order_accepted(self, client_order_id):
    	pass

    def reset(self):
    	pass


    # --- Internal Helpers ---

    def format_price(self, price):
        x = str(price)
        return x[:-4] + '.' + x[-4:]

    def act(self, action, orig_client_order_id, price, qty, order_type):
        client_order_id = self.get_next_client_order_id()
        self._actions.extend([action, client_order_id, orig_client_order_id,
                              str(price), str(qty), order_type])
        return client_order_id

    def get_next_client_order_id(self):
        self._client_order_id = self._client_order_id + 1
        return str(self._client_order_id)

    def format_time(self, t):
        x = t / 1000
        ms = t % x
        seconds = x % 60
        x = x/60
        minutes = x % 60
        x = x / 60
        hours = x % 24
        formatted = "%02d:%02d:%02d.%03d" % (hours, minutes, seconds, ms)
        return formatted

    def merge_dicts(*dict_args):
        '''
        Given any number of dicts, shallow copy and merge into a new dict,
        precedence goes to key value pairs in latter dicts.
        '''
        result = {}
        for dictionary in dict_args:
            result.update(dictionary)
        return result
