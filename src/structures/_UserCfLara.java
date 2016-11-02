package structures;
/***
 * @author Jiachuan
 * The data structure stores the information of a user used in cfLara.
 */
import utils.Utils;
public class _UserCfLara {
public String m_userName;
public int m_userID;
public double[]m_lambda_u,m_sigma_u;

public _UserCfLara(int uid,String m_userName){
	this.m_userName=m_userName;
	this.m_userID=uid;
	m_lambda_u=null;
	m_sigma_u=null;
}

public void set_m_lamda_u(int k,double alpha){
	m_lambda_u=new double[k];
	Utils.randomize(m_lambda_u, alpha);
}

public void set_m_sigma_u(int k,double alpha){
	m_sigma_u=new double[k];
	Utils.randomize(m_sigma_u, alpha);
}
}
