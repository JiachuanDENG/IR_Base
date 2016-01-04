package structures;

import java.util.ArrayList;

import utils.Utils;

/***
 * @author lin
 * The data structure stores the information of a user used in CoLinAdapt.
 */

public class _User {
	
	protected String m_userID;
	
	//text reviews associated with this user
	protected ArrayList<_Review> m_reviews; //The reviews the user have, they should be by ordered by time stamps.
	
	//profile for this user
	protected double[] m_lowDimProfile;
	protected _SparseFeature[] m_BoWProfile; //The BoW representation of a user.
	
	//personalized prediction model
	protected double[] m_pWeight;
	protected int m_classNo;
	protected int m_featureSize;
	
	public _User(String userID, ArrayList<_Review> reviews){
		m_userID = userID;
		m_reviews = reviews;

		m_lowDimProfile = null;
		m_BoWProfile = null;
		m_pWeight = null;

		constructSparseVector();
	}
	
	// Get the user ID.
	public String getUserID(){
		return m_userID;
	}
	
	public void setModel(double[] weight, int classNo, int featureSize) {
		m_pWeight = new double[weight.length];
		System.arraycopy(weight, 0, m_pWeight, 0, weight.length);
		m_classNo = classNo;
		m_featureSize = featureSize;
	}
	
	public void constructSparseVector(){
		ArrayList<_SparseFeature[]> reviews = new ArrayList<_SparseFeature[]>();

		for(_Review r: m_reviews) 
			reviews.add(r.getSparse());
		
		m_BoWProfile = Utils.MergeSpVcts(reviews);// this BoW representation is not normalized?!
	}
	
	//Get the sparse vector of the user.
	public _SparseFeature[] getBoWProfile(){
		return m_BoWProfile;
	}
	
	public int getReviewSize() {
		return m_reviews.size();
	}
	
	public ArrayList<_Review> getReviews(){
		return m_reviews;
	}
	
	public double getBoWSim(_User u) {
		return Utils.cosine(m_BoWProfile, u.getBoWProfile());
	}
	
	public double getSVDSim(_User u) {
		return Utils.cosine(u.m_lowDimProfile, m_lowDimProfile);
	}
	
	public int predict(_Doc doc) {
		_SparseFeature[] fv = doc.getSparse();
		double maxScore = Utils.dotProduct(m_pWeight, fv, 0);
		
		if (m_classNo==2) {
			return maxScore>0?1:0;
		} else {//we will have k classes for multi-class classification
			double score;
			int pred = 0; 
			
			for(int i = 1; i < m_classNo; i++) {
				score = Utils.dotProduct(m_pWeight, fv, i * (m_featureSize + 1));
				if (score>maxScore) {
					maxScore = score;
					pred = i;
				}
			}
			return pred;
		}
	}
 }
