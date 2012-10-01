package nl.waredingen.graphs.neo.mapreduce.group;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class GroupNodesAndEdgesReducer extends Reducer<Text, Text, NullWritable, Text> {

	private Text outputValue = new Text();
	protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException ,InterruptedException {
		String keyString = key.toString();
		String outputKey = keyString.substring(0, keyString.lastIndexOf(";"));
		for (Text value : values) {
			outputValue.set(outputKey+"\t"+ value);
			context.write(NullWritable.get(), outputValue);
		}
	}
}
