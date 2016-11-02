package Analyzer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import json.JSONObject;
import opennlp.tools.util.InvalidFormatException;
import structures.TokenizeResult;
import structures._Corpus;
import structures._CorpusCfLara;
import structures._Doc;
import structures._DoccfLara;
import structures._ItemCfLara;
import structures._UserCfLara;
import utils.Utils;

public class cfLaraAnalyzer extends ParentChildAnalyzer{
	protected _CorpusCfLara m_corpus;

	public cfLaraAnalyzer(String tokenModel, int classNo, String providedCV, int Ngram, int threshold)
			throws InvalidFormatException, FileNotFoundException, IOException {
		super(tokenModel, classNo, providedCV, Ngram, threshold);
		// TODO Auto-generated constructor stub
		this.m_corpus=new _CorpusCfLara();
	}
	public _CorpusCfLara returnCorpus(String finalLocation) throws FileNotFoundException {
		SaveCVStat(finalLocation);
		
		int sum = 0;
		for(int c:m_classMemberNo) {
			System.out.print(c + " ");
			sum += c;
		}
		System.out.println(", Total: " + sum);
		
		return getCorpus();
	}
	
	public _CorpusCfLara getCorpus() {
		//store the feature names into corpus
		this.m_corpus.setFeatures(m_featureNames);
		this.m_corpus.setFeatureStat(m_featureStat);
		this.m_corpus.setMasks(); // After collecting all the documents, shuffle all the documents' labels.
		this.m_corpus.setContent(!m_releaseContent);
		return this.m_corpus;
	}
	protected boolean AnalyzeDoc(_DoccfLara doc) {
		TokenizeResult result = TokenizerNormalizeStemmer(doc.getSource());// Three-step analysis.
		String[] tokens = result.getTokens();
		int y = doc.getYLabel();
		
		// Construct the sparse vector.
		HashMap<Integer, Double> spVct = constructSpVct(tokens, y, null);
		if (spVct.size()>m_lengthThreshold) {
			doc.createSpVct(spVct);
			doc.setStopwordProportion(result.getStopwordProportion());
//			System.out.println(m_corpus.getCollection().size());
			this.m_corpus.addDoc(doc);
			m_classMemberNo[y]++;
			if (m_releaseContent)
				doc.clearSource();
			return true;
		} else {
			/****Roll back here!!******/
			rollBack(spVct, y);
			return false;
		}
	}
public void LoadDoc(String fileName){
//		System.out.println("cfLaraLoadDoc");
		if (fileName == null || fileName.isEmpty())
			return;

		JSONObject json = LoadJSON(fileName);
		String content = Utils.getJSONValue(json, "content");
		String name = Utils.getJSONValue(json, "name");
		String parent = Utils.getJSONValue(json, "parent");
		String userName=Utils.getJSONValue(json, "user");
		String itemName=Utils.getJSONValue(json, "item");
//		System.out.println("cfLara got elements");
//		System.out.println(userName);
		_UserCfLara user=m_corpus.getUser(userName);
//		System.out.println(user.toString());
		_ItemCfLara item=m_corpus.getItem(itemName);
		
		_DoccfLara doc=new _DoccfLara(m_corpus.getSize(),user,item,content,0);
//		_Doc d = new _Doc(m_corpus.getSize(), content, 0);
		doc.setName(name);
		AnalyzeDoc(doc);	}


}
