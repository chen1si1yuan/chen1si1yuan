package cn.chwyteam.www.Controller.DaoImpl.User;

import java.io.FileOutputStream;

import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import org.springframework.stereotype.Service;


import cn.chwyteam.www.DES.Base64;
import cn.chwyteam.www.DES.Cryption;
import cn.chwyteam.www.Share.Dao.CommentDao;
import cn.chwyteam.www.Share.Dao.ShareDao;
import cn.chwyteam.www.Share.Pojo.Comment;
import cn.chwyteam.www.Share.Pojo.Share;
import cn.chwyteam.www.TimerServlet.Timer1;
import cn.chwyteam.www.User.Dao.UserDao;
import cn.chwyteam.www.User.Pojo.User;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import sun.misc.BASE64Decoder; 
import sun.misc.BASE64Encoder;

@Service
public class UserControllerDaoImpl {

	@Autowired
	UserDao userdao;
	
	@Autowired
	ShareDao sharedao;
	
	private static final Log logger =LogFactory.getLog(UserControllerDaoImpl.class);
	
	
	
	
	
	   /**
	    * 
	    * �û���¼�����ķ���
	    * 
	    * @param UserName String �û���  
	    * @param UserPwd String �û�����
	    * @param response HttpServletResponse
	    * @return String ���ز���Ϊjson���ݸ�ʽ���ַ�����ʽ  
	    * @author ��˼Դ
	    */
		public String login(String UserName,String UserPwd,HttpServletResponse response)          
		{
			JSONObject json=new JSONObject();                              //����json����
		    JSONObject UserInfor=new JSONObject();
			try {
			       User user=userdao.FindUserByUname(UserName);	             //ͨ���û��������û���Ϣ ���û�� ������catch�쳣��Ϣ����
			       if(user.getPwd()==UserPwd||user.getPwd().equals(UserPwd))
				    {
			    	 //��json�м���-���ڻ�ȡ�û���Ϣ������
			    	  UserInfor.put("UserId", user.getUid());
			    	  UserInfor.put("UserLink", "");                       
			    	  UserInfor.put("UserName",UserName);                   //�û���ע����
			    	  UserInfor.put("Token", "");                          //�û���¼״̬����
			    	  UserInfor.put("headimg", "");                        //�û���ͷ������
			    	  json.put("statu","true");                            //ע��-��¼�ɹ����
				      json.put("User",UserInfor.toString());	
				      Cryption csy=new Cryption();
				      Cookie cookie1=new Cookie(csy.encryption("UserName"), csy.encryption(UserName));
				      cookie1.setMaxAge(60*60*12);
				      Cookie cookie2=new Cookie(csy.encryption("PassWord"), csy.encryption(UserPwd));
				      cookie2.setMaxAge(60*60*12);
				      response.addCookie(cookie1);
				      response.addCookie(cookie2);
				    }
			       else
			        {
				      json.put("statu","PassWord Error");  
				      logger.info("��¼ʱ�������");
			        }
				}catch(Exception e)
				  {
					  json.put("statu","UserName Not Found");  
					  logger.warn("��¼ʱ�û���������");
					  logger.warn(e.getMessage());
				  }
			return json.toString();
		}
		
		
		

			
		/**
		 * 
		 * ע���û���Ϣ�����ķ���
		 * 
		 * @param UserName String �û��û���
		 * @param UserPwd  String �û�����
		 * @return String json���ݸ�ʽ���ַ�����ʽ  
		 * @author ��˼Դ
		 */
		public String Register(String UserName,String UserPwd,String email,String CAPTCHA)
		{
			
			boolean Flag=false;            //��֤���ܷ�ƥ��  trueΪƥ�� falseΪ��ƥ��
			JSONObject returnjson=new JSONObject();                       //�������ص�json����
		    JSONObject UserInfor=new JSONObject();
		    
		    //�����ʽ��֤
		    String regex="[a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9]+(\\.(com|cn|org|edu|hk))";
			if(!email.matches(regex))
			{
				returnjson.put("statu", "Email Format Wrong");
				logger.info("ע��ʱ�����ʽ����");
				return returnjson.toString();
			}
			
			//����User������������
			  User user=new User();
			  user.setUname(UserName);
			  user.setPwd(UserPwd); 
			  user.setEmail(email);
			  
			  //ʹ���û��������벻Ϊ��
			  if(UserName==null||UserName==""||UserPwd==null||UserPwd=="")
			  {
				  returnjson.put("statu", "UserName Or UserPwd Is Null");
				  logger.info("ע��ʱ�û���������Ϊ��");
				  return returnjson.toString();
			  }
			  
			  //�ж��û����Ƿ����
			  try
			  {
			    User s=userdao.FindUserByUname(UserName);   //ͨ���û�����ѯ�û�                  
			    if(s.getUname()!=null)
			    {
			    	returnjson.put("statu","UserName Exist");
			    	logger.info("ע��ʱ�û����Ѿ�����");
			    	return returnjson.toString();
			    }
			    else
			    	throw new Exception();
			  }catch(Exception e)
			  {
				  logger.warn(e.getMessage());
			  }
			  
			  //��֤����֤
			  if(CAPTCHA==null||CAPTCHA==""||CAPTCHA.equals(""))
			  {
				  returnjson.put("statu","��֤��Ϊ�գ�ע��ʧ��");
				  logger.info("ע��ʱδ������֤��");
			  }
			  else
				  if(Timer1.map.containsKey(email))
					{
						if(CAPTCHA.equals(Timer1.map.get(email))||CAPTCHA==Timer1.map.get(email))
							Flag=true;
						else
							{
							returnjson.put("statu","��֤�벻��ȷ������������");
							logger.info("ע��ʱ��֤�����");
							}        
					}
					else if(Timer1.cachemap.containsKey(email))
					{
						if(CAPTCHA.equals(Timer1.cachemap.get(email))||CAPTCHA==Timer1.cachemap.get(email))
							Flag=true;
						else
						{
							returnjson.put("statu","��֤�벻��ȷ������������");
							logger.info("ע��ʱ��֤�����");
						}
							
					}
					else 
						returnjson.put("statu","��֤����ʧЧ��������ע��");
			  
			  //�û�ע��
			  if(Flag)        //��֤��ƥ��ɹ�
			  {
				  userdao.InsertUser(user);                            //�û�ע��
				  UserInfor.put("UserId",user.getUid());
				  UserInfor.put("UserLink", "");                       //��json�м���-���ڻ�ȡ�û���Ϣ������
				  UserInfor.put("UserName",UserName);                   //�û���ע����
				  UserInfor.put("Token", "");                          //�û���¼״̬����
				  UserInfor.put("headimg", "");                        //�û���ͷ������
				  returnjson.put("statu","true");                            //ע��-��¼�ɹ����
				  returnjson.put("User",UserInfor.toString());
			  }
 
			  return returnjson.toString();
		}
		
		
		

		
		@Autowired
		private JavaMailSenderImpl mailSender;	
		/**
		 * 
		 * ����������֤��������֤��
		 * 
		 * @param UserName String �û���
		 * @param Email String Ŀ������
		 * @return String json���ݸ�ʽ���ַ�����ʽ  
		 * @author ��˼Դ
		 */
		public String EmailSend(String UserName,String Email)
		{
			JSONObject returnjson=new JSONObject();
			try
			  {
			    User s=userdao.FindUserByUname(UserName);                     //�ж��û����Ƿ����
			    if(s.getUname()!=null)
			    { 
			    	returnjson.put("statu","UserName Exist");
			    	return returnjson.toString();
			    }
			    else
			    	throw new Exception();
			  }catch(Exception e)
			  {  
				  logger.warn(e.getMessage());
			  }
			
			//�������ݸ�ʽ��֤
			String regex="[a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9]+(\\.(com|cn|org|edu|hk))";
			if(!Email.matches(regex))
			{
				returnjson.put("statu", "Email Format Wrong");
				logger.info("ע��׶�������֤ʱ�����ʽ����");
				return returnjson.toString();
			}
		
			//if(userdao.FindUserEmail(Email)<0)
			//{
			//	return "�������ѱ�ע��,�뻻�����������֤";
			//}
			try
			{
				//������֤
				SimpleMailMessage mailMessage = new SimpleMailMessage();   
				mailMessage.setTo(Email);
				mailMessage.setFrom("1441565921@qq.com");    
				mailMessage.setSubject("��;�Ƽ�APP��֤����֤"); 
				int code=new Random().nextInt(100000);
				mailMessage.setText("���������ڽ�����;�Ƽ�APP�û�ע����Ϣ������֤ ��������֤��Ϊ��"+code+"��   ���û�н���ע�������������ἴ�ɡ�"); 
				if(Timer1.cachemap.containsKey(Email))
				{
					Timer1.cachemap.remove(Email);
				}
				Timer1.map.put(Email, ""+code);
				mailSender.send(mailMessage); 
				returnjson.put("statu", "true");
				return returnjson.toString();
			}catch(Exception e)
			{
				returnjson.put("statu", "���䷢��ʧ�ܣ���ȷ�������Ƿ�ͨ������������ȷ��");
				logger.warn("ע��׶�������֤ʱ���䷢��ʧ��");
				logger.warn(e.getMessage());
				return returnjson.toString();	
			}
		}
		
		
		
		
				
