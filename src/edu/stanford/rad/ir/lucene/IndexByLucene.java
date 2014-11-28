package edu.stanford.rad.ir.lucene;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

public class IndexByLucene {

	public static final String FILES_TO_INDEX_DIRECTORY = "files";
	public static final String INDEX_DIRECTORY = "index/all";
	public static final Version luceneVersion = Version.LUCENE_46;
	public static final int HITS_PER_PAGE = 10;


	public static void main(String[] args) throws Exception {

		createIndex(); // Only once - if repeat delete the old one 
		
		
//		searchIndex("ct scan");
//		searchIndex("steak");
//		searchIndex("steak AND cheese");
//		searchIndex("steak and cheese");
//		searchIndex("bacon OR cheese");
		findSilimar("CT scan of the heart was done to evaluate for coronary artery calcification. The examination is positive and shows definite calcium deposits which are diagnostic of atherosclerotic disease. The calcium score is 5. This score places this patient in the 42 percentile for quantity of calcification for her age and gender group when compared to our age and gender matched control group. There is calcification at the proximal left anterior descending coronary artery. Remainder is negative.");
		
	}

	public static void createIndex() throws CorruptIndexException,
			LockObtainFailedException, IOException {
		Analyzer analyzer = new StandardAnalyzer(luceneVersion);
		Directory directory = SimpleFSDirectory.open(new File(INDEX_DIRECTORY));
		IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, config);

		List<File> files= new ArrayList<File>();
		listFiles(FILES_TO_INDEX_DIRECTORY, files);
		
		int counter = 0;
		for (File file : files) {
			Scanner scanner = new Scanner(file, "UTF-8");
			String contents = scanner.useDelimiter("\\Z").next();
			scanner.close();
			Document document = new Document();
			document.add(new Field("documentId", file.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
			document.add(new Field( "contents", contents ,Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
			document.add(new StringField("path", file.getPath(), Field.Store.YES));
			document.add(new StringField("filename", file.getName(), Field.Store.YES));
			indexWriter.addDocument(document);
			System.out.println("Indexed " + file.getPath());
			counter++;
		}
		indexWriter.close();
	}

	public static void searchIndex(String searchString) throws IOException,
			ParseException {

		System.out.println("Searching for \"" + searchString + "\"");
		Analyzer analyzer = new StandardAnalyzer(luceneVersion);
		Directory directory = SimpleFSDirectory.open(new File(INDEX_DIRECTORY));
		DirectoryReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		Query q = new QueryParser(luceneVersion, "contents", analyzer).parse(searchString);
		TopScoreDocCollector collector = TopScoreDocCollector.create(HITS_PER_PAGE, true);
		indexSearcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    printResults(hits, indexSearcher);

		indexReader.close();
	}
	
	private static void findSilimar(String searchForSimilar) throws IOException {
		System.out.println("Searching for similar documents for \"" + searchForSimilar + "\"");
		Analyzer analyzer = new StandardAnalyzer(luceneVersion);
		Directory directory = SimpleFSDirectory.open(new File(INDEX_DIRECTORY));
		DirectoryReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		MoreLikeThis moreLikeThis = new MoreLikeThis(indexReader);
		
		moreLikeThis.setAnalyzer(analyzer);
		moreLikeThis.setFieldNames(new String[]{"contents", "path", "filename"});
	    moreLikeThis.setMinTermFreq(0);
	    moreLikeThis.setMinDocFreq(0);
	    
	    Reader sReader = new StringReader(searchForSimilar);
	    Query query = moreLikeThis.like(sReader, null);
	    //Query query = moreLikeThis.like(57);
	    ScoreDoc[] hits = indexSearcher.search(query,HITS_PER_PAGE).scoreDocs;
	    printResults(hits, indexSearcher);
	    
		indexReader.close();
	}
	
	public static void printResults(ScoreDoc[] hits, IndexSearcher indexSearcher)
			throws IOException {
		System.out.println("Found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = indexSearcher.doc(docId);
			System.out.println((i + 1) + ". " + docId + "\t" + d.get("filename"));
		}
	}
	
	public static void listFiles(String directoryName, List<File> files) {
		File directory = new File(directoryName);

		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile() && !file.getName().startsWith(".")) {
				files.add(file);
			} else if (file.isDirectory()) {
				listFiles(file.getAbsolutePath(), files);
			}
		}
	}

}