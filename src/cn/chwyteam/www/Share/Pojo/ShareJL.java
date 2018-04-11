package cn.chwyteam.www.Share.Pojo;

import java.util.List;

import cn.chwyteam.www.User.Pojo.User;

public class ShareJL {

	
	Integer share_id;
	Integer user_id;
	String txt="";
	String time="";
	Integer like_count=0;
	String image_url="";
	List<Comment> comments;
	User user;
	
	public Integer getShare_id() {
		return share_id;
	}
	public void setShare_id(Integer share_id) {
		this.share_id = share_id;
	}
	public Integer getUser_id() {
		return user_id;
	}
	public void setUser_id(Integer user_id) {
		this.user_id = user_id;
	}
	public String getTxt() {
		return txt;
	}
	public void setTxt(String txt) {
		this.txt = txt;
	}

	public Integer getLike_count() {
		return like_count;
	}
	public void setLike_count(Integer like_count) {
		this.like_count = like_count;
	}
	public String getImage_url() {
		return image_url;
	}
	public void setImage_url(String image_url) {
		this.image_url = image_url;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	@Override
	public String toString() {
		String commentsst="";
		for (Comment comment : comments) {
			commentsst+=comment.toString();
		}
		return "ShareJL [share_id=" + share_id + ", user_id=" + user_id + ", txt=" + txt + ", time=" + time
				+ ", like_count=" + like_count + ", image_url=" + image_url + ", comments=" + commentsst + ", user="
				+ user.toString() + "]";
	}

	
	



}
