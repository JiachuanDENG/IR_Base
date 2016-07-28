package topicmodels.correspondenceModels;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import structures._ChildDoc;
import structures._Corpus;
import structures._Doc;
import structures._ParentDoc;
import structures._SparseFeature;
import structures._Stn;
import structures._Word;
import topicmodels.LDA.LDA_Gibbs;
import utils.Utils;

public class corrLDA_Gibbs extends LDA_Gibbs {
	boolean m_statisticsNormalized = false;//a warning sign of normalizing statistics before collecting new ones
	double[] m_topicProbCache;
	
	public corrLDA_Gibbs(int number_of_iteration, double converge, double beta, _Corpus c, double lambda,
			int number_of_topics, double alpha, double burnIn, int lag){
		super(number_of_iteration, converge, beta, c, lambda, number_of_topics, alpha, burnIn, lag);
	
		m_topicProbCache = new double[number_of_topics];
	}
	
	@Override
	protected void initialize_probability(Collection<_Doc> collection){
		createSpace();
		for(int i=0; i<number_of_topics; i++)
			Arrays.fill(word_topic_sstat[i], d_beta);
		Arrays.fill(m_sstat, d_beta*vocabulary_size);
		
		for(_Doc d: collection){
			if(d instanceof _ParentDoc){
				for(_Stn stnObj: d.getSentences()){
					stnObj.setTopicsVct(number_of_topics);	
				}
				d.setTopics4Gibbs(number_of_topics, 0);

			}else if(d instanceof _ChildDoc){
				((_ChildDoc) d).setTopics4Gibbs_LDA(number_of_topics, 0);
			}
			
			for(_Word w:d.getWords()){
				word_topic_sstat[w.getTopic()][w.getIndex()] ++;
				m_sstat[w.getTopic()] ++;
			}
		}
		
		imposePrior();
		
		m_statisticsNormalized = false;
	}
	
	public String toString(){
		return String.format("correspondence LDA [k:%d, alpha:%.2f, beta:%.2f, Gibbs Sampling]",
				number_of_topics, d_alpha, d_beta);
	}
	
	public double calculate_E_step(_Doc d){
		d.permutation();
		
		if(d instanceof _ParentDoc)
			sampleInParentDoc((_ParentDoc)d);
		else if(d instanceof _ChildDoc)
			sampleInChildDoc((_ChildDoc)d);
		
		return 0;
	}
	
	public void sampleInParentDoc(_ParentDoc d){
		int wid, tid;
		double normalizedProb;
		
		for(_Word w:d.getWords()){
			wid = w.getIndex();
			tid = w.getTopic();
			
			d.m_sstat[tid] --;
			if(m_collectCorpusStats){
				word_topic_sstat[tid][wid] --;
				m_sstat[tid] --;
			}
			
			normalizedProb = 0;
			for(tid=0; tid<number_of_topics; tid++){
				double pWordTopic = parentWordByTopicProb(tid, wid);
				double pTopicPDoc = parentTopicInDocProb(tid, d);
				double pTopicCDoc = parentChildInfluenceProb(tid, d);
				
				m_topicProbCache[tid] = pWordTopic*pTopicPDoc*pTopicCDoc;
				normalizedProb += m_topicProbCache[tid];
			}
			
			normalizedProb *= m_rand.nextDouble();
			for(tid=0; tid<number_of_topics; tid++){
				normalizedProb -= m_topicProbCache[tid];
				if(normalizedProb<0)
					break;
			}
			
			if(tid == number_of_topics)
				tid --;
			
			w.setTopic(tid);
			d.m_sstat[tid] ++;
			if(m_collectCorpusStats){
				word_topic_sstat[tid][wid] ++;
				m_sstat[tid] ++;
			}
			
		}
	}
	
	protected double parentWordByTopicProb(int tid, int wid){
		return word_topic_sstat[tid][wid]/m_sstat[tid];
	}
	
	protected double parentTopicInDocProb(int tid, _ParentDoc d){
		return (d_alpha+d.m_sstat[tid]);
	}
	
	protected double parentChildInfluenceProb(int tid, _ParentDoc d){
		double term = 1;
		
		if(tid==0)
			return term;
		
		for (_ChildDoc cDoc : d.m_childDocs4Dynamic) {
			term *= influenceRatio(cDoc.m_sstat[tid], d.m_sstat[tid],
					cDoc.m_sstat[0], d.m_sstat[0]);
		}
		
		return term;
	}
	
