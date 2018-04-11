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
	    * 用户登录操作的方法
	    * 
	    * @param UserName String 用户名  
	    * @param UserPwd String 用户密码
	    * @param response HttpServletResponse
	    * @return String 返回参数为json数据格式的字符串形式  
	    * @author 陈思源
	    */
		public String login(String UserName,String UserPwd,HttpServletResponse response)          
		{
			JSONObject json=new JSONObject();                              //创建json对象
		    JSONObject UserInfor=new JSONObject();
			try {
			       User user=userdao.FindUserByUname(UserName);	             //通过用户名查找用户信息 如果没有 将进行catch异常信息处理
			       if(user.getPwd()==UserPwd||user.getPwd().equals(UserPwd))
				    {
			    	 //向json中加入-用于获取用户信息的链接
			    	  UserInfor.put("UserId", user.getUid());
			    	  UserInfor.put("UserLink", "");                       
			    	  UserInfor.put("UserName",UserName);                   //用户的注册名
			    	  UserInfor.put("Token", "");                          //用户登录状态描述
			    	  UserInfor.put("headimg", "");                        //用户的头像链接
			    	  json.put("statu","true");                            //注册-登录成功与否
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
				      logger.info("登录时密码错误");
			        }
				}catch(Exception e)
				  {
					  json.put("statu","UserName Not Found");  
					  logger.warn("登录时用户名不存在");
					  logger.warn(e.getMessage());
				  }
			return json.toString();
		}
		
		
		

			
		/**
		 * 
		 * 注册用户信息操作的方法
		 * 
		 * @param UserName String 用户用户名
		 * @param UserPwd  String 用户密码
		 * @return String json数据格式的字符串形式  
		 * @author 陈思源
		 */
		public String Register(String UserName,String UserPwd,String email,String CAPTCHA)
		{
			
			boolean Flag=false;            //验证码能否匹配  true为匹配 false为不匹配
			JSONObject returnjson=new JSONObject();                       //创建返回的json对象
		    JSONObject UserInfor=new JSONObject();
		    
		    //邮箱格式验证
		    String regex="[a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9]+(\\.(com|cn|org|edu|hk))";
			if(!email.matches(regex))
			{
				returnjson.put("statu", "Email Format Wrong");
				logger.info("注册时邮箱格式错误");
				return returnjson.toString();
			}
			
			//定义User对象及属性设置
			  User user=new User();
			  user.setUname(UserName);
			  user.setPwd(UserPwd); 
			  user.setEmail(email);
			  
			  //使得用户名和密码不为空
			  if(UserName==null||UserName==""||UserPwd==null||UserPwd=="")
			  {
				  returnjson.put("statu", "UserName Or UserPwd Is Null");
				  logger.info("注册时用户名或密码为空");
				  return returnjson.toString();
			  }
			  
			  //判断用户名是否存在
			  try
			  {
			    User s=userdao.FindUserByUname(UserName);   //通过用户名查询用户                  
			    if(s.getUname()!=null)
			    {
			    	returnjson.put("statu","UserName Exist");
			    	logger.info("注册时用户名已经存在");
			    	return returnjson.toString();
			    }
			    else
			    	throw new Exception();
			  }catch(Exception e)
			  {
				  logger.warn(e.getMessage());
			  }
			  
			  //验证码验证
			  if(CAPTCHA==null||CAPTCHA==""||CAPTCHA.equals(""))
			  {
				  returnjson.put("statu","验证码为空，注册失败");
				  logger.info("注册时未输入验证码");
			  }
			  else
				  if(Timer1.map.containsKey(email))
					{
						if(CAPTCHA.equals(Timer1.map.get(email))||CAPTCHA==Timer1.map.get(email))
							Flag=true;
						else
							{
							returnjson.put("statu","验证码不正确，请重新输入");
							logger.info("注册时验证码错误");
							}        
					}
					else if(Timer1.cachemap.containsKey(email))
					{
						if(CAPTCHA.equals(Timer1.cachemap.get(email))||CAPTCHA==Timer1.cachemap.get(email))
							Flag=true;
						else
						{
							returnjson.put("statu","验证码不正确，请重新输入");
							logger.info("注册时验证码错误");
						}
							
					}
					else 
						returnjson.put("statu","验证码已失效，请重新注册");
			  
			  //用户注册
			  if(Flag)        //验证码匹配成功
			  {
				  userdao.InsertUser(user);                            //用户注册
				  UserInfor.put("UserId",user.getUid());
				  UserInfor.put("UserLink", "");                       //向json中加入-用于获取用户信息的链接
				  UserInfor.put("UserName",UserName);                   //用户的注册名
				  UserInfor.put("Token", "");                          //用户登录状态描述
				  UserInfor.put("headimg", "");                        //用户的头像链接
				  returnjson.put("statu","true");                            //注册-登录成功与否
				  returnjson.put("User",UserInfor.toString());
			  }
 
			  return returnjson.toString();
		}
		
		
		

		
		@Autowired
		private JavaMailSenderImpl mailSender;	
		/**
		 * 
		 * 发送邮箱验证并保存验证码
		 * 
		 * @param UserName String 用户名
		 * @param Email String 目标邮箱
		 * @return String json数据格式的字符串形式  
		 * @author 陈思源
		 */
		public String EmailSend(String UserName,String Email)
		{
			JSONObject returnjson=new JSONObject();
			try
			  {
			    User s=userdao.FindUserByUname(UserName);                     //判断用户名是否存在
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
			
			//邮箱数据格式验证
			String regex="[a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9]+(\\.(com|cn|org|edu|hk))";
			if(!Email.matches(regex))
			{
				returnjson.put("statu", "Email Format Wrong");
				logger.info("注册阶段邮箱验证时邮箱格式错误");
				return returnjson.toString();
			}
		
			//if(userdao.FindUserEmail(Email)<0)
			//{
			//	return "该邮箱已被注册,请换个邮箱进行验证";
			//}
			try
			{
				//邮箱验证
				SimpleMailMessage mailMessage = new SimpleMailMessage();   
				mailMessage.setTo(Email);
				mailMessage.setFrom("1441565921@qq.com");    
				mailMessage.setSubject("在途科技APP验证码验证"); 
				int code=new Random().nextInt(100000);
				mailMessage.setText("您现在正在进行在途科技APP用户注册信息邮箱验证 。您的验证码为："+code+"。   如果没有进行注册操作，不予理会即可。"); 
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
				returnjson.put("statu", "邮箱发送失败，请确认邮箱是否开通服务和邮箱的正确性");
				logger.warn("注册阶段邮箱验证时邮箱发送失败");
				logger.warn(e.getMessage());
				return returnjson.toString();	
			}
		}
		
		
		
		
				
		/**
		 * 
		 * 修改用户信息 
		 * @param  Userid int 用户id
		 * @param  ModifyArray JSONArray 需要被修改的保存在json数组里的信息
		 * @return String json数据格式的字符串形式  
		 * @author 陈思源
		 *
		 */
		public String Modify(int Userid,JSONArray ModifyArray)
		{
			JSONObject returnjson=new JSONObject();              //定义返回的json数据
			boolean Flag=true;
			//定义修改前后user对象信息
			User beforemodifyuser = new User();  //修改前user
			User aftermodifyuser=new User();    //修改后user
			
			//通过id查询初始化修改前user数据
			try {
			beforemodifyuser=userdao.FindUserByUserId(Userid);  
			}catch(Exception e)
			{
				returnjson.put("statu", "UserId Not Exits");
				logger.warn("通过用户标识符修改信息时用户标识符不存在");
				logger.warn(e.getMessage());
				Flag=false;     //Flag为false 即Userid不存在，将不执行后续操作
			}
			
			//如果Userid存在
			if(Flag) 
			{
			aftermodifyuser.setUid(Userid);
			for (Object object : ModifyArray) {   //从json数组里面依次获取json对象
				JSONObject json=JSONObject.fromObject(object);
				String key=(String) json.get("ModifyInfo");
				String value=(String) json.get("Value");
				if(key.equals("HeadImg"))   //判断修改的数据与数据库字段是否对应
				{
					//将Base64编码转换为图片文件并保存在根目录下的img文件中
				    //String path =getClass().getResource("/").getPath();
			        //path = path.substring(1, path.indexOf("classes"));   //获取保存文件路径
				    String FileHeadImg="img/"+beforemodifyuser.getUname()+".jpg";  //定义图片保存路径
				    aftermodifyuser.setHeadimg(FileHeadImg);
				    if(GenerateImage(value,FileHeadImg))  //判断图片是否保存成功
				    {
				    	aftermodifyuser.setHeadimg(FileHeadImg);
				    }
				    else
				    {
					    json.put("statu", "Image Data Exception");
					    logger.info("通过用户标识符修改信息时修改图片-图片数据异常");
				    }
				}
				else
				{
					Flag=false;
					returnjson.put("statu", "Update Data Not Exits");
					logger.info("通过用户标识符修改信息时修改字段不存在");
					break;
				}
			}}
			
			//如果Userid存在且数据格式正确
			if(Flag)
			{
				try
				{
				  userdao.ModificationUserInfo(aftermodifyuser);
				  returnjson.put("RequsetUrl", "");
				  JSONObject UserInfor=new JSONObject();
				  UserInfor.put("UserId", aftermodifyuser.getUid());                       //用户id
		    	  UserInfor.put("UserName",beforemodifyuser.getUname());                   //用户姓名
		    	  UserInfor.put("UserTid",beforemodifyuser.getTid());                      //用户登录状态描述
		    	  UserInfor.put("HeadImg", aftermodifyuser.getHeadimg());                  //用户的头像链接
		    	  returnjson.put("statu","true");                                      //注册-登录成功与否
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
		 * 返回目标动态前10条动态信息
		 * @param NewsTime String 目标动态发布时间
		 * @return String 包含10条目标动态信息的json格式字符串
		 * @author 陈思源
		 */
		public String GetShare(String NewsTime)
		{
			JSONObject returnjson=new JSONObject();  //定义存放数据的json对象
			JSONArray returnArray=new JSONArray();   //定义存放10条动态的json数组对象
			SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  //设置时期格式  例：2018-03-24 17:37:45
			try
			{
				
				// 设置lenient为false. 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
				format.setLenient(false);
				format.parse(NewsTime);
			} catch (Exception e) 
			 {
					returnjson.put("statu", "时间格式异常");
					logger.warn("通过时间获取用户动态信息时-时间格式异常");
					logger.warn(e.getMessage());
					return returnjson.toString();
			 } 
			
			//通过NewsTime（目标动态发布时间）查询多条动态对象
			Share shareparam=new Share();
			shareparam.setTime(NewsTime);
			List<Share> list_share=sharedao.FindShare1(shareparam);
			
			//如果不存在动态信息，返回
			if(list_share==null)
			{
				returnjson.put("statu", "已无动态信息");
		         return returnjson.toString(); 
			}
			
			//遍历动态信息，依次放入返回数据的json数组里面
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
				
				
				//通过得到的Share_id（动态主键）查询该动态下的所有评论主键（CommentId）
				List<Integer> listCommentid=commentDao.GetCommentId(share.getShare_id());
                if(listCommentid!=null)  //查询结果为空 即该动态下不存在评论
                {
                	JSONArray commentidArray=new JSONArray();  //定义存放评论id的json数组
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
				// 设置lenient为false. 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
				           format.setLenient(false);
				           format.parse(NewsTime);
				        } catch (Exception e) {
				        	returnjson.put("statu", "时间格式异常");
		         return returnjson.toString();
				        } 
			
			
			
			//通过NewsTime（目标动态发布时间）查询多条动态对象
			Share shareparam=new Share();
			shareparam.setTime(NewsTime);
			shareparam.setUser_id(UserId);
			List<Share> list_share=sharedao.FindShare1(shareparam);
			if(list_share==null)
			{
				returnjson.put("statu", "已无动态信息");
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
		 * 通过动态标识符（ShareId）获取对应的评论信息
		 * @param ShareId  int 目标动态标识符
		 * @return String 目标动态的所有评论数据的json格式字符串
		 * @author 陈思源
		 */
		public String GetComment(int ShareId)
		{
			JSONObject returnJsonObject=new JSONObject();
			SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  //设置返回时间格式
			
			//通过评论主键查询返回评论对象
			List<Comment> commentlist=null;
			try
			{
			commentlist=commentDao.GetCommentByShareId(ShareId) ;
			System.out.println(commentlist);
			}catch(Exception e)
			{
				
			}
			if(commentlist.size()<=0)  //返回评论为空 即不存在该评论主键
			{
				returnJsonObject.put("statu","该动态没有评论");
				return returnJsonObject.toString();
			}
			
			
			//返回评论具体信息
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
		 * 通过用户标识符获取用户信息
		 * @param UserId String 用户标识符
		 * @return String 包含用户相关基础信息的json格式字符串
		 * @author 陈思源 
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
				returnjson.put("statu", "该用户名不存在");
				logger.warn("通过用户标识符获取用户信息时-用户标识符不存在");
				logger.warn(e.getMessage());
				return returnjson.toString();	
			}		
		}
		
		
		
		
		
		/**
		 * 
		 * 发布动态信息
		 * @param ShareTime  String 动态发布时间
		 * @param ShareText  String 动态内容
		 * @param ShareUserId  int 发布人标识符
		 * @return String 发布成功与否的json格式字符串
		 */
		public String InsertShareInfo(String ShareTime,String ShareText,int ShareUserId)
		{
			JSONObject returnJson=new JSONObject();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
			Date ShareDate;
			    try {
					ShareDate=sdf.parse(ShareTime);
				} catch (ParseException e) {
					returnJson.put("statu", "时间格式异常");
					logger.warn("插入动态发布信息时-时间格式错误");
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
				returnJson.put("statu", "发布信息数据异常");
				logger.warn("插入动态发布信息时-发布信息数据异常");
				logger.warn(e.getMessage());
				return returnJson.toString();
			}

		}
		
		
		

		
		//学号绑定
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
				 UserInfor.put("UserId", user.getUid());                       //向json中加入-用于获取用户信息的链接
			     UserInfor.put("StudentID",StudentID);                         //用户的注册名
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
		 * 将base64格式的图片解码
		 * @param imgStr String base63编码字符串
		 * @param imgFilePath  String 图片保存路径
		 * @return Boolean 转换成功与否
		 */
        public  boolean GenerateImage(String imgStr, String imgFilePath) {// 对字节数组字符串进行Base64解码并生成图片 
          if (imgStr == null) // 图像数据为空 
            return false; 
          System.out.println(1);
          BASE64Decoder decoder = new BASE64Decoder();       
          System.out.println(2);
          try { 
          // Base64解码 
          byte[] bytes = decoder.decodeBuffer(imgStr); 
          for (int i = 0; i < bytes.length; ++i) { 
          if (bytes[i] < 0) {// 调整异常数据 
          bytes[i] += 256; 
          } 
          } 
          System.out.println(3);
          // 生成jpeg图片 
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
