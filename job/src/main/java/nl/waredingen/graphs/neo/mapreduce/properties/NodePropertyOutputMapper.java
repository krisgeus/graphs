package nl.waredingen.graphs.neo.mapreduce.properties;

import java.io.IOException;

import nl.waredingen.graphs.misc.RowNumberJob;
import nl.waredingen.graphs.neo.mapreduce.input.MetaData;
import nl.waredingen.graphs.neo.mapreduce.input.writables.ByteMarkerIdPropIdWritable;
import nl.waredingen.graphs.neo.mapreduce.input.writables.FullNodePropertiesWritable;
import nl.waredingen.graphs.neo.mapreduce.input.writables.NodePropertyOutputCountersAndValueWritable;
import nl.waredingen.graphs.neo.neo4j.Neo4JUtils;

import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;

public class NodePropertyOutputMapper extends
		Mapper<LongWritable, FullNodePropertiesWritable, ByteMarkerIdPropIdWritable, NodePropertyOutputCountersAndValueWritable> {

	private ByteMarkerIdPropIdWritable outputKey = new ByteMarkerIdPropIdWritable();
	private NodePropertyOutputCountersAndValueWritable outputValue = new NodePropertyOutputCountersAndValueWritable();
	private long[] blockCountCounters;
	private long[] propertyIdCounters;
	private int numReduceTasks;
	private long maxIds;
	private MetaData metaData;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		metaData = Neo4JUtils.getMetaData(context.getConfiguration());
		numReduceTasks = context.getNumReduceTasks();
		maxIds = metaData.getNumberOfNodes();

		blockCountCounters = new long[numReduceTasks];
		propertyIdCounters = new long[numReduceTasks];
		outputKey.setMarker(new ByteWritable(RowNumberJob.VALUE_MARKER));
	}

	@Override
	protected void map(LongWritable key, FullNodePropertiesWritable value, Context context) throws IOException, InterruptedException {
		outputKey.setIds(value.getNodeId(), value.getPropertyIndex());
		outputValue.setValues(value.getNodeId(), value);
		blockCountCounters[NodePropertyOutputPartitioner.partitionForValue(outputValue, numReduceTasks, maxIds)] += value.getBlockCount().get();
		propertyIdCounters[NodePropertyOutputPartitioner.partitionForValue(outputValue, numReduceTasks, maxIds)]++;
		context.write(outputKey, outputValue);
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		outputKey.setMarker(new ByteWritable(RowNumberJob.COUNTER_MARKER));
		outputKey.setIds(new LongWritable(Long.MIN_VALUE), new IntWritable(Integer.MIN_VALUE));
		for (int c = 0; c < blockCountCounters.length - 1; c++) {
			outputValue.setCounter(c + 1, blockCountCounters[c], propertyIdCounters[c]);
			context.write(outputKey, outputValue);
			blockCountCounters[c + 1] += blockCountCounters[c];
			propertyIdCounters[c + 1] += propertyIdCounters[c];
		}
	}
}
