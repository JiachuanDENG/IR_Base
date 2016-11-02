package structures;

import utils.Utils;

/***
 * @author Jiachuan
 * The data structure stores the information of a item used in cfLara.
 */
public class _ItemCfLara {
	public String m_itemName;
	public int m_itemID;
	public double[] m_lambda_v,m_sigma_v;
	
	public _ItemCfLara(int iid,String itemID){
		this.m_itemID=iid;
		this.m_itemName=itemID;
	}
	public void set_m_lamda_v(int k,double alpha){
		m_lambda_v=new double[k];
		Utils.randomize(m_lambda_v, alpha);
	}

	public void set_m_sigma_v(int k,double alpha){
		m_sigma_v=new double[k];
		Utils.randomize(m_sigma_v, alpha);
	}

}
