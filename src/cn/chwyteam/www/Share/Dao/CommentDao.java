package cn.chwyteam.www.Share.Dao;

import java.util.List;

import org.springframework.stereotype.Service;

import cn.chwyteam.www.Share.Pojo.Comment;

@Service
public interface CommentDao {
	
	/**
	 * 通过动态标识符share_id获得所有评论标识符
	 * @param share_id
	 * @return
	 */
	public List<Integer> GetCommentId(int share_id);
	
	/**
	 * 通过评论标识符获得相应评论
	 * @param CommentId
	 * @return
	 */
	public Comment GetCommentByCommentId(int CommentId);
	
	/**
	 * 通过动态标识符share_id获得所有评论
	 * @param share_id
	 * @return
	 */
	public List<Comment> GetCommentByShareId(int share_id);
	
	


}
