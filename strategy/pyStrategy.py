import sys
sys.path.append('../backtest/PyBridge')

from basestrategy import BaseStrategy

class StrategyPlugin(BaseStrategy):
    def __init__(self):
        BaseStrategy.__init__(self)

        # Strategy configuration
        self.all_configs = {
            'max_position'        : 1000,
            'max_pnl_loss'        : 50 * self.cents_multiplier,
            'max_pnl_gain'        : 90 * self.cents_multiplier,
            'min_time'            : 1 * self.book.millis_in_second,
            'min_cents'           : 2 * self.cents_multiplier,
            'cooldown'            : 2 * self.book.millis_in_second,
            'max_open_order_time' : 10 * self.book.millis_in_second,
            'closeout_freshness'  : (1.0 / 2.0) * self.book.millis_in_second,
        }
        for key, value in self.all_configs.iteritems():
            setattr(self, key, value)

        # Tracking variables
        self.reset()

    def reset(self):
        self.last_order_time = 0
        self.closeout_client_order_id = None
        self.eod = False
        self.closed_out = False
        self.canceled_out = False

    def on_ask(self):
        self.calc()

    def on_bid(self):
        self.calc()

    def on_trade(self):
        self.calc()

    def on_fill(self):
        if self.last_fill.client_order_id == self.closeout_client_order_id and \
                self.last_fill.remaining_qty == 0:
            # Our limit order for closing out is fully filled
            self.closeout_client_order_id = None
        self.calc()

    def on_cancel_replace_rejected(self, client_order_id):
        if client_order_id == self.closeout_client_order_id:
            self.closeout_client_order_id = None

    def on_new_order_rejected(self, client_order_id):
        if client_order_id == self.closeout_client_order_id:
            self.closeout_client_order_id = None

    def calc(self):

        return #TODO

        if self.check_eod():
            # Don't trade as we get close to the end of the day
            return

        if self.last_time < self.book.open_time:
            # Don't place orders until the market open
            return

        if self.unrealized_pnl > self.max_pnl_gain:
            # Realize gains
            self.closeout_position()
        elif self.unrealized_pnl < -1 * self.max_pnl_loss:
            # Cut losses
            self.closeout_position()

        if self.last_time < self.last_order_time + self.cooldown:
            # Don't place orders too quickly
            return

        for key, order in self.open_orders.iteritems():
            if self.last_time - order.placed_time > self.max_open_order_time:
                # Cancel open orders that are too old
                self.cancel(order.client_order_id)

        if self.book.midpoint_price < self.last_trade.price - self.min_cents and \
                self.last_time > self.last_trade.timestamp + self.min_time and \
                abs(self.shares_held) < self.max_position:
            # Sell signal
            self.log_signal("sell")
            if self.shares_held < 100:
                self.new_order(self.book.top_bid_price, 100, 'short')
            else:
                self.new_order(self.book.top_bid_price, 100, 'sell')
            self.last_order_time = self.last_time
        elif self.book.midpoint_price > self.last_trade.price + self.min_cents and \
                self.last_time > self.last_trade.timestamp + self.min_time and \
                abs(self.shares_held) < self.max_position:
            # Buy signal
            self.log_signal("buy")
            self.new_order(self.book.top_ask_price, 100, 'buy')
            self.last_order_time = self.last_time

    def closeout_position(self):
        if self.closeout_client_order_id != None:
            # We already have a limit order open for closing position
            #TODO this is buggy
            order = self.search_unfilled_orders(self.closeout_client_order_id)
            if order.remaining_qty != self.shares_held or \
                    order.placed_time > self.last_time + self.closeout_freshness:
                if self.shares_held > 0:
                    cid = self.cancel_replace(order.client_order_id, self.book.top_bid_price, self.shares_held, 'sell')
                    self.closeout_client_order_id = cid
                elif self.shares_held < 0:
                    cid = self.cancel_replace(order.client_order_id, self.book.top_ask_price, abs(self.shares_held), 'cover')
                    self.closeout_client_order_id = cid
        else:
            # Create a new limit order for closing position
            if self.shares_held > 0:
                cid = self.close_client_order_id = self.new_order(self.book.top_bid_price, self.shares_held, 'sell')
                self.closeout_client_order_id = cid
            elif self.shares_held < 0:
                cid = self.new_order(self.book.top_ask_price, abs(self.shares_held), 'cover')
                self.closeout_client_order_id = cid

    def check_eod(self):
        cancel_time = self.book.close_time - (60 * self.book.millis_in_second)
        if self.last_time > cancel_time and self.canceled_out == False:
            # Stop placing orders in the last 60 seconds
            self.eod = True
            self.cancel_all()
            self.canceled_out = True

        closeout_time = self.book.close_time - (30 * self.book.millis_in_second)
        if self.shares_held > 0 and self.last_time > closeout_time and self.closed_out == False:
            # Close out open positions in the last 30 seconds
            if self.shares_held > 0:
                self.new_market_order(self.shares_held, 'sell')
            elif self.shares_held < 0:
                self.new_market_order(abs(self.shares_held), 'buy')
            self.closed_out = True

        return self.eod
