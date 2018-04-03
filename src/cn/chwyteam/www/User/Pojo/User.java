package cn.chwyteam.www.User.Pojo;

public class User {

	 Integer uid;
	 String uname="";
	 String pwd="";
	 String tid="";
	 String headimg="";
	 String email="";
	 byte[] group;
	 String sid="";
	
	public Integer getUid() {
		return uid;
	}
	public void setUid(Integer uid) {
		this.uid = uid;
	}
	public String getUname() {
		return uname;
	}
	public void setUname(String uname) {
		this.uname = uname;
	}
	public String getPwd() {
		return pwd;
	}
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	public String getTid() {
		return tid;
	}
	public void setTid(String tid) {
		this.tid = tid;
	}
	public String getHeadimg() {
		return headimg;
	}
	public void setHeadimg(String headimg) {
		this.headimg = headimg;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getSid() {
		return sid;
	}
	public void setSid(String sid) {
		this.sid = sid;
	}
	public byte[] getGroup() {
		return group;
	}
	public void setGroup(byte[] group) {
		this.group = group;
	}

	
	
	 
}
