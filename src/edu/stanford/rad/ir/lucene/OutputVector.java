package edu.stanford.rad.ir.lucene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.VectorWritable;


public class OutputVector {

	public static void main(String[] args) throws Exception {
		Map <Integer, String> dictionaryMap = new HashMap<Integer, String>();
		String dicFile = "/Users/saeedhp/Dropbox/Stanford/Mining/vectors/dictionary.txt";
		BufferedReader bReader = new BufferedReader(new FileReader(dicFile));
		String line;
		while ((line = bReader.readLine()) != null) {
			String[] dictokens = line.split("\\s+");
			if(dictokens.length == 3)
			{
				dictionaryMap.put(Integer.parseInt(dictokens[2]), dictokens[0]);
				//System.out.println(dictokens[2] + " " + dictokens[0]);
			}
		}
		bReader.close();
		
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		String vectorsPath = "/Users/saeedhp/Dropbox/Stanford/Mining/vectors/vectors.txt";
		Path path = new Path(vectorsPath);
		 
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, path, conf);
		LongWritable key = new LongWritable();
		VectorWritable value = new VectorWritable();
		
		while (reader.next(key, value)) {
			NamedVector namedVector = (NamedVector)value.get();
			System.out.println(namedVector.getName() + ":");
			RandomAccessSparseVector vect = (RandomAccessSparseVector)namedVector.getDelegate();
			
			for(int i=0; i<vect.size(); ++i)
			{
				Element  e = vect.getElement(i);
				int index = e.index();
				double weight = e.get();
				if (weight != 0.0)
				{
					System.out.println("Index:"+ index + " Token:" + dictionaryMap.get(index) + " TF-IDF weight:" + weight) ;
				}
			}
			System.out.println("===============================");
		}
		reader.close();
	}

}
