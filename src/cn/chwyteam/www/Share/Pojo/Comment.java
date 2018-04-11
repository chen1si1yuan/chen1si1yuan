package cn.chwyteam.www.Share.Pojo;

import java.util.Date;

public class Comment {
	Integer communicated_id;
	Integer share_id;
	Integer uid;
	String txt;
	String time;
	
	
	public Integer getCommunicated_id() {
		return communicated_id;
	}
	public void setCommunicated_id(Integer communicated_id) {
		this.communicated_id = communicated_id;
	}
	public Integer getShare_id() {
		return share_id;
	}
	public void setShare_id(Integer share_id) {
		this.share_id = share_id;
	}
	
	public String getTxt() {
		return txt;
	}
	public void setTxt(String txt) {
		this.txt = txt;
	}
	
	public Integer getUid() {
		return uid;
	}
	public void setUid(Integer uid) {
		this.uid = uid;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	@Override
	public String toString() {
		return "Comment [communicated_id=" + communicated_id + ", share_id=" + share_id + ", uid=" + uid + ", txt="
				+ txt + ", time=" + time + "]";
	}

}
