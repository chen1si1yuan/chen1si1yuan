package cn.chwyteam.www.Share.Dao;

import java.util.List;

import org.springframework.stereotype.Service;

import cn.chwyteam.www.Share.Pojo.Share;

@Service
public interface ShareDao {
	
	/**
	 * 通过评论动态发布时间获得 前10条动态
	 * @param time
	 * @return
	 */
	public List<Share> FindShare(String time);
	
	
	/**
	 * 插入动态信息
	 * @param share
	 */
	public void InsertShare(Share share);
	
	/**
	 * 通过share对象动态获取动态信息
	 * @param share
	 * @return
	 */
	public List<Share> FindShare1(Share share);


}
