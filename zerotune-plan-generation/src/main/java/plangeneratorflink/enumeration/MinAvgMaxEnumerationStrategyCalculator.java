package plangeneratorflink.enumeration;

import org.apache.flink.api.java.functions.NullByteKeySelector;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.graph.StreamNode;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamSink;
import org.apache.flink.streaming.api.operators.StreamSource;
import org.apache.flink.streaming.runtime.operators.windowing.WindowOperator;

import org.graphstream.graph.Graph;
import plangeneratorflink.utils.Constants;

/** Applies the random parallelism strategy to operators. * */
public class MinAvgMaxEnumerationStrategyCalculator extends EnumerationStrategyCalculator {

    private Type currentType = Type.MAX; // default to max to start with min

    public MinAvgMaxEnumerationStrategyCalculator(Configuration config) {
        super(config);
    }

    @Override
    public void initNewQuery(StreamGraph streamGraph, Graph pgfGraph, String runName) {
        switch (this.currentType) {
            case AVG:
                this.currentType = Type.MAX;
                break;
            case MAX:
                this.currentType = Type.MIN;
                break;
            case MIN:
                this.currentType = Type.AVG;
                break;
            default:
                throw new RuntimeException(
                        "unknown MinAvgMax type in MinAvgMax enumeration strategy.");
        }
    }

    @Override
    public <T> Integer getParallelismOfOperator(
            StreamGraph streamGraph, Graph pgfGraph, StreamNode node, String runName) {
        StreamOperator<?> op = node.getOperator();
        // set parallelism to 1 for Sources, Sinks and All-Window operators, for all other
        // cases set parallelism based on parallelism strategy
        int parallelism = 1;
        if (op instanceof StreamSource
                || op instanceof StreamSink
                || (op instanceof WindowOperator
                        && ((WindowOperator<?, ?, ?, ?, ?>) op).getKeySelector()
                                instanceof NullByteKeySelector)) {
            parallelism = 1;
            if (op instanceof StreamSource) {
                // define custom parameter or set default value out of constants
                // Integer confEventRate = pgfGraphNode.getAttribute("confEventRate",
                // Integer.class);
                parallelism =
                        config.getInteger(
                                ConfigOptions.key("sourceParallelism")
                                        .intType()
                                        .defaultValue(Constants.Synthetic.SOURCE_PARALLELISM));
            }
            return parallelism;
        }
        int avg = (int) Math.round((maxParallelism - minParallelism) / 2.0);
        switch (this.currentType) {
            case AVG:
                return avg;
            case MAX:
                return maxParallelism;
            case MIN:
                return minParallelism;
            default:
                throw new RuntimeException(
                        "unknown MinAvgMax type in MinAvgMax enumeration strategy.");
        }
    }

    @Override
    public boolean hasNext(String runName) {
        return true;
    }

    private enum Type {
        MIN,
        AVG,
        MAX
    }
}
