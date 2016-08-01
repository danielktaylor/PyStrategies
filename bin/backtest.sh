#!/bin/bash
set -e

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 quotes.csv strategy.py" >&2
  exit 1
fi

cd ./strategy
java -cp "../backtest/TradingFramework4j/lib/*:../backtest/TradingFramework4j/bin/" backtester.Backtester ../$1 ../$2
