package cn.chwyteam.www.User.Dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import cn.chwyteam.www.User.Pojo.User;

@Service
public interface UserDao {
	
	//注册用户
	public int InsertUser(User user);  
	
	//通过用户名查找用户
	public User FindUserByUname(@Param("uname")String uname);   
	
	//修改用户基础信息
	public void ModificationUserInfo(User uer);
	
	//通过ID查找用户
	public User FindUserByUserId(@Param("uid")int uid);
	
	//学号绑定
	public void StudentIDBinding(String sid,String uname);
	
	//邮箱是否存在
	public Integer FindUserEmail(String email);
	
	

}
