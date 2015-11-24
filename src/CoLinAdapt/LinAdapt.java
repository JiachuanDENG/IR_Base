package CoLinAdapt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import structures.MyLinkedList;
import structures._PerformanceStat;
import structures._Review;
import structures._User;

/****
 * In this class, we collect all adaptation users and try to schedule reviews and adaptation process.
 * @author lin
 */
public class LinAdapt {
	
	protected ArrayList<_User> m_users; //Mapping from users to models.
	protected HashMap<String, Integer> m_userIDIndexMap; // key: userID, value: index.
	protected TreeMap<Integer, ArrayList<Integer>> m_featureGroupIndex;
	protected String[] m_userIDs; // If we know index, we can get the userID.
	protected OneLinAdapt[] m_userModels; //The array contains all users's models.
	protected MyLinkedList<_Review> m_trainQueue;
	protected int m_featureNo, m_featureGroupNo;
	protected double[] m_globalWeights;
	
	public LinAdapt(ArrayList<_User> users, int featureNo, int featureGroupNo, TreeMap<Integer, ArrayList<Integer>> featureGroupIndex){
		m_users = users;
		m_userIDs = new String[users.size()];
		m_userModels = new OneLinAdapt[users.size()];
		m_userIDIndexMap = new HashMap<String, Integer>();
		m_featureNo = featureNo;
		m_featureGroupNo = featureGroupNo;
		m_featureGroupIndex = featureGroupIndex;
	}
	
	//Set the global weights.
	public void setGlobalWeights(double[] ws){
		m_globalWeights = ws;
	}

	//Fill in the user related information map and array.
	public void init(){
		_User user;
		for(int i=0; i<m_users.size(); i++){
			user = m_users.get(i);
			m_userIDs[i] = user.getUserID();
			m_userIDIndexMap.put(user.getUserID(), i);
			m_userModels[i] = new OneLinAdapt(user, m_featureGroupNo, m_featureNo, m_featureGroupIndex, m_globalWeights);
			m_userModels[i].init();
		}
	}
	
	
	//In this process, we collect one review from each user and train online.
	public void onlineTrain(){
		int predL;
		int[] predLabels;
		int[] trueLabels;
		OneLinAdapt model;
		ArrayList<_Review> reviews;
		
		//The review order doesn't matter, so we can adapt one user by one user.
		for(int i=0; i<m_users.size(); i++){
			model = m_userModels[i];
			reviews = m_users.get(i).getReviews();
			predLabels = new int[reviews.size()];
			trueLabels = new int[reviews.size()];
			for(int j=0; j<reviews.size(); j++){
				//Predict first.
				predL = model.predict(reviews.get(j));
				predLabels[j] = predL;
				trueLabels[j] = reviews.get(j).getYLabel();
				
				//Adapt based on the new review.
				ArrayList<_Review> trainSet = new ArrayList<_Review>();
				trainSet.add(reviews.get(j));
				model.train(trainSet);
			}
			model.fillTrueLabels(trueLabels);
			model.fillPredLabels(predLabels);
		}
	}
	
	//Accumulate the performance, accumulate all users and get stat.
	public void calcPerformance(){
		for(OneLinAdapt l: m_userModels){
			l.m_perfStat.calcuatePreRecF1();
		}
	}
}