	protected double influenceRatio(double njc, double njp, double n1c,
			double n1p) {
		double ratio = 1.0;
		double smoothingParameter = 1e-20;
		
		for(int n=1; n<=n1c; n++){
			ratio *= (n1p + smoothingParameter) * 1.0
					/ (n1p + 1 + smoothingParameter);
		}
		
		for(int n=1; n<=njc; n++){
			ratio *= (njp + 1 + smoothingParameter) * 1.0
					/ (njp + smoothingParameter);
		}
		
		return ratio;
	}
	
	protected void sampleInChildDoc(_ChildDoc d){
		int wid, tid;
		double normalizedProb = 0;
		
		for(_Word w: d.getWords()){
			wid = w.getIndex();
			tid = w.getTopic();
			
			d.m_sstat[tid]--;
			if(m_collectCorpusStats){
				word_topic_sstat[tid][wid] --;
				m_sstat[tid] --;
			}
			
			normalizedProb = 0;
			for(tid=0; tid<number_of_topics; tid++){
				double pWordTopic = childWordByTopicProb(tid, wid);
				double pTopicDoc = childTopicInDocProb(tid, d);
				
				m_topicProbCache[tid] = pWordTopic*pTopicDoc;
				normalizedProb += m_topicProbCache[tid];
			}
			
			normalizedProb *= m_rand.nextDouble();
			for(tid=0; tid<number_of_topics; tid++){
				normalizedProb -= m_topicProbCache[tid];
				if(normalizedProb<0)
					break;
			}
			
			if(tid == number_of_topics)
				tid --;
			
			w.setTopic(tid);
			d.m_sstat[tid] ++;
			if(m_collectCorpusStats){
				word_topic_sstat[tid][wid] ++;
				m_sstat[tid] ++;
			}
		}
	}
	
	protected double childWordByTopicProb(int tid, int wid){
		return word_topic_sstat[tid][wid]/m_sstat[tid];
	}
	
	protected double childTopicInDocProb(int tid, _ChildDoc d){
		double smoothingParameter = 1e-20;
		_ParentDoc pDoc = (_ParentDoc)(d.m_parentDoc);
		double term = (pDoc.m_sstat[tid]+smoothingParameter)/(pDoc.getDocInferLength()+smoothingParameter*number_of_topics);

		return term;
	}
	
	public double inference(_Doc pDoc){
		ArrayList<_Doc> sampleTestSet = new ArrayList<_Doc>();
		
		initTest(sampleTestSet, pDoc);
	
		double logLikelihood = 0.0, count = 0;
		int  iter = 0;
		do {
			int t;
			_Doc tmpDoc;
			for(int i=sampleTestSet.size()-1; i>1; i--) {
				t = m_rand.nextInt(i);
				
				tmpDoc = sampleTestSet.get(i);
				sampleTestSet.set(i, sampleTestSet.get(t));
				sampleTestSet.set(t, tmpDoc);			
			}
			
			for(_Doc doc: sampleTestSet)
				calculate_E_step(doc);
			
			if (iter>m_burnIn && iter%m_lag==0){
				for(_Doc doc: sampleTestSet){
					collectStats(doc);
				}
				count ++;
			}
		} while (++iter<this.number_of_iteration);
	
		for(_Doc doc: sampleTestSet){
			estThetaInDoc(doc);
			logLikelihood += calculate_test_log_likelihood(doc);
		}
		
		return logLikelihood;
	}
	
	public void calculate_M_step(int iter){
		if(iter>m_burnIn && iter%m_lag==0){
			if(m_statisticsNormalized){
				System.err.println("The statistics collector has been normlaized before, cannot further accumulate the samples!");
				System.exit(-1);
			}
			
			for(int i=0; i<number_of_topics; i++){
				for(int v=0; v<vocabulary_size; v++){
					topic_term_probabilty[i][v] += word_topic_sstat[i][v];
				}
			}
			
			for(_Doc d:m_trainSet){
				if(d instanceof _ParentDoc)
					collectParentStats((_ParentDoc)d);
				else if(d instanceof _ChildDoc)
					collectChildStats((_ChildDoc)d);
					
			}
		}
	}
	
	public void collectParentStats(_ParentDoc d){
		for(int k=0; k<number_of_topics; k++)
			d.m_topics[k] += d.m_sstat[k] + d_alpha;
		d.collectTopicWordStat();		
	}
	
	public void collectChildStats(_ChildDoc d){
		for(int k=0; k<number_of_topics; k++)
			d.m_topics[k] += d.m_sstat[k];
	}

