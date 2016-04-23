package Classifier.supervised.modelAdaptation.CoLinAdapt;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import Classifier.supervised.modelAdaptation._AdaptStruct;
import LBFGS.LBFGS;
import LBFGS.LBFGS.ExceptionWithIflag;
import structures._Doc;
import structures._RankItem;
import structures._Review;
import structures._SparseFeature;
import structures._User;
import structures._Review.rType;
import utils.Utils;

public class CoLinAdaptWithDiffFeatureGroups extends CoLinAdapt{
	
	int m_dimA;
	int m_dimB;
	int[] m_featureGroupMapB; // bias term is at position 0
	double[] m_cache;
	double[] m_pWeightsB = new double[m_featureSize + 1];
	
	public CoLinAdaptWithDiffFeatureGroups(int classNo, int featureSize,
			HashMap<String, Integer> featureMap, int topK, String globalModel,
			String featureGroupMap, String featureGroupMapB) {
		super(classNo, featureSize, featureMap, topK, globalModel, featureGroupMap);
		m_dimA = m_dim;
		loadFeatureGroupMapB(featureGroupMapB); // Load the feature group map for the other class.
	}
	
	int getASize(){
		return m_dimA*2*m_userList.size();
	}
	
	int getBSize(){
		return m_dimB*2*m_userList.size();
		
	}
	
	@Override
	int getVSize() {
		return getASize() + getBSize();
	} 
	
	void constructUserList(ArrayList<_User> userList) {
		int ASize = 2*m_dimA;
		int BSize = 2*m_dimB;
		
		//step 1: create space
		m_userList = new ArrayList<_AdaptStruct>();		
		for(int i=0; i<userList.size(); i++) {
			_User user = userList.get(i);
			m_userList.add(new _CoLinAdaptDiffFvGroupsStruct(user, m_dimA, i, m_topK, m_dimB));
		}
		m_pWeights = new double[m_gWeights.length];			
		
		//huge space consumption
		_CoLinAdaptDiffFvGroupsStruct.sharedA = new double[getASize()];
		_CoLinAdaptDiffFvGroupsStruct.sharedB = new double[getBSize()];
		
		//step 2: copy each user's A and B to shared A and B in _CoLinAdaptStruct		
		_CoLinAdaptDiffFvGroupsStruct user;
		for(int i=0; i<m_userList.size(); i++) {
			user = (_CoLinAdaptDiffFvGroupsStruct)m_userList.get(i);
			System.arraycopy(user.m_A, 0, _CoLinAdaptDiffFvGroupsStruct.sharedA, ASize*i, ASize);
			System.arraycopy(user.m_B, 0, _CoLinAdaptDiffFvGroupsStruct.sharedB, BSize*i, BSize);
		}
	}
	
	// Feature group map for the super user.
	public void loadFeatureGroupMapB(String filename){
		// If there is no feature group for the super user.
		if(filename == null){
			m_dimB = m_featureSize + 1;
			m_featureGroupMapB = new int[m_featureSize + 1]; //One more term for bias, bias->0.
			for(int i=0; i<=m_featureSize; i++)
				m_featureGroupMapB[i] = i;
			return;
		} else{// If there is feature grouping for the super user, load it.
			try{
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
				String[] features = reader.readLine().split(",");//Group information of each feature.
				reader.close();
				
				m_featureGroupMapB = new int[features.length + 1]; //One more term for bias, bias->0.
				m_dimB = 0;
				//Group index starts from 0, so add 1 for it.
				for(int i=0; i<features.length; i++) {
					m_featureGroupMapB[i+1] = Integer.valueOf(features[i]) + 1;
					if (m_dimB < m_featureGroupMapB[i+1])
						m_dimB = m_featureGroupMapB[i+1];
				}
				m_dimB ++;
			} catch(IOException e){
				System.err.format("[Error]Fail to open super user group file %s.\n", filename);
			}
		}
		System.out.format("[Info]Feature group size for super user %d\n", m_dimB);
	}

	// There is still issue in calculating R2 since we don't know which set to use for a user.
	@Override
	protected double calculateFuncValue(_AdaptStruct u) {		
		double fValue = super.calculateFuncValue(u), R2 = 0, diffA, diffB;
		
		//R2 regularization
		_CoLinAdaptDiffFvGroupsStruct ui = (_CoLinAdaptDiffFvGroupsStruct)u, uj;
		for(_RankItem nit:ui.getNeighbors()) {
			uj = (_CoLinAdaptDiffFvGroupsStruct)m_userList.get(nit.m_index);
			diffA = 0;
			diffB = 0;
			for(int k=0; k<m_dim; k++) {
				diffA += (ui.getScaling(k) - uj.getScaling(k)) * (ui.getScaling(k) - uj.getScaling(k));
				diffB += (ui.getShifting(k) - uj.getShifting(k)) * (ui.getShifting(k) - uj.getShifting(k));
			}
			// We also need to sum over the another set of parameters.
			for(int k=0; k<m_dimB; k++){
				diffA += (ui.getScalingB(k) - uj.getScalingB(k)) * (ui.getScalingB(k) - uj.getScalingB(k));
				diffB += (ui.getShiftingB(k) - uj.getShiftingB(k)) * (ui.getShiftingB(k) - uj.getShiftingB(k));
			}
			R2 += nit.m_value * (m_eta3*diffA + m_eta4*diffB);
		}
		return fValue + R2;
	}
	
