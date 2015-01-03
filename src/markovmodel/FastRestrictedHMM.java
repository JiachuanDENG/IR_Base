package markovmodel;

import structures._Doc;
import utils.Utils;

public class FastRestrictedHMM {

	int number_of_topic;
	int length_of_seq;
	double alpha[][];
	double beta[][];
	double norm_factor[];
	double m_epsilon;//single epsilon shared by all the sentences
	int best[][]; // for Viterbi
	
	public FastRestrictedHMM(double epsilon) {
		number_of_topic = 0;
		m_epsilon = epsilon;//in real space!
	}
	
	void init(_Doc d) {
		this.number_of_topic = d.m_topics.length;
		this.length_of_seq = d.getSenetenceSize();
		
		alpha  = new double[this.length_of_seq][2*this.number_of_topic];
		beta = new double[this.length_of_seq][2*this.number_of_topic];
		norm_factor = new double[this.length_of_seq];
	}
	
	//NOTE: in real space!!!!
	double getEpsilon(int t) {
		return m_epsilon;
	}
	
	public double ForwardBackward(_Doc d, double[][] emission) {
		init(d);
		
		double loglik = initAlpha(d.m_topics, emission[0]) + forwardComputation(emission, d.m_topics);
		backwardComputation(emission, d.m_topics);		
		
		return loglik;
	}
	
	//NOTE: all computation in log space
	double initAlpha(double[] theta, double[] local0) {
		double norm = Double.NEGATIVE_INFINITY;//log0
		for (int i = 0; i < this.number_of_topic; i++) {
			alpha[0][i] = local0[i] + theta[i];
			alpha[0][i+this.number_of_topic] = Double.NEGATIVE_INFINITY;//document must start with a new topic
			//this is full computation, but no need to do so
			//norm = Utils.logSum(norm, Utils.logSum(alpha[0][i], alpha[0][i+this.number_of_topic]));
			norm = Utils.logSum(norm, alpha[0][i]);
		}
		
		//normalization
		for (int i = 0; i < this.number_of_topic; i++) {
			alpha[0][i] -= norm;
			//this.alpha[0][i+this.number_of_topic] -= norm; // no need to compute this
		}
		
		norm_factor[0] = norm;
		return norm;
	}
	
	double forwardComputation(double[][] emission, double[] theta) {
		double logLikelihood = 0, norm, logEpsilon, logOneMinusEpsilon;
		for (int t = 1; t < this.length_of_seq; t++) {
			norm = Double.NEGATIVE_INFINITY;//log0
			logEpsilon = Math.log(getEpsilon(t));
			logOneMinusEpsilon = Math.log(1.0 - getEpsilon(t));
			
			for (int i = 0; i < this.number_of_topic; i++) {
				alpha[t][i] = logEpsilon + theta[i] + emission[t][i];  // regardless of the previous
				alpha[t][i+this.number_of_topic] = logOneMinusEpsilon + Utils.logSum(alpha[t-1][i], alpha[t-1][i+this.number_of_topic]) + emission[t][i];
				
				norm = Utils.logSum(norm, Utils.logSum(alpha[t][i], alpha[t][i+this.number_of_topic]));
			}
			
			//normalization
			for (int i = 0; i < this.number_of_topic; i++) {
				alpha[t][i] -= norm;
				alpha[t][i+this.number_of_topic] -= norm;
			}
			
			logLikelihood += norm; 
			norm_factor[t] = norm;
		}
		return logLikelihood;
	}
	
	void backwardComputation(double[][] emission, double[] theta) {
		double sum, logEpsilon, logOneMinusEpsilon;
		for(int t=this.length_of_seq-2; t>=0; t--) {
			logEpsilon = Math.log(getEpsilon(t+1));
			logOneMinusEpsilon = Math.log(1.0 - getEpsilon(t+1));
			
			sum = Double.NEGATIVE_INFINITY;//log0
			for (int j = 0; j < this.number_of_topic; j++)
				sum = Utils.logSum(sum, theta[j] + emission[t+1][j] + beta[t+1][j]);
			sum += logEpsilon;
			
			for (int i = 0; i < this.number_of_topic; i++) {
				beta[t][i] = Utils.logSum(logOneMinusEpsilon + beta[t+1][i] + emission[t+1][i], sum) - norm_factor[t];
				beta[t][i + this.number_of_topic] = beta[t][i];
			}
		}
	}
	
	public void collectExpectations(double[][] sstat) {
		for(int t=0; t<this.length_of_seq; t++) {
			double norm = Double.NEGATIVE_INFINITY;//log0
			for(int i=0; i<2*this.number_of_topic; i++) 
				norm = Utils.logSum(norm, alpha[t][i] + beta[t][i]);
			
			for(int i=0; i<2*this.number_of_topic; i++) 
				sstat[t][i] = Math.exp(alpha[t][i] + beta[t][i] - norm); // convert into original space
		}
	}
	
	//-----------------Viterbi Algorithm--------------------//
	//NOTE: all computation in log space
	public void ComputeAllalphas(double[][] emission, double[] theta) {
		double norm, logEpsilon, logOneMinusEpsilon;
		
		for (int t = 1; t < this.length_of_seq; t++) {
			int prev_best = FindBestInLevel(t-1);
			norm = Double.NEGATIVE_INFINITY;//log0
			logEpsilon = Math.log(getEpsilon(t));
			logOneMinusEpsilon = Math.log(1.0 - getEpsilon(t));
			
			for (int i = 0; i < this.number_of_topic; i++) {
				alpha[t][i] = alpha[t-1][prev_best] + theta[i] + emission[t][i] + logEpsilon;
				best[t][i] = prev_best;
				if(alpha[t-1][i] > alpha[t-1][i+this.number_of_topic]) {
					alpha[t][i+this.number_of_topic] = alpha[t-1][i] + logOneMinusEpsilon + emission[t][i];
					best[t][i+this.number_of_topic] = i;
				} else {
					alpha[t][i+this.number_of_topic] = alpha[t-1][i+this.number_of_topic] + logOneMinusEpsilon + emission[t][i];
					best[t][i+this.number_of_topic] = i + this.number_of_topic;
				}
				norm = Utils.logSum(norm, Utils.logSum(alpha[t][i], alpha[t][i+this.number_of_topic]));
			}// End for i

			//normalization
			for (int i = 0; i < this.number_of_topic; i++) {
				alpha[t][i] -= norm;
				alpha[t][i+this.number_of_topic] -= norm;
			}
		}//End For t
	}

	int FindBestInLevel(int t) {
		double best = alpha[t][0];
		int best_index = 0;
		for(int i = 1; i<2*this.number_of_topic; i++){
			if(alpha[t][i] > best){
				best = alpha[t][i];
				best_index = i;
			}
		}
		return best_index;
	}
	
	public void BackTrackBestPath(_Doc d, double[][] emission, int[] path) {
		this.number_of_topic = d.m_topics.length;
		this.length_of_seq = d.getSenetenceSize();
		alpha = new double[this.length_of_seq][2*this.number_of_topic];
		this.best = new int [this.length_of_seq][2*this.number_of_topic];
		
		initAlpha(d.m_topics,emission[0]);
		ComputeAllalphas(emission, d.m_topics);
		
		int level = this.length_of_seq - 1;
		path[level] = FindBestInLevel(level);
		for(int i = this.length_of_seq - 2; i>=0; i--)
			path[i] = best[i+1][path[i+1]];  
	}
	
}
