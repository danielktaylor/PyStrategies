import os, sys, re
import optunity
from fabric.api import env, local, lcd

base_path = os.path.dirname(os.path.realpath(__file__))
quote_file = ""
strategy_file = ""
NUM_EVALS = 0

def convert_dollars(dollars):
    score = dollars.replace("$","")
    if score[0] == "(":
        score = float(score.replace("(","").replace(")","")) * -1.0
    else:
        score = float(score)
    return score

def run_test(print_output, my_configs):
    lcd(base_path)
    with lcd('./strategy'):
        java_cmd = 'java -cp "../backtest/TradingFramework4j/lib/*:../backtest/TradingFramework4j/bin/" backtester.Backtester '
        cmd_args = "../quotes/%s %s \"%s\"" % (quote_file, strategy_file, my_configs)
        output = local(java_cmd + cmd_args, capture=True)

        if print_output:
            print output

        match = re.search(r'.*Closed PnL \(no fees\):\s*(.*)$', output, re.MULTILINE)
        if match:
            pnl = float(convert_dollars(match.group(1)))
        else:
            print "Error getting PnL."
            sys.exit(1)

        match = re.search(r'.*Total Cost:\s*(.*)$', output, re.MULTILINE)
        if match:
            cost = float(convert_dollars(match.group(1)))
        else:
            print "Error getting cost."
            sys.exit(1)

        score = pnl - cost
        return score

def test_run(**kwargs):
    my_configs = ""
    for key, value in kwargs.items():
        my_configs += "%s=%s;" % (key, value)
    return run_test(True, my_configs)

def evaluate(**kwargs):
    my_configs = ""
    for key, value in kwargs.items():
        my_configs += "%s=%s;" % (key, value)
    return run_test(False, my_configs)

if __name__ == '__main__':
    if len(sys.argv) != 4:
        print "usage: %s quotes.csv strategy.py num_iterations" % sys.argv[0]
        sys.exit(0)

    quote_file = os.path.basename(sys.argv[1])
    strategy_file = os.path.basename(sys.argv[2])
    NUM_EVALS = int(sys.argv[3])
    print "Starting %i evaluations..." % NUM_EVALS

    my_configs = {}
    with open(os.path.join(base_path,"parameters.csv"), "r") as infile:
        all_lines = infile.readlines()
        for line in all_lines[1:]:
            s = line.split(",")
            name = s[0]
            minimum = long(s[1])
            maximum = long(s[2].split("#")[0])
            my_configs[name] = [minimum, maximum]

    hps, d1, d2 = optunity.maximize(evaluate, num_evals=NUM_EVALS, pmap=optunity.pmap, **my_configs)

    # Print final report
    print "\n\n\n\n"
    print "*** Optimal Score:", str(d1.optimum)
    print "*** Best Parameters Found:", hps
    print "\n\n\n\n"
    test_run(**hps)