	//Calculate the function value of the new added instance.
	protected double calcLogLikelihood(_AdaptStruct user){
		double L = 0; //log likelihood.
		double Pi = 0;
		for(_Review review:user.getReviews()){
			if (review.getType() != rType.ADAPTATION)
				continue; // only touch the adaptation data
			calcPosterior(review.getSparse(), user);
			if(review.getYLabel() == 1)
				Pi = m_cache[1];
			else
				Pi = m_cache[0];
			
			// Why do we need to judge inside?
			if(review.getYLabel() == 1) {
				if (Pi>0.0)
					L += Math.log(Pi);					
				else
					L -= Utils.MAX_VALUE;
			} else {
				if (Pi<1.0)
					L += Math.log(1 - Pi);					
				else
					L -= Utils.MAX_VALUE;
			}
		}
		return L/getAdaptationSize(user);
	}

	public void calcPosterior(_SparseFeature[] fvs, _AdaptStruct u){
		m_cache = new double[2];
		// We want get p(y=0|x) and p(y=1|x) based on ylabel.
		_CoLinAdaptDiffFvGroupsStruct user = (_CoLinAdaptDiffFvGroupsStruct)u;
		double exp0 = 0, exp1 = 0;
		int n = 0, k = 0; // feature index and feature group index
		// w0*x
		exp0 = user.getScaling(0)*m_gWeights[0] + user.getShifting(0);// Bias term: w0*a0+b0.
		for(_SparseFeature fv: fvs){
			n = fv.getIndex() + 1;
			k = m_featureGroupMap[n];
			exp0 += (user.getScaling(k)*m_gWeights[n] + user.getShifting(k)) * fv.getValue();
		}
		// w1*x
		exp1 = user.getScalingB(0)*m_gWeights[0] + user.getShiftingB(0); // Bias term.
		for(_SparseFeature fv: fvs){
			n = fv.getIndex() + 1;
			k = m_featureGroupMapB[n];
			exp1 += (user.getScalingB(k)*m_gWeights[n] + user.getShiftingB(k)) * fv.getValue();
		}
		// Return corresponding values accordingly.
		m_cache[0] = Math.exp(exp0)/(Math.exp(exp0)+Math.exp(exp1));
		m_cache[1] = Math.exp(exp1)/(Math.exp(exp0)+Math.exp(exp1));
	}
	
	//shared gradient calculation by batch and online updating
	@Override
	protected void gradientByFunc(_AdaptStruct u, _Doc review, double weight) {
		_CoLinAdaptDiffFvGroupsStruct user = (_CoLinAdaptDiffFvGroupsStruct)u;
		
		int n, k; // feature index and feature group index		
		int offset = 2*m_dim*user.getId();//general enough to accommodate both LinAdapt and CoLinAdapt
		double Pij = 0, delta = 0;
		
		// j=0
		Pij = m_cache[0];
		if(review.getYLabel() == 0)
			delta = 1 - Pij;
		else
			delta = - Pij;
		delta /= getAdaptationSize(user);
		//Bias term.
		m_g[offset] -= weight*delta*m_gWeights[0]; //a[0] = w0*x0; x0=1
		m_g[offset + m_dim] -= weight*delta;//b[0]
		//Traverse all the feature dimension to calculate the gradient.
		for(_SparseFeature fv: review.getSparse()){
			n = fv.getIndex() + 1;
			k = m_featureGroupMap[n];
			m_g[offset + k] -= weight * delta * m_gWeights[n] * fv.getValue();
			m_g[offset + m_dim + k] -= weight * delta * fv.getValue();  
		}
	
		// j=1
		Pij = m_cache[1];
		if(review.getYLabel() == 1)
			delta = 1 - Pij;
		else
			delta = -Pij;
		delta /= getAdaptationSize(user);
		offset = 2*m_dim*m_userList.size() + 2*m_dimB*user.getId(); // Offset starts for another class.
		m_g[offset] -= weight*delta*m_gWeights[0]; // a[0]
		m_g[offset + m_dimB] -= weight*delta; // b[0]
		for(_SparseFeature fv: review.getSparse()){
			n = fv.getIndex() + 1;
			k = m_featureGroupMapB[n];
			m_g[offset + k] -= weight * delta * m_gWeights[n] * fv.getValue();
			m_g[offset + m_dimB + k] -= weight * delta * fv.getValue();  
		}	
	}
	