		/**
		 * 
		 * �޸��û���Ϣ 
		 * @param  Userid int �û�id
		 * @param  ModifyArray JSONArray ��Ҫ���޸ĵı�����json���������Ϣ
		 * @return String json���ݸ�ʽ���ַ�����ʽ  
		 * @author ��˼Դ
		 *
		 */
		public String Modify(int Userid,JSONArray ModifyArray)
		{
			JSONObject returnjson=new JSONObject();              //���巵�ص�json����
			boolean Flag=true;
			//�����޸�ǰ��user������Ϣ
			User beforemodifyuser = new User();  //�޸�ǰuser
			User aftermodifyuser=new User();    //�޸ĺ�user
			
			//ͨ��id��ѯ��ʼ���޸�ǰuser����
			try {
			beforemodifyuser=userdao.FindUserByUserId(Userid);  
			}catch(Exception e)
			{
				returnjson.put("statu", "UserId Not Exits");
				logger.warn("ͨ���û���ʶ���޸���Ϣʱ�û���ʶ��������");
				logger.warn(e.getMessage());
				Flag=false;     //FlagΪfalse ��Userid�����ڣ�����ִ�к�������
			}
			
			//���Userid����
			if(Flag) 
			{
			aftermodifyuser.setUid(Userid);
			for (Object object : ModifyArray) {   //��json�����������λ�ȡjson����
				JSONObject json=JSONObject.fromObject(object);
				String key=(String) json.get("ModifyInfo");
				String value=(String) json.get("Value");
				if(key.equals("HeadImg"))   //�ж��޸ĵ����������ݿ��ֶ��Ƿ��Ӧ
				{
					//��Base64����ת��ΪͼƬ�ļ��������ڸ�Ŀ¼�µ�img�ļ���
				    //String path =getClass().getResource("/").getPath();
			        //path = path.substring(1, path.indexOf("classes"));   //��ȡ�����ļ�·��
				    String FileHeadImg="img/"+beforemodifyuser.getUname()+".jpg";  //����ͼƬ����·��
				    aftermodifyuser.setHeadimg(FileHeadImg);
				    if(GenerateImage(value,FileHeadImg))  //�ж�ͼƬ�Ƿ񱣴�ɹ�
				    {
				    	aftermodifyuser.setHeadimg(FileHeadImg);
				    }
				    else
				    {
					    json.put("statu", "Image Data Exception");
					    logger.info("ͨ���û���ʶ���޸���Ϣʱ�޸�ͼƬ-ͼƬ�����쳣");
				    }
				}
				else
				{
					Flag=false;
					returnjson.put("statu", "Update Data Not Exits");
					logger.info("ͨ���û���ʶ���޸���Ϣʱ�޸��ֶβ�����");
					break;
				}
			}}
			
			//���Userid���������ݸ�ʽ��ȷ
			if(Flag)
			{
				try
				{
				  userdao.ModificationUserInfo(aftermodifyuser);
				  returnjson.put("RequsetUrl", "");
				  JSONObject UserInfor=new JSONObject();
				  UserInfor.put("UserId", aftermodifyuser.getUid());                       //�û�id
		    	  UserInfor.put("UserName",beforemodifyuser.getUname());                   //�û�����
		    	  UserInfor.put("UserTid",beforemodifyuser.getTid());                      //�û���¼״̬����
		    	  UserInfor.put("HeadImg", aftermodifyuser.getHeadimg());                  //�û���ͷ������
		    	  returnjson.put("statu","true");                                      //ע��-��¼�ɹ����
		    	  returnjson.put("User",UserInfor.toString());	   
				}catch(Exception e)
				{
					returnjson.put("stuta", "Modification User File");
					logger.warn(e.getMessage());
				}	
			}

				return returnjson.toString();	
		}
		
		
		

		
		@Autowired
		CommentDao commentDao;	
		/**
		 *  
		 * ����Ŀ�궯̬ǰ10����̬��Ϣ
		 * @param NewsTime String Ŀ�궯̬����ʱ��
		 * @return String ����10��Ŀ�궯̬��Ϣ��json��ʽ�ַ���
		 * @author ��˼Դ
		 */
		public String GetShare(String NewsTime)
		{
			JSONObject returnjson=new JSONObject();  //���������ݵ�json����
			JSONArray returnArray=new JSONArray();   //������10����̬��json�������
			SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  //����ʱ�ڸ�ʽ  ����2018-03-24 17:37:45
			try
			{
				
				// ����lenientΪfalse. ����SimpleDateFormat��ȽϿ��ɵ���֤���ڣ�����2007/02/29�ᱻ���ܣ���ת����2007/03/01
				format.setLenient(false);
				format.parse(NewsTime);
			} catch (Exception e) 
			 {
					returnjson.put("statu", "ʱ���ʽ�쳣");
					logger.warn("ͨ��ʱ���ȡ�û���̬��Ϣʱ-ʱ���ʽ�쳣");
					logger.warn(e.getMessage());
					return returnjson.toString();
			 } 
			
			//ͨ��NewsTime��Ŀ�궯̬����ʱ�䣩��ѯ������̬����
			Share shareparam=new Share();
			shareparam.setTime(NewsTime);
			List<Share> list_share=sharedao.FindShare1(shareparam);
			
			//��������ڶ�̬��Ϣ������
			if(list_share==null)
			{
				returnjson.put("statu", "���޶�̬��Ϣ");
		         return returnjson.toString(); 
			}
			
			//������̬��Ϣ�����η��뷵�����ݵ�json��������
			for (Share share : list_share) {
				returnjson.put("ShareId", share.getShare_id());
				returnjson.put("UserId", share.getUser_id());
				returnjson.put("Text", share.getTxt());
				returnjson.put("Time", share.getTime());
				returnjson.put("LikeCount", share.getLike_count());
				String Image_url=share.getImage_url();
				String Image_urllist[]=null;
				if(Image_url!=null)
				{Image_urllist=Image_url.split("\\|");
				 JSONArray imgjsonarray=new JSONArray();
				 for (String image_url : Image_urllist) {
					JSONObject imgjsonobject=new JSONObject();
					imgjsonobject.put("ImageUrl", image_url);
					imgjsonarray.add(imgjsonobject);
				}
				 returnjson.put("Images", imgjsonarray);
				}
				
				
				//ͨ���õ���Share_id����̬��������ѯ�ö�̬�µ���������������CommentId��
				List<Integer> listCommentid=commentDao.GetCommentId(share.getShare_id());
                if(listCommentid!=null)  //��ѯ���Ϊ�� ���ö�̬�²���������
                {
                	JSONArray commentidArray=new JSONArray();  //����������id��json����
                	for (Integer commentid : listCommentid) {
						JSONObject commentidObject=new JSONObject();
						commentidObject.put("CommentId", commentid);
						commentidArray.add(commentidObject);
					}
                	returnjson.put("Comments", commentidArray);
                }  
                returnArray.add(returnjson);
			}
			return returnArray.toString();
			
		}
		
