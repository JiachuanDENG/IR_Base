package structures;

import java.util.ArrayList;
import java.util.Arrays;
import utils.Utils;
/***
 * @author Jiachuan
 * The data structure stores the information of a document used in cfLara.
 */
public class _DoccfLara extends _Doc{
	public _UserCfLara m_user;
	public _ItemCfLara m_item;
	public double[] m_pi_d,m_gamma_d,m_lambda_ds,m_sigma_ds;//m_phi is in class _Doc
	public _DoccfLara(int ID, _UserCfLara user,_ItemCfLara item,String source, int ylabel) {
		
		super(ID, source, ylabel);
		this.m_user=user;
		this.m_item=item;

	}
	
	public void set_m_pi_d(int k, double alpha){
		m_pi_d=new double[k];
		Utils.randomize(m_pi_d, alpha);
	}
	
	public void set_m_gamma_d(int k, double alpha){
		m_gamma_d=new double[k];
		Utils.randomize(m_gamma_d, alpha);
	}
	
	public void set_m_lambda_ds(int k, double alpha){
		m_lambda_ds=new double[k];
		Utils.randomize(m_lambda_ds, alpha);
	}
	
	public void set_m_sigma_ds(int k, double alpha){
		m_sigma_ds=new double[k];
		Utils.randomize(m_sigma_ds, alpha);
	}
	
	
	
}
