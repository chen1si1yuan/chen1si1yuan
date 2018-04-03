package cn.chwyteam.www.User.Dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import cn.chwyteam.www.User.Pojo.User;

@Service
public interface UserDao {
	
	//ע���û�
	public int InsertUser(User user);  
	
	//ͨ���û��������û�
	public User FindUserByUname(@Param("uname")String uname);   
	
	//�޸��û�������Ϣ
	public void ModificationUserInfo(User uer);
	
	//ͨ��ID�����û�
	public User FindUserByUserId(@Param("uid")int uid);
	
	//ѧ�Ű�
	public void StudentIDBinding(String sid,String uname);
	
	//�����Ƿ����
	public Integer FindUserEmail(String email);
	
	

}
