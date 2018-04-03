package cn.chwyteam.www.Share.Dao;

import java.util.List;

import org.springframework.stereotype.Service;

import cn.chwyteam.www.Share.Pojo.Comment;

@Service
public interface CommentDao {
	
	/**
	 * ͨ����̬��ʶ��share_id����������۱�ʶ��
	 * @param share_id
	 * @return
	 */
	public List<Integer> GetCommentId(int share_id);
	
	/**
	 * ͨ�����۱�ʶ�������Ӧ����
	 * @param CommentId
	 * @return
	 */
	public Comment GetCommentByCommentId(int CommentId);
	
	/**
	 * ͨ����̬��ʶ��share_id�����������
	 * @param share_id
	 * @return
	 */
	public List<Comment> GetCommentByShareId(int share_id);
	
	


}