	protected void initTest(ArrayList<_Doc> sampleTestSet, _Doc d){
		_ParentDoc pDoc = (_ParentDoc)d;
		for(_Stn stnObj: pDoc.getSentences()){
			stnObj.setTopicsVct(number_of_topics);
		}
		
		int testLength = (int)(m_testWord4PerplexityProportion*pDoc.getTotalDocLength());
//		testLength = 0;
		pDoc.setTopics4GibbsTest(number_of_topics, 0, testLength);		
		sampleTestSet.add(pDoc);
		
		for(_ChildDoc cDoc: pDoc.m_childDocs){
			
			testLength = (int)(m_testWord4PerplexityProportion*cDoc.getTotalDocLength());
			cDoc.setTopics4GibbsTest(number_of_topics, 0, testLength);
			sampleTestSet.add(cDoc);
		}
	}

	public double logLikelihoodByIntegrateTopics(_ParentDoc d){
		double docLogLikelihood = 0;
		
		_SparseFeature[] fv = d.getSparse();
		
		for(int j=0; j<fv.length; j++){
			int wid = fv[j].getIndex();
			double value = fv[j].getValue();
			
			double wordLogLikelihood = 0;
			for(int k=0; k<number_of_topics; k++){
				double wordPerTopicLikelihood = parentWordByTopicProb(k, wid)*parentTopicInDocProb(k, d)/(d_alpha*number_of_topics+d.getTotalDocLength());
				wordLogLikelihood += wordPerTopicLikelihood;
			}
			
			if(Math.abs(wordLogLikelihood)<1e-10){
				System.out.println("wordLogLikelihood\t"+wordLogLikelihood);
				wordLogLikelihood += 1e-10;
			}
			
			wordLogLikelihood = Math.log(wordLogLikelihood);
			
			docLogLikelihood += value*wordLogLikelihood;
		}
		
		return docLogLikelihood;
	}
	
	public double logLikelihoodByIntegrateTopics(_ChildDoc d){
		double docLogLikelihood = 0;
		
		_SparseFeature[] fv = d.getSparse();
		for(int i=0; i<fv.length; i++){
			int wid = fv[i].getIndex();
			double value = fv[i].getValue();
			double wordLogLikelihood = 0;
			for(int k=0; k<number_of_topics; k++){
				double wordPerTopicLikelihood = childWordByTopicProb(k, wid)
						* childTopicInDoc(k,d);
				wordLogLikelihood += wordPerTopicLikelihood;
			}
			
			if(wordLogLikelihood< 1e-10){
				wordLogLikelihood += 1e-10;
				System.out.println("small likelihood in child");
			}
			
			wordLogLikelihood = Math.log(wordLogLikelihood);
			
			docLogLikelihood += value*wordLogLikelihood;
		}
		
		return docLogLikelihood;
	}
	
	protected double testLogLikelihoodByIntegrateTopics(_ChildDoc d){
//		_ChildDoc4BaseWithPhi cDoc = (_ChildDoc4BaseWithPhi)d;
		double docLogLikelihood = 0.0;

		// prepare compute the normalizers
		_SparseFeature[] fv = d.getSparse();

		for (_Word w : d.getTestWords()) {
			int wid = w.getIndex();

			double wordLogLikelihood = 0;
			for (int k = 0; k < number_of_topics; k++) {
				double term1 = childWordByTopicProb(k, wid);
				double term2 = childTopicInDoc(k, d);
				
				double wordPerTopicLikelihood = term1*term2;
				wordLogLikelihood += wordPerTopicLikelihood;
			}
			
			if (Math.abs(wordLogLikelihood) < 1e-10) {
				System.out.println("wordLoglikelihood\t" + wordLogLikelihood);
				wordLogLikelihood += 1e-10;
			}

			wordLogLikelihood = Math.log(wordLogLikelihood);
			docLogLikelihood += wordLogLikelihood;
		}

		return docLogLikelihood;
	}
	
	public double childTopicInDoc(int tid, _ChildDoc cDoc){
		return cDoc.m_sstat[tid]/cDoc.getDocInferLength();
	}
	
	public void initTest4Dynamical(ArrayList<_Doc> sampleTestSet, _Doc d, int commentNum){
		_ParentDoc pDoc = (_ParentDoc)d;
		pDoc.m_childDocs4Dynamic = new ArrayList<_ChildDoc>();
		pDoc.setTopics4Gibbs(number_of_topics, 0);
		for(_Stn stnObj: pDoc.getSentences()){
			stnObj.setTopicsVct(number_of_topics);
		}

		sampleTestSet.add(pDoc);
		int count = 0;
		for(_ChildDoc cDoc:pDoc.m_childDocs){
			if(count>=commentNum){
				break;
			}
			count ++;
			cDoc.setTopics4Gibbs_LDA(number_of_topics, 0);
			sampleTestSet.add(cDoc);
			pDoc.addChildDoc4Dynamics(cDoc);
		}
	}
}