		public String GetShare(int UserId,String NewsTime)
		{
			JSONObject returnjson=new JSONObject();
			JSONArray returnArray=new JSONArray();
			SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				// ����lenientΪfalse. ����SimpleDateFormat��ȽϿ��ɵ���֤���ڣ�����2007/02/29�ᱻ���ܣ���ת����2007/03/01
				           format.setLenient(false);
				           format.parse(NewsTime);
				        } catch (Exception e) {
				        	returnjson.put("statu", "ʱ���ʽ�쳣");
		         return returnjson.toString();
				        } 
			
			
			
			//ͨ��NewsTime��Ŀ�궯̬����ʱ�䣩��ѯ������̬����
			Share shareparam=new Share();
			shareparam.setTime(NewsTime);
			shareparam.setUser_id(UserId);
			List<Share> list_share=sharedao.FindShare1(shareparam);
			if(list_share==null)
			{
				returnjson.put("statu", "���޶�̬��Ϣ");
		         return returnjson.toString(); 
			}
			for (Share share : list_share) {
				returnjson.put("ShareId", share.getShare_id());
				returnjson.put("UserId", share.getUser_id());
				returnjson.put("Text", share.getTxt());
				returnjson.put("Time", share.getTime());
				returnjson.put("LikeCount", share.getLike_count());
				List<Integer> listCommentid=commentDao.GetCommentId(share.getShare_id());
                if(listCommentid!=null)
                {
                	JSONArray commentidArray=new JSONArray();
                	for (Integer commentid : listCommentid) {
						JSONObject commentidObject=new JSONObject();
						commentidObject.put("CommentId", commentid);
						commentidArray.add(commentidObject);
					}
                	returnjson.put("Comments", commentidArray);
                }  
                returnArray.add(returnjson);
			}
			return returnArray.toString();
			
		}
		
		
		
		
			
		/**
		 * 
		 * 
		 * ͨ����̬��ʶ����ShareId����ȡ��Ӧ��������Ϣ
		 * @param ShareId  int Ŀ�궯̬��ʶ��
		 * @return String Ŀ�궯̬�������������ݵ�json��ʽ�ַ���
		 * @author ��˼Դ
		 */
		public String GetComment(int ShareId)
		{
			JSONObject returnJsonObject=new JSONObject();
			SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  //���÷���ʱ���ʽ
			
			//ͨ������������ѯ�������۶���
			List<Comment> commentlist=null;
			try
			{
			commentlist=commentDao.GetCommentByShareId(ShareId) ;
			System.out.println(commentlist);
			}catch(Exception e)
			{
				
			}
			if(commentlist.size()<=0)  //��������Ϊ�� �������ڸ���������
			{
				returnJsonObject.put("statu","�ö�̬û������");
				return returnJsonObject.toString();
			}
			
			
			//�������۾�����Ϣ
			JSONArray comments=new JSONArray();
			for (Comment comment : commentlist) {
				JSONObject commentJsonObject=new JSONObject(); 
				commentJsonObject.put("CommentId",comment.getCommunicated_id());
				commentJsonObject.put("Time",comment.getTime());
				commentJsonObject.put("Text", comment.getTxt());
				commentJsonObject.put("UserId", comment.getUid());
				comments.add(commentJsonObject);
			}
			returnJsonObject.put("statu","true" );
			returnJsonObject.put("Comments",comments );
			return returnJsonObject.toString();
			
		}
		
		
		
		
		
		/**
		 * 
		 * ͨ���û���ʶ����ȡ�û���Ϣ
		 * @param UserId String �û���ʶ��
		 * @return String �����û���ػ�����Ϣ��json��ʽ�ַ���
		 * @author ��˼Դ 
		 */
		public String GetUserInfoByUserId(int UserId)
		{
			JSONObject returnjson=new JSONObject();
			try
			{
				User user=userdao.FindUserByUserId(UserId);
				returnjson.put("UserId", user.getUid());
				returnjson.put("UserName", user.getUname());
				returnjson.put("HeadImg", user.getHeadimg());
				returnjson.put("StudentId", user.getSid());
				returnjson.put("Email", user.getEmail());
				returnjson.put("Group", user.getGroup());
				return returnjson.toString();
			}catch(Exception e)
			{
				returnjson.put("statu", "���û���������");
				logger.warn("ͨ���û���ʶ����ȡ�û���Ϣʱ-�û���ʶ��������");
				logger.warn(e.getMessage());
				return returnjson.toString();	
			}		
		}
		
		
		
		
		
		/**
		 * 
		 * ������̬��Ϣ
		 * @param ShareTime  String ��̬����ʱ��
		 * @param ShareText  String ��̬����
		 * @param ShareUserId  int �����˱�ʶ��
		 * @return String �����ɹ�����json��ʽ�ַ���
		 */
		public String InsertShareInfo(String ShareTime,String ShareText,int ShareUserId)
		{
			JSONObject returnJson=new JSONObject();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
			Date ShareDate;
			    try {
					ShareDate=sdf.parse(ShareTime);
				} catch (ParseException e) {
					returnJson.put("statu", "ʱ���ʽ�쳣");
					logger.warn("���붯̬������Ϣʱ-ʱ���ʽ����");
					logger.warn(e.getMessage());
					return returnJson.toString();
				}
			Share share=new Share();
			share.setUser_id(ShareUserId);
			share.setTime(ShareTime);
			share.setTxt(ShareText);
			try
			{
			sharedao.InsertShare(share);
			returnJson.put("statu", "true");
			return returnJson.toString();
			}catch(Exception e)
			{
				returnJson.put("statu", "������Ϣ�����쳣");
				logger.warn("���붯̬������Ϣʱ-������Ϣ�����쳣");
				logger.warn(e.getMessage());
				return returnJson.toString();
			}

		}
		
		
		

		
		//ѧ�Ű�
		public String StudentIDBinding(String StudentID,String UserName)
		{
			JSONObject returnjson=new JSONObject();
			
			try 
			{
			User user=userdao.FindUserByUname(UserName);
			  try
			  {
			     userdao.StudentIDBinding(StudentID, UserName);
			     JSONObject UserInfor=new JSONObject();
				 UserInfor.put("UserId", user.getUid());                       //��json�м���-���ڻ�ȡ�û���Ϣ������
			     UserInfor.put("StudentID",StudentID);                         //�û���ע����
			     returnjson.put("User",UserInfor.toString());	   
			     return returnjson.toString();
			  }catch(Exception e)
			  {
				  returnjson.put("statu", "StudentIDBinding Fail"); 
				  return  returnjson.toString();
			  }
			}catch(Exception e)
			{
				returnjson.put("statu", "UserName Not Exits");
				return  returnjson.toString();
			}
			
		}
		
		
		
		/**
		 * 
		 * ��base64��ʽ��ͼƬ����
		 * @param imgStr String base63�����ַ���
		 * @param imgFilePath  String ͼƬ����·��
		 * @return Boolean ת���ɹ����
		 */
        public  boolean GenerateImage(String imgStr, String imgFilePath) {// ���ֽ������ַ�������Base64���벢����ͼƬ 
          if (imgStr == null) // ͼ������Ϊ�� 
            return false; 
          System.out.println(1);
          BASE64Decoder decoder = new BASE64Decoder();       
          System.out.println(2);
          try { 
          // Base64���� 
          byte[] bytes = decoder.decodeBuffer(imgStr); 
          for (int i = 0; i < bytes.length; ++i) { 
          if (bytes[i] < 0) {// �����쳣���� 
          bytes[i] += 256; 
          } 
          } 
          System.out.println(3);
          // ����jpegͼƬ 
          OutputStream out = new FileOutputStream(imgFilePath);        
          out.write(bytes); 
          System.out.println(5);
          out.flush(); 
          out.close(); 
          return true; 
          } catch (Exception e) { 
        	  logger.warn(e.getMessage());
        	  return false; 
          } 
          }


          }
