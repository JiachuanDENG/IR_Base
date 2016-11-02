package structures;

import java.util.ArrayList;

public class _CorpusCfLara extends _Corpus {
	public ArrayList<_UserCfLara> userList;
	public ArrayList<_ItemCfLara> itemList;
	
	public _CorpusCfLara(){
		super();
		this.userList=new ArrayList<_UserCfLara>();
		itemList=new ArrayList<_ItemCfLara>();
	}
	public void setUserList(ArrayList<_UserCfLara>userList){
		this.userList=userList;
	}
	
	public void setItemList(ArrayList<_ItemCfLara>itemList){
		this.itemList=itemList;
	}
	
	public ArrayList<_UserCfLara> getUserList(){
		return this.userList;
	}
	
	public ArrayList<_ItemCfLara> getItemList(){
		return this.itemList;
	}

	public _UserCfLara getUser(String userID){
//		_UserCfLara user=null;
//		System.out.println("getUser");
		if (userList.size()!=0){
		for (_UserCfLara u:userList){
			if (u.m_userName.equals(userID)){
				return u;
			}
		}}
		
	   _UserCfLara u =new _UserCfLara(userList.size(),userID);
	   this.userList.add(u);
	  
		return u;
	}
	
	public _ItemCfLara getItem(String itemID){
		_ItemCfLara item=null;
		if (itemList.size()!=0){
		for (_ItemCfLara i:itemList){
			if (i.m_itemName.equals(itemID)){
				return i;
			}
		}}
		_ItemCfLara i=new _ItemCfLara(itemList.size(),itemID);
		this.itemList.add(i);
		return i;
		}
}
