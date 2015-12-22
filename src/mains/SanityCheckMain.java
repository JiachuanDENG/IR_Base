package mains;

import java.io.FileNotFoundException;
import java.io.IOException;

import SanityCheck.FurtherPuritySanityCheck;
import SanityCheck.PartSanityCheck;
import SanityCheck.PuritySanityCheck;
import opennlp.tools.util.InvalidFormatException;
import structures._Corpus;
import Analyzer.Analyzer;
import Analyzer.DocAnalyzer;
import Analyzer.jsonAnalyzer;

public class SanityCheckMain {
	//In this main function, I want to check the purity given by Random, BoW, Topic vectors, BoW+Topic 
	//to see in which situation the similarity works and why? 
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException{
		int classNumber = 5;
		int Ngram = 2; //The default value is unigram. 
		int lengthThreshold = 5; //Document length threshold		
		int number_of_topics = 30;
		
		/*****The parameters used in loading files.*****/
		String folder = "./data/amazon/small/dedup/RawData";
//		String folder = "./data/amazon/small/dedup/debug";
		String suffix = ".json";
		String tokenModel = "./data/Model/en-token.bin"; //Token model.

		String category = "tablets"; //"electronics"
		String dataSize = "86jsons"; //"50K", "100K"
		String fvFile = String.format("./data/Features/fv_%dgram_%s_%s.txt", Ngram, category, dataSize);
		String fvStatFile = String.format("./data/Features/fv_%dgram_stat_%s_%s.txt", Ngram, category, dataSize);
		String stopwords = "./data/Model/stopwords.dat";
		
		System.out.println("Creating feature vectors, wait...");
		Analyzer analyzer;
		_Corpus c;
		analyzer = new jsonAnalyzer(tokenModel, classNumber, fvFile, Ngram, lengthThreshold);
		((DocAnalyzer) analyzer).setReleaseContent(false);
		((DocAnalyzer) analyzer).LoadStopwords(stopwords);
		analyzer.LoadDirectory(folder, suffix); //Load all the documents as the data set.
		
//		int numOfAspects = 28; // 12, 14, 24, 28
//		String topicFile = String.format("./data/TopicVectors/%dAspects_topicVectors_corpus.txt", numOfAspects);
//		analyzer.loadTopicVectors(topicFile, number_of_topics);
		
		//construct effective feature values for supervised classifiers 
		analyzer.setFeatureValues("BM25", 2);
		c = analyzer.returnCorpus(fvStatFile); // Get the collection of all the documents.
		c.mapLabels(4);
		
		int topK = 20;
		String anchorNodeFile = "./data/SanityCheck/AnchorNode.txt";
		PuritySanityCheck check = new PuritySanityCheck(c, "BoW");
		check.calculateSimilarity();
		check.calculateInlinks(topK);
		check.calculatePatK4All(topK);
		check.writePatK(anchorNodeFile);

//		check.loadAnnotatedFile("./data/Selected100Files/100Files_IDs_Annotation.txt");
//		int[] groupSize = check.getGroupSize();
//		check.setFeature(analyzer.getFeatures());
//
//		//BoW and topic performance check.
//		String tGroup = String.format("data/SanityCheck/DiffGroupTP_%dAspects_", numOfAspects);
//		double[] performance = check.constructPurity(topK, 1, tGroup);//0: Bow; else: topic; return purity.
//		
////		double[] performance = check.trainSVM(); // Return precision in this case.
//		
//		for(int i= 0; i<performance.length; i++)
//			System.out.format("%d\t", i);
//		System.out.println();
//		
//		for(int i= 0; i<groupSize.length; i++)
//			System.out.format("%d\t", groupSize[i]);
//		System.out.println();
//		
//		for(int i= 0; i<performance.length; i++)
//			System.out.format("%.4f\t", performance[i]);
//		System.out.println();
		
	}
}
