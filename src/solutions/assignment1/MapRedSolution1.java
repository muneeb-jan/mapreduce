package solutions.assignment1;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.*;

import java.io.FileInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import examples.MapRedFileUtils;

public class MapRedSolution1
{
    /* your code goes in here*/
    public static class MapRecords extends Mapper<LongWritable, Text, Text, LongWritable>
    {
        private final static LongWritable one = new LongWritable(1);
        private Text wordy = new Text();
        
        @Override
        protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException
        {
            final String regex = "^(\\S+) (\\S+) (\\S+) " +  
            "\\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(\\S+)" +  
            " (\\S+)\\s*(\\S+)?\\s*\" (\\d{3}) (\\S+)"; 
            final Pattern pattern = Pattern.compile(regex); 
            final Matcher matcher = pattern.matcher(value.toString()); 
            while (matcher.find())
            {
                String str = "http://localhost" + matcher.group(6);
                wordy.set(str);
                context.write(wordy, one);
            }
        }
    }

    public static class ReduceRecords extends Reducer<Text, LongWritable, Text, LongWritable>
    {
        private LongWritable result = new LongWritable();

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values,
            Context context) throws IOException, InterruptedException
        {
            int sum = 0;
        
            for (LongWritable val : values)
            sum += val.get();
            
            result.set(sum);
            context.write(key, result);
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        Configuration conf = new Configuration();

        String[] otherArgs =
            new GenericOptionsParser(conf, args).getRemainingArgs();
        
        if (otherArgs.length != 2)
        {
            System.err.println("Usage: MapRedSolution1 <in> <out>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "MapRed Solution #1");
        
        /* your code goes in here*/

        job.setMapperClass(MapRecords.class);
        job.setCombinerClass(ReduceRecords.class);
        job.setReducerClass(ReduceRecords.class);
       
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        MapRedFileUtils.deleteDir(otherArgs[1]);
        int exitCode = job.waitForCompletion(true) ? 0 : 1; 
        
        FileInputStream fileInputStream = new FileInputStream(new File(otherArgs[1]+"/part-r-00000"));
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileInputStream);
        fileInputStream.close();
        
        String[] validMd5Sums = {"af174f7148177b6ad68b7092bc9789f9","878ed82c762143aaf939797f6b02e781"};
        
        for (String validMd5 : validMd5Sums) 
        {
            if (validMd5.contentEquals(md5))
            {
                System.out.println("The result looks good :-)");
                System.exit(exitCode);
            }
        }
        System.out.println("The result does not look like what we expected :-(");
        System.exit(exitCode);
    }
}
