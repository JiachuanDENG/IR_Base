package mains;

import java.io.IOException;
import java.text.ParseException;

import Analyzer.cfLaraAnalyzer;
import structures._Corpus;
import structures._CorpusCfLara;
import structures._Doc;
import structures._DoccfLara;
import structures._UserCfLara;

public class CfAnalyzerTest {
  public static void main(String[] args) throws IOException, ParseException {	
	String tokenModel = "./data/Model/en-token.bin"; //Token model.
	int classNumber = 5;
	String featureValue = "TF";
	String articleType = "Tech";
	String fvStatFile="./data/Features/fv_1gram_stat_Tripadvisor_featureGeneration.txt";
	int norm=0;
	String commentFolder = String.format(
			"./data/ParentChildTopicModel/%sComments",
					articleType);
	commentFolder="./data/General4Analyzer";
//	 commentFolder =String.format("/Users/dengjiachuan/Desktop/yelpsplit_part/yelpsplit8");
//	 commentFolder="/if15/jd5rh/ctr_U/JSON_CTR";
	String suffix = ".json";
	String fvFile = String.format("./data/Features/fv_1gram_topicmodel_TripAdvisor.txt");
	int Ngram = 1;
	int lengthThreshold = 0;
	cfLaraAnalyzer analyzer=new cfLaraAnalyzer(tokenModel,classNumber, fvFile, Ngram, lengthThreshold) ;
	analyzer.LoadDirectory(commentFolder, suffix);
	analyzer.setFeatureValues(featureValue, norm);
	_CorpusCfLara c =  analyzer.returnCorpus(fvStatFile); // Get the collection of all the documents.	
	System.out.println("corpus already");
	System.out.println(c.getCollection().size());
//	for(_Doc d:c .getCollection()){
//		System.out.println("---");
//		System.out.println((((_DoccfLara)d).m_user).m_userName);
//	}         
//	System.out.println(analyzer.getCorpus().getSize());
	
	}
}