	@Override
	protected void gradientByR1(_AdaptStruct u){
		super.gradientByR1(u);
		_CoLinAdaptDiffFvGroupsStruct user = (_CoLinAdaptDiffFvGroupsStruct)u;
		int offset = 2*m_dim*m_userList.size() + 2*m_dimB*user.getId();//general enough to accommodate both LinAdapt and CoLinAdapt
		//R1 regularization part for another class.
		for(int k=0; k<m_dimB; k++){
			m_g[offset + k] += 2 * m_eta1 * (user.getScalingB(k)-1);// add 2*eta1*(a_k-1)
			m_g[offset + k + m_dimB] += 2 * m_eta2 * user.getShiftingB(k); // add 2*eta2*b_k
		}
	}
	
	//Calculate the gradients for the use in LBFGS.
	protected void gradientByR2(_AdaptStruct user){
		// Part 1, gradients from class 0.
		super.gradientByR2(user);
		
		_CoLinAdaptDiffFvGroupsStruct ui = (_CoLinAdaptDiffFvGroupsStruct)user, uj;
		int offseti = m_userList.size()*m_dim*2 + m_dimB*2*ui.getId(), offsetj;
		double coef, dA, dB;
		// Part 2, gradients from class 1.
		for(_RankItem nit:ui.getNeighbors()) {
			uj = (_CoLinAdaptDiffFvGroupsStruct)m_userList.get(nit.m_index);
			offsetj = m_userList.size()*m_dim*2 + m_dimB*2*uj.getId();
			coef = 2 * nit.m_value;
			
			for(int k=0; k<m_dim; k++) {
				dA = coef * m_eta3 * (ui.getScalingB(k) - uj.getScalingB(k));
				dB = coef * m_eta4 * (ui.getShiftingB(k) - uj.getShiftingB(k));
				
				// update ui's gradient
				m_g[offseti + k] += dA;
				m_g[offseti + k + m_dim] += dB;
				
				// update uj's gradient
				m_g[offsetj + k] -= dA;
				m_g[offsetj + k + m_dim] -= dB;
			}			
		}
	}
	//this is batch training in each individual user
	@Override
	public double train(){
		int[] iflag = {0}, iprint = {-1, 3};
		double fValue, oldFValue = Double.MAX_VALUE;;
		int vSize = getVSize(), displayCount = 0;
//		double oldMag = 0;
		_CoLinAdaptDiffFvGroupsStruct user;
			
		initLBFGS();
		init();
		try{
			do{
				fValue = 0;
				Arrays.fill(m_g, 0); // initialize gradient				
					
				// accumulate function values and gradients from each user
				for(int i=0; i<m_userList.size(); i++) {
					user = (_CoLinAdaptDiffFvGroupsStruct)m_userList.get(i);
					fValue += calculateFuncValue(user);
					calculateGradients(user);
				}
					
				//added by Lin for stopping lbfgs.
				double curMag = gradientTest();
//				if(Math.abs(oldMag -curMag)<0.1) 
//					break;
//				oldMag = curMag;
					
				if (m_displayLv==2) {
					System.out.println("Fvalue is " + fValue);
				} else if (m_displayLv==1) {
					if (fValue<oldFValue)
						System.out.print("o");
					else
						System.out.print("x");
						
					if (++displayCount%100==0)
						System.out.println();
				} 
				oldFValue = fValue;
					
				LBFGS.lbfgs(vSize, 5, _CoLinAdaptDiffFvGroupsStruct.getSharedAB(), fValue, m_g, false, m_diag, iprint, 1e-3, 1e-16, iflag);//In the training process, A is updated.
			} while(iflag[0] != 0);
			System.out.println();
		} catch(ExceptionWithIflag e) {
			e.printStackTrace();
		}		
		
		setPersonalizedModel();
		return oldFValue;
	}
	
	@Override
	public void setPersonalizedModel(){
		super.setPersonalizedModel();
		m_pWeightsB = new double[m_featureSize + 1];
		int gid;
		_CoLinAdaptDiffFvGroupsStruct user;
		for(int i=0; i<m_userList.size(); i++) {
			user = (_CoLinAdaptDiffFvGroupsStruct)m_userList.get(i);
			
			//set bias term
			m_pWeightsB[0] = user.getScalingB(0) * m_gWeights[0] + user.getShiftingB(0);
			
			//set the other features
			for(int n=0; n<m_featureSize; n++) {
				gid = m_featureGroupMapB[1+n];
				m_pWeightsB[1+n] = user.getScalingB(gid) * m_gWeights[1+n] + user.getShiftingB(gid);
			}
			user.setPersonalizedModelB(m_pWeightsB);
		}
	}
	@Override
	protected int predict(_Doc review, _AdaptStruct user) {
		if (review==null)
			return -1;
		else{
			_SparseFeature[] fvs = review.getSparse();
			calcPosterior(fvs, user);
			return Utils.maxOfArrayIndex(m_cache);
		}
	}
}
