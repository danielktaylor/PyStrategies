package backtester.strategy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import backtester.trade.DoNothingTradeDestination;
import backtester.trade.TradeDestination;

public class StrategyConfiguration {
    private final TradeDestination tradeDestination;
    private final Date tradingDate;
    private final Map<String, String> miscConfig;

    /**
     * Create new strategy configuration using an existing configuration .
     *
     * @param otherStrategyConfiguration
     * @return new strategy configuration
     */
    public static StrategyConfiguration newStrategyConfiguration(final StrategyConfiguration otherStrategyConfiguration) {
        return new StrategyConfiguration(otherStrategyConfiguration.getTradeDestination(),
                otherStrategyConfiguration.getTradingDate(), otherStrategyConfiguration.getAllVariables());
    }

    public static StrategyConfiguration emptyStrategyConfiguration() {
        return new StrategyConfiguration(new DoNothingTradeDestination(), new Date(0));
    }
    
    public StrategyConfiguration(final TradeDestination tradeDestination,
            final Date tradingDate, final Map<String, String> miscConfig) {
        this.tradeDestination = tradeDestination;
        this.tradingDate = tradingDate;
        this.miscConfig = miscConfig;
    }
    
    public StrategyConfiguration(final TradeDestination tradeDestination,
            final Date tradingDate) {
        this.tradeDestination = tradeDestination;
        this.tradingDate = tradingDate;
        this.miscConfig = new HashMap<String, String>();
    }

    public TradeDestination getTradeDestination() {
        return tradeDestination;
    }

    public Date getTradingDate() {
        return tradingDate;
    }
    
    public void setVariable(String key, String value) {
    	miscConfig.put(key, value);
    }
    
    public String getVariable(String key) {
    	return miscConfig.get(key);
    }
    
    public Map<String, String> getAllVariables() {
    	return miscConfig;
    }
}
