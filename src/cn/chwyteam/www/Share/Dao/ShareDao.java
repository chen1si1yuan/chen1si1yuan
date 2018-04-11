package cn.chwyteam.www.Share.Dao;

import java.util.List;

import org.springframework.stereotype.Service;

import cn.chwyteam.www.Share.Pojo.Share;
import cn.chwyteam.www.Share.Pojo.ShareJL;

@Service
public interface ShareDao {
	
	/**
	 * ͨ�����۶�̬����ʱ���� ǰ10����̬
	 * @param time
	 * @return
	 */
	public List<Share> FindShare(String time);
	
	
	/**
	 * ���붯̬��Ϣ
	 * @param share
	 */
	public void InsertShare(Share share);
	
	/**
	 * ͨ��share����̬��ȡ��̬��Ϣ
	 * @param share
	 * @return
	 */
	public List<Share> FindShare1(Share share);
	
	public ShareJL findShareJLByshareid(int share_id);


}
