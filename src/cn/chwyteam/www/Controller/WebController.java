package cn.chwyteam.www.Controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;

import com.sun.mail.util.MailSSLSocketFactory;

import Interceptor.PathInterceptor;
import cn.chwyteam.www.Controller.DaoImpl.User.UserControllerDaoImpl;
import cn.chwyteam.www.Share.Dao.ShareDao;
import cn.chwyteam.www.Share.Pojo.Share;
import cn.chwyteam.www.Share.Pojo.ShareJL;
import cn.chwyteam.www.User.Dao.UserDao;
import cn.chwyteam.www.User.Pojo.User;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Controller
@RequestMapping("/web")
public class WebController {

	@Autowired
	UserDao userdao;

	@Autowired
	ShareDao sharedao;

	@Autowired
	UserControllerDaoImpl UCDI;

	private static final Log logger = LogFactory.getLog(WebController.class);

	/**
	 * 
	 * 用户请求响应 进行用户登录或用户注册
	 */
	@RequestMapping("/UserRequest")
	public void LoginOrRegisterUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try // 用try包裹 避免Request请求的数据格式不是json而报出的异常
		{
			String Request = request.getParameter("Request"); // 获取请求信息
			// 获取传入参数基本信息
			JSONObject jsonrequest = JSONObject.fromObject(Request);
			String RequestType = jsonrequest.getString("RequestType"); // 获取请求类型
			String LoginType = jsonrequest.getString("LoginType"); // 获取登录类型
			String RequestTime = jsonrequest.getString("RequestTime"); // 获取请求时间
			String User = jsonrequest.getString("User"); // 获取用户信息
			logger.info("请求路径：UserRequest-" + RequestType);
			JSONObject json = JSONObject.fromObject(User); // 将用户信息User转化成json数据格式
			String UserName = (String) json.get("UserName");
			String UserPwd = json.getString("UserPwd");
			response.setContentType("text/html;charset=GBK");

			// 请求类型是否为-登录验证
			if (RequestType == "Login" || RequestType.equals("Login")) {
				response.getWriter().println(UCDI.login(UserName, UserPwd, response)); // 向客户端传递用户相关信息
				logger.info("请求登录用户信息--Success");
			}

			// 请求类型是否为-注册功能
			else if (RequestType == "Register" || RequestType.equals("Register")) {
				String email = json.getString("Email");
				String CAPTCHA = json.getString("CAPTCHA");
				response.getWriter().println(UCDI.Register(UserName, UserPwd, email, CAPTCHA)); // 返回注册相关信息
				logger.info("请求注册用户信息--Success");
			}
		} catch (Exception e) {
			JSONObject jsonerror = new JSONObject();
			jsonerror.put("statu", "ReqestDate Exception");
			response.getWriter().println(jsonerror.toString());
			logger.warn(e.getMessage());
			logger.warn("请求登录或注册用户信息--Fail");
		}
	}

	/**
	 * 
	 * 注册时的邮箱验证 向用户邮箱发送验证码
	 */
	@RequestMapping("/SendEmail")
	public void SendEmail(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String Request = request.getParameter("Request"); // 获取请求信息
			logger.info("请求数据：" + Request);
			JSONObject jsonrequest = JSONObject.fromObject(Request); // 将请求信息转换为json
			String UserName = jsonrequest.getString("UserName");
			String Email = jsonrequest.getString("Email");
			response.getWriter().println(UCDI.EmailSend(UserName, Email)); // 向用户邮箱发送验证码
			logger.info("请求向目标邮箱" + Email + "发送邮件--Success");
		} catch (Exception e) {
			JSONObject jsonerror = new JSONObject();
			jsonerror.put("statu", "Request Exeception");
			response.getWriter().println(jsonerror.toString());
			logger.warn("请求向目标邮箱发送邮件--Fail");
			logger.warn(e.getMessage());
		}
	}

	/**
	 * 
	 * 通过用户id进行信息修改
	 */
	@RequestMapping(value = "/UserModification/{Userid}", method = RequestMethod.POST)
	public void ModificationUser(@PathVariable("Userid") int Userid, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			String Request = request.getParameter("Request"); // 获取请求信息
			logger.info(Request);
			JSONObject jsonRequest = JSONObject.fromObject(Request);

			String ModifyQuery = jsonRequest.getString("ModifyQuery"); // 获取请求修改的信息（jsonarray数据格式）
			logger.info(ModifyQuery);
			JSONArray ModifyArray = JSONArray.fromObject(ModifyQuery);
			response.getWriter().println(UCDI.Modify(Userid, ModifyArray));
			logger.info("请求通过用户id修改相关信息--Success");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.warn(e.getMessage());
			try {
				response.getWriter().println("{'statu':'RequestData Exception'}");
				logger.info("请求通过用户id修改相关信息--Fail");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				logger.warn(e.getMessage());
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * 获取动态信息 通过是否接受到用户ID 来区分请求公有动态和请求私有动态
	 */
	@RequestMapping("/GetShare")
	public void GetShare(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=GBK"); // 设置回传信息格式，避免参数乱码

		// 捕获请求参数数据格式异常
		try {
			String Request = request.getParameter("Request");
			JSONObject getjson = JSONObject.fromObject(Request); // 将参数转化为json格式
			String RequestTime = getjson.getString("RequestTime"); // 获取请求访问时间
			String NewsTime = getjson.getString("NewsTime"); // 获取目标动态发布时间

			if (getjson.has("UserId")) {
				int UserId = getjson.getInt("UserId");
				response.getWriter().println(UCDI.GetShare(UserId, NewsTime));
				logger.info("请求通过用户id获得相关10条动态信息--Success");
			} else {

				// 返回时间在NewsTime（目标动态时间）发布之前的10条动态
				response.getWriter().println(UCDI.GetShare(NewsTime));
				logger.info("请求获得公共动态信息--Success");
			}
		} catch (Exception e) {
			JSONObject returnJson = new JSONObject();
			returnJson.put("statu", "请求数据格式异常");
			response.getWriter().println(returnJson.toString());
			logger.warn("请求获得有关动态信息--Fail");
			logger.warn(e.getMessage());
		}

	}

	/**
	 * 
	 * 响应目标动态‘评论’的信息 通过获取评论id（CommentId）来获取评论信息 并向客户端返回评论Id，评论时间，评论文本，评论发出人id等信息
	 */
	@RequestMapping("/GetComment")
	public void GetComment(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=GBK"); // 设置回传信息格式，避免参数乱码
		// 捕获请求参数数据格式异常
		try {
			String Request = request.getParameter("Request");
			JSONObject getJson = JSONObject.fromObject(Request); // 将参数转化为json格式
			String RequestTime = getJson.getString("RequestTime"); // 获取请求访问时间
			int ShareId = getJson.getInt("ShareId"); // 获取评论Id
			// 通过评论id查询返回该评论的具体信息
			response.getWriter().println(UCDI.GetComment(ShareId));
			logger.info("请求获得某条动态信息的评论--Success");
		} catch (Exception e) {
			JSONObject returnJson = new JSONObject();
			returnJson.put("statu", "请求数据格式异常");
			response.getWriter().println(returnJson.toString());
			logger.warn("请求获得某条动态信息的评论--Fail");
			logger.warn(e.getMessage());
		}
	}

	/**
	 * 
	 * 通过UserId获取User全部信息 通过 占位符 访问 更直接更方便
	 */
	@RequestMapping(value = "/GetUserInfo/{UserId}", method = RequestMethod.GET)
	public void GetUserInfo(@PathVariable("UserId") int UserId, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=GBK"); // 设置回传信息格式，避免参数乱码
		response.getWriter().println(UCDI.GetUserInfoByUserId(UserId));
	}

	/**
	 * 
	 * 动态分享
	 */
	@RequestMapping("/PublicShare")
	public void PublicShare(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=GBK"); // 设置回传信息格式，避免参数乱码
		try {
			String Request = request.getParameter("Request");
			JSONObject getJson = JSONObject.fromObject(Request);
			String RequestTime = getJson.getString("RequestTime");
			String ShareTime = getJson.getString("ShareTime");
			String ShareText = getJson.getString("ShareText");
			int ShareUserId = getJson.getInt("ShareUserId");
			response.getWriter().println(UCDI.InsertShareInfo(ShareTime, ShareText, ShareUserId));
		} catch (Exception e) {
			JSONObject returnJson = new JSONObject();
			returnJson.put("statu", "请求数据格式异常");
			response.getWriter().println(returnJson.toString());
			logger.warn("请求发布动态时参数异常-Fail");
			logger.warn(e.getMessage());
		}

	}

	// 学号绑定
	@RequestMapping("/StudentIDBinding")
	public void StudentIDBinding(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String Request = request.getParameter("Request");
		JSONObject jsonRequest = JSONObject.fromObject(Request);
		String StudentID = jsonRequest.getString("StudentID");
		String UserName = jsonRequest.getString("UserName");
		response.getWriter().println(UCDI.StudentIDBinding(StudentID, UserName));
	}

	@Autowired
	private JavaMailSenderImpl mailSender;

	@RequestMapping(value = "/sendEmail")
	public void sendEmail() throws GeneralSecurityException {
		SimpleMailMessage mailMessage = new SimpleMailMessage();

		MailSSLSocketFactory sf = new MailSSLSocketFactory();

		mailMessage.setTo("1638192316@qq.com");
		mailMessage.setFrom("1441565921@qq.com");
		mailMessage.setSubject("welcome");
		mailMessage.setText("333");

		System.out.println(1);

		mailSender.send(mailMessage);
		System.out.println(2);

	}

	public static void main(String[] args) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.qq.com");
		mailSender.setPort(465);
		mailSender.setUsername("1441565921@qq.com");
		mailSender.setPassword("zzfxgncpjlioiehd");// 授权码
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setTo("1638192316@qq.com");
		mail.setFrom("1441565921@qq.com");
		mail.setSubject("test mail");
		mail.setText("test mail content");
		System.out.println(1);
		mailSender.send(mail);
		System.out.println("success");
	}

	@RequestMapping("/insert")
	public String getinform(HttpServletRequest request, HttpServletResponse response, Map<String, String> map) {
		// String param=request.getParameterValues(arg0);
		String name = map.get("UserName");
		String pwd = map.get("UserPwd");
		if (name == null || name == "" || pwd == null || pwd == "") {
			request.getSession().setAttribute("infor", "用户名或密码为空");
			return "error";
		}
		if (name != null && pwd != null) {
			User user = new User();
			user.setUname(name);
			user.setPwd(pwd);
			userdao.InsertUser(user);
			return "success";
		} else
			return "error";
	}

	@RequestMapping("/select")
	public String FindUserByUname(HttpServletRequest request, HttpServletResponse response) {

		String name = request.getParameter("UserName");
		System.out.println(name);
		String pwd = request.getParameter("UserPwd");
		if (name == null || name == "" || pwd == null || pwd == "") {
			request.getSession().setAttribute("infor", "用户名或密码为空");
			return "error";
		}
		// JSONObject json=new JSONObject();
		try {
			User user = userdao.FindUserByUname(name);
			System.out.println(user.getPwd());
			response.setContentType("text/html;charset=GBK");
			if (user.getPwd() == pwd || user.getPwd().equals(pwd)) {
				// json.put("userId", user.getUid());
				// response.getWriter().println(json.toString());
				System.out.println(user.getUid());
				request.getSession().setAttribute("username", user.getUname());
				return "main";
			} else {
				request.getSession().setAttribute("infor", "密码错误");
				return "error";
			}
		} catch (Exception e) {
			System.out.println("用户名不存在");
			request.getSession().setAttribute("infor", "用户名不存在");
			return "error";
		}

	}

	@RequestMapping("/Modification")
	public String ModifyPwd(HttpServletRequest request, HttpServletResponse response) {
		String UserName = request.getParameter("UserName");
		String OldPwd = request.getParameter("OldPwd");
		String NewPwd = request.getParameter("NewPwd");
		try {
			// userdao.Modification(UserName, OldPwd, NewPwd);
			request.getSession().setAttribute("username", null);

		} catch (Exception e) {
			request.getSession().setAttribute("infor", "用户名或密码错误");
			return "error";
		}
		return "main";
	}

	@RequestMapping(value = "/test")
	public void ss(HttpServletRequest request, HttpServletResponse response) {

		String text = "\\\\\\/9j\\\\\\/4QCCRXhpZgAATU0AKgAAAAgABAEAAAMAAAABAfQAAAEBAAMAAAABAfQAAIdpAAQAAAABAAAA\\\\nPgESAAMAAAABAAAAAAAAAAAAAZIIAAQAAAABAAAAAAAAAAAAAwEAAAMAAAABAfQAAAEBAAMAAAAB\\\\nAfQAAAESAAMAAAABAAAAAAAAAAD\\\\\\/4AAQSkZJRgABAQAAAQABAAD\\\\\\/2wBDAAMCAgMCAgMDAwMEAwME\\\\nBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT\\\\\\/2wBDAQME\\\\nBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQU\\\\nFBQUFBT\\\\\\/wAARCAH0AfQDASIAAhEBAxEB\\\\\\/8QAHQAAAAcBAQEAAAAAAAAAAAAAAAECAwQFBgcICf\\\\\\/E\\\\nAFQQAAEDAwIDBAcEBgYHBAkFAQEAAgMEBRESIQYxQQcTUWEIFCJxgZGhMkKxwSNSYnKS0RUzQ4Ki\\\\n4QkWJFOywvAXJTRjGERFVFVkc4OEJjWTo9Lx\\\\\\/8QAGgEAAwEBAQEAAAAAAAAAAAAAAAECAwQFBv\\\\\\/E\\\\nADARAQEAAgEEAQMDAwMEAwAAAAABAhEDEiExQVEEBSITMmFCcZEGFaEUQ4HRI1Pw\\\\\\/9oADAMBAAIR\\\\nAxEAPwD3XgpQbslhpwjDcL2Hz\\\\\\/kgNSuRwl48EelCiBlKwjDSUYGEAnGCjIwhjdHhLRibsE6x\\\\\\/wAk\\\\n0coxsEaOXSQHNJ5ozjmFFB8EDMQd1PSvqSCAU2YwU36xj3ojUgImNPqgzCwnomJYW42wluqRzTEs\\\\n2rYZVyVllcTEjQ1R3+ynnDJTcgORgZytZ2c1u3PO0u5F8tvtbDkOd6zP+637I+LvwWUPNWXFFWaz\\\\nia4ScxG\\\\\\/1dvkGbH\\\\\\/ABZVa5c2V3X2X0HH+lwY\\\\\\/wA9xJMn2UHPDThNueXDCl6e2A7UryIoaa2Rn25D\\\\n30nk0cvr+Cm9mlvkgtb62ZxJm9iNp5NYD+ZWB4orXX3iyqEeXF04pYh44OkY95yuv0UcNqt8FMHB\\\\nrYWBmTtnHVL2FkHlKEmFUy3ukg+1UM92cqO\\\\\\/iqhbykc791qo4v8AvM+CPvAPes07jKkH3JTjyA\\\\\\/N\\\\nNu41pwCRBJn3hLZ6agyou93wsk\\\\\\/jdv3af5uTLuNZDyiYPiUbGq2mtV96rXU1vk7r+vlxFEP2nbBZ\\\\nd3GdRjZrB8FU1\\\\\\/FVXVXOkcHtDaYOkLQNi47N\\\\\\/wCZGydGoohRUcMDdxGwNz44TveYXPf9cK4\\\\\\/2g\\\\\\/h\\\\nSDxXWu\\\\\\/tiPcEtq1Y6KHou8zzXN3cSVb+c70g8Q1XLv3\\\\\\/AMSNnqulGQDqk96PFc0de6h\\\\\\/Od\\\\\\/zTZvE\\\\n5\\\\\\/tnn+8U9np07vW45hF37M\\\\\\/aHzXLzdpTt3r\\\\\\/AIuSTc3k57xx+KNpuLqBqo\\\\\\/12j4pJq4x\\\\\\/aN+a5eb\\\\ng876z80g1xP3j81Oy6XUjVxY\\\\\\/rGn+8Ek1kWd5WD+8Fy41zs\\\\\\/bJSXVhPU\\\\\\/NGx0uo+uRcu9Zn94LF0\\\\ntyjo+0eujMje7qImYOds6Vnn1ZHU+9Zyrqu54kEmTuG7\\\\\\/DCex0yV3YVUZG0jT8UZlaeRB9xXKm1j\\\\ngMB5+aWLlI3YSvHuKJVSOoucDySVzRl7qWH2Z5B\\\\\\/eKcHEVaOVQ\\\\\\/4lGz06PnYpJOy54OKa9v9uSPc\\\\nlDjCvbj9I13vajqTpu5YY5AdbGu\\\\\\/eGVBqLLRVA9qnYD5DCyo40rG\\\\\\/aEbveE63jqQZD4GH3HCOotC\\\\nquEaeG8hjHvihqGEt6jWOny\\\\\\/BJm4IkaCI52u8nDCZuvG8ToI5TTOBgkbLlrugPtD4jIVnFxxb5mh\\\\nwbMA4ZyWj+aXbYksUsvB9e07NY\\\\\\/3PAUd3ClxB\\\\\\/qM+57f5rVx8UW6XYThh\\\\\\/aCmRXCmqBmOeN2fByr\\\\nsru55W8L3CN0U3qzh3Tw4kEcuR+i1Nm4ddFMJqloGn7LOe\\\\\\/mr6eLvqeRmR7bS35pFBN39JE8nLgN\\\\nLveNj9Uk7PAbKw4evD+H+IrdXg4iEghnH\\\\\\/lv2J+BwVX5wEiojFRTyRZwHNIz4J632YcmMzwsr0iK\\\\nhg6\\\\\\/JBZThi8i+2GirmnJkjGvycNnD5goLeSa8vhcsunKyuo6UelKwjASOTRICPCPG+EelLaiQ1DS\\\\nnAAOmUelGwbxnoho8k5pQAyEbPRkgBEAnizdJ07JkbISHNyndJwkuCE005qQWp4tyEgtIJ2T2jZh\\\\nzEhzFIIyUgs5qkXujOCj1lWy30s1TL\\\\\\/Vwxukd7gMqY5uCst2izdzwnWM1aTPphAHM6jv9MpWrwx6\\\\nspjPbl4q3Vf+0yANknJmeB+s46j9SkueMc0h5AIHIBU92v0NExzWPbJN+rnl71zPvMcdSSLOedkQ\\\\n1PcGjxJWZ4l4xitVtqpIP0krWHSemroqOuus1WdUshI\\\\\\/VHILI8Y1+mkigDsd4\\\\\\/JHkN0ttFRap3uv\\\\nNI5ryHxv77V1yNwfmtfNc5qgkySuk95WM4dGqoqZz0xGPxKuzNvzSXJtZetZRGqx\\\\\\/kq11RjzSO\\\\\\/O\\\\nOaFaWprCEg1efBVRqD4od\\\\\\/5oUtPW0Rqyqo1GOqT6wfFAW\\\\\\/rmBv06qtoq31p09QDs9+ke5uw\\\\\\/NVd6\\\\nuTqS3yFp9t3st95TlCPVaSKLOS1oB96Sfa8FV5ojVeaqzPhF3\\\\\\/mmpaetnxQ9az1VWZz4kId\\\\\\/jqgL\\\\nP1nzRetearPWERnQFl61hF60Sq3v0O\\\\\\/weaAsvWsjmh63hVhn80Rm3QFkaoojVhV3fkdUXf8AmgLA\\\\n1O\\\\\\/gs3fKktugd10NP1KszN5rLXmpL7rKM5DWNA+qE1t4azvImOznIynBUHHNUVqqu8oYjncDCmCf\\\\nbmg1j6x5ovWCq\\\\\\/vj4od95oNPM6T6woJmIRd95oCaZs9Ul0xUMzdURm2QR6qf3tNKzxaR9FBsFaZa\\\\nHuycuhdoPu6Jx0ucg9Vn7dKaS+PjBIZKC0jpkbj80qV8td3++SUYqCwkhxB8ioBmQ77PVML+j4lr\\\\nqMjROXNH3X7hXvDXF8dSaiCpAjkbJnLfs4dv\\\\\\/NYPvvNNUM7or2859iWHOPNpH5FBX+Xa2StkaHNd\\\\nqB6hK14WP4NkqrjcDTxVDWRtbrc2TJGMgHHmtTPNA2rmgil7wxO0nofknMkXXiLfhzjR3CVJPQgZ\\\\njdM6VnkHAZ+uUFRVFI2peHO5gYQVal9vGz+jwyztseyw3YJQYPEIwCAldFo+d0bLcFHjKXpyUNKW\\\\nzIwlacpQYjAwlsE6EWnCcIwiLc7o2DekDZFpTmnIRFvLZPZGiD1SC3KdcMhBzcDKraTJGyQRlOuG\\\\nOiQRhMa2aISDuniEgjYp7ZWGHgfFc77Vaze2UYP2pHTuHk0YH1cuiuaCSFybtMfq4qjbnaOiZ83P\\\\nf+QCWV7O36HGZc+O3PeIrsLdAGMI75\\\\\\/LyHisDPOSSScnxUviyudUX5rQ46W6248hgBVEsmeZXLt9\\\\nlByz77LFcS1nrF2LRyhYGn3nf8MLUTTEHyXPrjWd9LVVA++5xHw2H4J2qvar+wtLLa13+8c5\\\\\\/wBd\\\\nlNMuMKNRt7mipmctMbQfkEp7tkLl1Dj5UjvT4plz9kgyJqnc+ZCi7wlMF+Qkl+AkaR3u6T3qYMmU\\\\n1NP3UT3n7oJQFbd6z1m50lOCSO8GR9T+CuzN1JWLtNUariYB3NkT5D5EkALVa\\\\\\/JTES7Su980O9z7\\\\n1T09Wai4zgHLGNAH5qdrVL2kmXGN0DLkKNrKGvxS3sbP975od75qP3mSiLkdxtJ77zRGbzUbWQh3\\\\niW6EnvfNDvcdVF1nwR61Q2kGVEZB4lRy9FrSLaQZPNZCvm1XSqOc7gLTOfpBPhusO6oMtxqvHIP4\\\\npVNrX2GbVRYzycVZ6x4rP8PS\\\\\\/oJW9dWVbMlDhkbgHCcqtpXeDCHeqPrx5oa0dxtJEvmiMvyTAei1\\\\nouxtIEgwkmTbmmC5AvynBsI53GWWNxBwQW+4qju0nqdzjn5AODs+XVWUxMdZFIOTgWH8Qq7iOPvI\\\\nWP8A7uyWkr4SahnOQUNfmqqzVRqLbA47uA0u942U7Wg9n+8TMlR3dXSvzzcWfME\\\\\\/kkl\\\\\\/goVymLGw\\\\nSfqTNP5fmlexV0DgO4er8TUozgSB0fzG31AR8S3Ka3cW1z43ljhIDt12Bws1Y640t6oJv1J2En+8\\\\nFvuK+GbdVXV1VVXT1GSpIDWvaC3IaBz+qXtNsxy2srfxDRVtKyUy6HHYtJ5FBZ6bs0r434grYZY\\\\\\/\\\\n1nZYflv+KCcyLqxfQLRjzRhqWEeAtnw+iA34o8YS8IAIGieSGlKwUEHonGUCMBKwggyMJOE5hEQh\\\\nNhlyIjLU4RhJc3O6e0mT5pJCdOyTgBXC0ZISSE6cE4SHc002bRXj2iuJ9otaDxFdpT\\\\\\/YhsQ\\\\\\/usB\\\\\\/\\\\nFxXbZG+2V5y7Qa3VUX6TP26uRo+B0\\\\\\/koyvZ6n23HfLb8Rya4TGe6anc9Bdn3lRpX80mV2q4VB6AN\\\\nb+aalcN1zfy+ox7IN2qhS0M8ucFrTj3rCyjMcUY31Oa3377rScVTYpGRf7x4HwG6zULdVfSDbHeg\\\\n\\\\\\/JB2te92E0+Q4Rudt+agUsxlhLy7Vqc7B8s7Kt6aT+UkvTeo9UWsgc0hzsndLe1Fl+UnUkFyLVtz\\\\nR3BZdjqq2+1Jhoy0Hd+3wU3Us7f5zPVCNhzpGMDxRYm1XcHyescQ3CTOQyMMB+IWuraj1elfJ1A2\\\\nCO8cEx8BcUvtzMmR9BTzy6jvre0F31VPxFPpiijB+0clLC7x3GeG9dy+H3l0sxO5IyrnVvhZ\\\\\\/h5+\\\\ne+38Fdat1a4d1eaGtNF6AchRzWUNRKaJQ1Jlo7qKLUmi7dFr3yp3BqnS9APJTWsnwQ1b7o2ejupF\\\\nqwkFyQ+VsbS5xw3zRuFYOqkLaaR3g0qpfwmYuEIuJWueTUV8lHI37rQ1jXNPzLlMukuihlyQM4A+\\\\na6lScOms9FSacM1SQ1rqsHG+BJpJ+Sw5MunRdO3IOH5NM0rfEZVpRSlzZMnlI4fVUNneG12M8xhW\\\\ndumD56toOQJD1W3g1oHoa\\\\\\/LKZLketPcGj2tDWmg8AIainsjmvzQLjyTWs5RF3wRs5Ee7SFlGZAcG\\\\nJwfn4qPcpRU2nvG+R+qduhBttUD\\\\\\/ALp34KqtdQJ7dNCTk6dQ3S2V8pHDUxMVRGeTHhw9xH+SuS48\\\\n1m+H5hHWyMzjWz8D\\\\\\/mtBqUiHNRIUC8O00Ejh90h3yIUondQ7qc2+f91MXwnxTBjmvHMEFb+5dpFu\\\\nrO4cbS6plhOqMzvAAd44GVzhhxG33BCR3sO36FGtpsl8tdUdol7qZS9lSKdvSONgwPnkoLIUry+m\\\\nicTzaD9EEDUfVcNR6UaGFpt8Vok7FGAj05QxjZIaBBDCMDfdMaEi0pRHgiSGiEEojKLCe02EEJBC\\\\ncIykO2TTYbc1II2Tx8ElzdlUpGS3BSHb5TrgU07ZUnSLM7Q1zvDJXlHi+sM1C15dnvpnye8FxK9O\\\\n8V3Bto4cuda4gNgp5H7+TTheTuKZcUNtb1MQcfkssns\\\\\\/bMfyyrExvLpap55GQj5bJuVyVACIs9XO\\\\nc\\\\\\/5klMyOAWT6KeGZ4plzUUzPBrnH6AKoo97jS\\\\\\/vE\\\\\\/RS7\\\\\\/N3t0kb0Y1rfz\\\\\\/NV9HL\\\\\\/AN40uf1iPogv\\\\nloZanMErsYLQ76KBZXmS2Qk7kjKclkzSVW\\\\\\/Iv\\\\\\/NRLC\\\\\\/Nri5ck2iyLsJDnYSXOSC74qvBwvVsklyQ\\\\nXJGr4I2o4XgAnKp+H6U3njC2UwwTU10UXlh0gCm1s4gpJX5xhqV2Qwis7UeFYyM5uUL\\\\\\/AOF2r8ln\\\\nndY2p9t325lv\\\\\\/bFeWtAwyngYPg1cj4gqtVeI87tbsF03tpqHP7auJg4bN7trfdoC5BdJu9uznnf7\\\\nX4hRx\\\\\\/sifC84cftMPcrvVss7w4\\\\\\/DpRz2CvNa1kXD2vzQLvBNa8bZQMhQWzmtDWcJnUURcjVPZ4vJ\\\\nwhqTOvCGtGi2eD9yiL9+aa1+aIuT0NnS4Dqq+91IhoXAnGogKUXrN8T1hce6HJo6eJSs0W1xfJx3\\\\nTIx945XrHgbh3170b4rcWAOqbdM8B3iS5w\\\\\\/JeOrhU+tVDQ05AaGjHivoTwPY3N4Js1raMu9RjiAx\\\\n1LP81x\\\\\\/UXWlz2+e9veY66LPR2Cp1nl\\\\\\/2+ub\\\\\\/AOY7\\\\\\/iKav1tm4f4pr7fN\\\\\\/W0lZJC7pu15CZs8mLtV\\\\njPOR\\\\\\/wCK6pWcaPXlDUmg5DV4K9Hs7rR6k1qRh3wS0ezupESm9WDlEXZRoGLo7\\\\\\/u+o\\\\\\/cP4LL2aq7l\\\\n0Lz9kjB+WFo7q\\\\\\/FvmGebcLG0jv0DR4ZCSL5W9BKI7vER9kuLfmtOXLERy93NE\\\\\\/8AVeD9Vsw7knDh\\\\nzVlRbkc0M\\\\\\/7hT5cfFRbk\\\\\\/TQT\\\\\\/uFVRUiN36Jn7oRk7FMxHMTP3Ql5wpBFE8mkhxn7IQTdA\\\\\\/NKwA8s\\\\nj5FBPYkfWjThFhSDFkpfq+d8J7fGdKLjCMN8VJMHij7odAl1QaRNKBGFJdH8k05unxTl2VmjSIjb\\\\nKV5dUWk5TIlERlL0lFpwgtGzsknklkEkhJIwMJ7RSHbImguCWRnolBgBRsTHZhzDlNSMPRTsb7on\\\\nNbkhG1dLi3pKXt9m7OJIIyRJcKqKk2O+Dlzvo3HxXAuO5hA6Icu7px+a6B6UXErLhxlaLFE\\\\\\/UyiL\\\\nJJQOkjyNvg0fVcz7RHh1xEXiGMHzyot7voPoePo4932zYJbFGPBoH0UaV2cqRM7nv1VbcJhDTTPJ\\\\nxpaTlTHremLqpjNVVDz96Q\\\\\\/yUaB+m6UWDzefwRk\\\\\\/ox44TIo6qWWOsip5ZKWkkYZ5mtJbGHHAyemS\\\\nlbpne2O1o+rPeXOA8mtc4D3gpjhio721tHPGPwUWsfpvNRgnEkT2\\\\\\/TKgcKV7IqKQvO7W\\\\\\/gUbVL+W\\\\nmvLvgmnOHio8VU2aJkg2BGcEpmpuUFK0l8oyOTRuSq3trOyW6QAE9AqehuDqi5yt1ksI9kHknbpV\\\\nhlue8Hd4AHxWftNRou9OzP2g4\\\\\\/T\\\\\\/ADSFq9v8+mlawH7R5LV+jlRmt7X7GNOoQiWbccsMOPxXP+Ja\\\\nxjamOPXnDc4G5V72Rdp7ezLjB12fbHXAupXwsYZNGCSN+vh9VnybuNkTcpK3fb8z1XtovfTvKeKT\\\\n\\\\\\/CP5LiU0hdVud5fmtZ2gdotf2i8c1t8dSU9I6WBsQhDiQGtGAc+KwwqXCpk1MxhoyW7+KOOaxkqe\\\\nrbScOyfppR5LQatllOH6hvrgw4Yc3ASrheJzNIxj9DWnGy1i2oMm\\\\\\/iiMiw7quV+5kcfeUPWH4+27\\\\n5p7Db60RfvhZuz1xDZw5xOGFwyfBVr6mRxP6R3jzRsNsZENefFYbv5M\\\\\\/bd81Z2Oqe6r0ucS3SdiV\\\\nOw02vbmUQlDxkHPmCq2uuMbaB72O3cC1qoKO5S0TyWHLTzaU9hr5ZhExz3H2WjJWNr6k1VQ6Q\\\\\\/ed\\\\nyT9bd5axmggNb4N6qsLwZmt8BkpbK2RqeCrab1xRaaEDUaiqijwfNwX074Lo2\\\\\\/0pTsDRphbsByAA\\\\nwF84ewSrooe1zhc11RFT04qi50kjgGgiN5bk+\\\\\\/C+k3ZzWUlfPVS09XBPpa0fopGu558D5Lg57+UO\\\\n5SYZV8\\\\\\/fSftLLH258VwxjSx9X34H74DvzXNLRL\\\\\\/3xNvze9dv9OGmbR9u1Y8DHf0FNLt1OC3\\\\\\/AJV5\\\\n6t9a6CofLjJD3LrwvaMsL2jc6j0OUrUqey1gqKYtc7L2nqd8KyyVrLpqe1I9Sa1IalWxo7rwi15T\\\\nerl0Q1DojY0g36XRREfrHCydI79EfJx\\\\\\/FX\\\\\\/EU2BGz3lZulkDmvGdw87fFRfKb5iTI7LHYzyytpA\\\\\\/\\\\nVBEScktCxJOprvctbbHl1BA79gfgg\\\\\\/aaT4nZQ7u\\\\\\/Ftqf3CpGfNQb27Ta6gjnpwnsWdkyI\\\\\\/oo\\\\\\/wB0\\\\nJRTFFJ3lJE7rpCdzlLZotJOY++Zj7Mjv5\\\\\\/mgq64VHq1XIME6sO+n+SCNI6tPsWHI+88yk6Sj0FN8\\\\njNj1Z80C\\\\\\/wA0nGETh0CNbLuN0ibc7UEWDgolUTQ5HKHVDkEg8k0nHPB5BNl2fcklDlhAAJXd5SQQ\\\\nClteg4HdYROZjluj7xAvCnuvsQeajXGsit1DUVczgyKCN0r3E8mtGSnZahkYy4ge9cu7feKYqHs9\\\\nqaSKT9NcXtgGP1AQ5\\\\\\/zAx8VNrTDDqykjyzxLeH8SceMuEpzLWVglPiATsPgAE1xzN33Eobz0DPyH\\\\n+arbc\\\\\\/1njGgGc4nGB5BpT3EM4n4hrZAc6fZHzP8AIJPpMJ09orZj1VDxJIW2qZoO7i1v1CuZH81m\\\\n+Jqg9zBH+s\\\\\\/J+ATbVn3dB0XoXsx4FjrvRW7Rrq6Md\\\\\\/PI0xvI+7DpJ+pK89E+3jbGF747EOF2V3om\\\\nU9BoDnXKgrZHNI+0XSS6foGrDlupGfJfxj57VMxMglO2AAfdjH5q94\\\\\\/4Vh4ZdY5KZgZT3G2Q1P8A\\\\nfxhx+YVBWRHuHxu2OnSR54XZO1i1x1\\\\\\/YV2d35gzNCwUT3fslriPqw\\\\\\/NPK6yxaSbu3DXTv06dbsN5\\\\nDKYc\\\\\\/Ls5+ZTdXWxUw9tw1Hk0c1EMzp26tL3B33G7fVbbh3LV0ta+7NlpIIGfpJGjdoPL3lVETnuv\\\\ndK2Ulrcf2ZwR5ZTrGyAYDGRN+a1Nq4BF07OeIOKhUSiqtVdTwBgADHRvByT1yDhZ5Za71nlLVTdo\\\\n4oaxzYmhjQAD5lNWenNdeoo42OmdjGmNpcc55YCjyQtmeXyAyOPVziV62\\\\\\/0flphk4r4lqTBG4sp4\\\\ng0loODk8kZ5XHHasrZNvKF4tlZZLi9lXSVNMXD2RLC5hOD0yFTtqQ2qmOh+4b09691+nrYYxYrXc\\\\nBE0SRV4j19dL43Ej5sC8Nu2rJgf1W\\\\\\/mpwz65ujv27hS1wpalk7A4Fp3aWnBShUiqLpACNRJwUDz5\\\\noicLVXeDygkkosoB6KZ0OrScagWn3JGUjOUWUDZzKdpqp1K8vaMkghRs+aGUzOF5PVIdIGDLiAPE\\\\nospuWnjnPtjUB0ycJFbfQCcyn9HgN\\\\\\/XcmIIWy1M5kd3hbgDp0UuOljOGtjBJ2A5rQ8dcDU\\\\\\/BvFE1\\\\nqYHOfFBA6TU7fW6JrnfVxU295Gdltigihb63HpYBpaSt9wnX1tijFTRVlTRVDt+8p5nMdty3BXYP\\\\nRt9H3hvjbhCout+oZqiepqTFTOZUSRlrG4Bxg43JPNd5v3oQcL1lJGLJc620SNZ9iYidhPxwR81l\\\\n+rjuyq7YT8vbwl2icUXriviJtbeLlNc6ptMyJstScuDGl2BnyyfmsfTzDLw8FhLzz5c1130iOxq7\\\\n9jPFdDSXKaCuhrKYyQVFNnDg1xByDyP81yOKaKZhGep2PvWmNl8Ixsv7UmORzDlriD5FS4btUw\\\\\\/Z\\\\nkJHmqxwdGPY3A+6SlRyh\\\\\\/kRzHULRrK09tvb6qYRSMGT94FWJqgKpkPVzSfcspbZO7rYTn72FKrLo\\\\nWXN72HcAtH4IXtY1V\\\\\\/EMzmNj1Bp5kp2gvLa2XuzGWOI8crMGQucSdyeqkW2oFPVseTgdSkaVe5jL\\\\nWOHRnsqMKJrbTDVMADtRD\\\\\\/ME7H\\\\\\/rxTVRL3s8jyc6nFyu6Cm9Z4fZDt7bD885CEZeVDnBwOS1Njdq\\\\ntVNv938ysqCds8xsVp7Ef+6af3H8SmflY9VW8QSBttlB6kD6hT87+SqeJX4oGD9aVo\\\\\\/EpHfCXaXZ\\\\noI1Kz\\\\\\/1lV1jl7ygA5aHFv\\\\\\/XzU9BotVSiokDj0GEFK1FBNnqvr7o3Q0BNQ1TZWZByld8CSAVL5fsP\\\\nQi7rfOETpdMZPUIRVAfGDkZU77loTo9uSbMSk94wdU2+Vuei0lK4xGdGkPbkqTrBSJHsa0kkDHVG\\\\n09MV9U6SAagwub1wEuKQVEYc1PNmEjeeQoVHI2CslgJA1btRsdEqQWkdMpJJ9yeeTpJGMhVVJe4q\\\\nieSnf7EzTjB6quqTynovpNLzhR6iqEW7nBg8zhMPvUNPWerykNcfsk9VScTV8kcRD2ZidkBzeanL\\\\nkmE3SmG75XNRoq4XAEPBHMFeS+2C8vqeJJqMyF0VCzQBnk47n8l2W3cVVdodVQ1BPcwtMrXn9UDJ\\\\nXlziG8T3I1tdUu\\\\\\/TVDnSv97jn81hM8OazPH09P6Pj1nbfSv4OPf8WwOG\\\\\\/dtklOfANP8ANMTVHf1N\\\\nW\\\\\\/xlI+SVwX31DdK6snjdHCLe90bnD7WXAZHyVfSyO9Wa4nd5Lz8TlbSyzcevj3vY5K7KynEU2uuY\\\\n3mGM+pK0r3jCxV1mMlwqHA7atPyTaWGHOABOenVfS\\\\\\/0cbxRX7sX4WmoWPFHHSimDnjGtzPZeQPDU\\\\nHD4L5lNdrJbnmOa+mXozWo2fsG4LpTzFEZNv25Hv\\\\\\/wCZc\\\\\\/N4jHk9PnP2lWgWPtA4otwbpbSXWqha\\\\n3waJXAfTC7dwbaG9p\\\\\\/oxV9khA9et75DG0HfW062n4gkLKelvwrJwz22X2Qs0w3HTXRnGztQ9r\\\\\\/EC\\\\nrX0QeIRTcTXmyyPwyrphURtPIvYQCPk76KOTdwmXw6OPVkebzTNhyNIDuTvHPmkE4XSO3jgk8C9o\\\\ntxo2gCmqf9tp8DbQ8nb4ODguauO5XTheqSq1obea9GdhvDn+sHYHx9StZ3klRI\\\\\\/Q0DOXMjBGPivO\\\\nbTkhexfQ8iB7NrkTgh9ykB\\\\\\/gYFjz9sTk3dPH7WluMjfkQehXsv8A0fMGaviqToBEPxXl7tK4afwp\\\\nx7fLY9ulsNU8sGMew72m\\\\\\/Qr1Z\\\\\\/o9Wg0\\\\\\/Fx695EPolyZdWG2Wfium+mHwv\\\\\\/TvZRcpWsL5IGidoAz7\\\\nTDn8CV8zKo93c9PRzF9ie0SyMv8AwnW0b2hzZGFpBGdiCD+K+YEHZ0yaLjyCaMOuFkhEkTs8tMhD\\\\nvm1ZceUksqPOM05sdkklOPB5ponAXa1AlETlFnKCehoecIZSeSGpMxoIs7IA5QBpbRlITsW5x1QH\\\\nRuwTgp\\\\\\/G\\\\\\/aXaaQxa6WmkFVUHGwYw5395wEz2q1h4g7UuIqmEF4lrnRxgbkgEMaB\\\\\\/Cu6eitY4+Gez\\\\nniLi2pYGSTGRkb3bfoo2ZyPe4n5LBejnwIePe0g3GoDXUVtk9bma7cvcXEsHz3+C4+v8ssvUVJ3e\\\\nzuxzgxnC1qsFkaARRwgSEDm4DLj812otwMLK8B29wE1U5ux9hpP1WuIXJj8ubnu8tfDxh\\\\\\/pEbeTb\\\\n+EK0My1ks8RfjlloOPovDOkGJoIyMdV9Bf8ASG02rswsdQBnu7loz4amO\\\\\\/kvn7KNJxyxsu\\\\\\/iu8S4\\\\n\\\\\\/COY9OdDi3y5hIfl2NbcEcnNTxwiyt22iI6jScOPLk8cv8kprtUr3DfAwkyQsmB1cyMZChW7vomk\\\\nnD4iTyO4S9s92WRaasIw7ZIBCGU2kLJw0+QWvt7O6oYG9QwfgsZK8MgeT0C21O4Pgjc3kWghBzzp\\\\nm7vTmC4SHGGv9sfmrmwuP9EU\\\\\\/wDe\\\\\\/wCIqLxHAXQxTDkwkH3HCk2IYtVOORw7\\\\\\/iKJ5E7VY6lTcTH\\\\\\/\\\\nAGaBv\\\\\\/m5+hVsTvsqLiSX26ePyc78Ag8kjh0n1WYf+Zn6BWupUfD0gAqGZ3y130\\\\\\/yVzqQIMoJDnAF\\\\nBLUN9aTN6pUFvJh5KTTkuYXE81gpuJZ7lRwSMbh7gDk9Vqai5Pt9rjmmABwOSz65O75bpqzjlJL2\\\\nE7qBSVzmesNO+gqqouJ4q2qY5jg0nYjxVdd7+y01tQHcn7hY5ckmqnpu2yFWDAHk74yo764OdE4H\\\\nYnBWQ4ZvVRcHTySOJiAIAKmOq3QPDHnHtZCM+bUlFwu2sdUCMAlUvElwdHTsjY7BeRjCav16gp7M\\\\n+QSgPa3IwsL\\\\\\/AK4i60MMrTr7s7\\\\\\/BPm5ZhhsY4Wtvdro6zQUr85acBwKqbxdKmsuVM6nd3bepCqn1\\\\nFRxXRTvLu7ZE3UB5hV1LPXMsslZtI2F2CQOiP1ZnO3heOGu7Y8ZcXv4ct0QiAlqHjYeKwFdxjK+S\\\\nG4MGidpHet\\\\\\/FUfFN1ud7mpLpTHUyEYMZ5HdZeg7QaWtvFbS1zG073gsLTtullbk1xx6XQ+O7xNVW\\\\nSnutNKBJGQfZKn8OcWDjbhORwcBU0+MjK4m\\\\\\/i6SjbV2mWXVA4l0eSsVLx9dOC7sDbZi2CoyHsHVR\\\\nhyW24ZDPhlm47N2o8QwRcFuEZDKyZwpvZ54P2vouAV0T60x0sf25nhuw5DmT8Ap1dxTWcQvb6ydo\\\\niX6f2j1+QVjwPHTz37valo0RMc1hPLW4Y\\\\\\/D8Vy2\\\\\\/9JwZ8mXn\\\\\\/wDad\\\\\\/FJx8fV8o\\\\\\/G08NJb6sU+zY6\\\\nOGlaB5uJ\\\\\\/JZEERxtaOjQFacbVDTNVU7XZ117G4H6rWZVRI7Zdf0c1wYb+HVxSahmaXGVgZp8skl6\\\\nuy\\\\\\/K2F0qPV6KeTqGnHvWOmp5aiNsEEZllkc2NkbRkuJIAAXZtrlRMkAYc89OSvqj2IVFNVdkXCMl\\\\nJK2aD+jYWa2bjU1ulw+DgR8F87e1DsMv\\\\\\/ZLb7FV350DXXmCR7YIXEug0actd54eOS97+i9JbJOwf\\\\nhGO01IqqaCmdG94GMS63Okafc5xXNyWZSWMM7vTkvp6cCiv4Xs3FELCZaGU0s5A+4\\\\\\/dp+BH1Xjbg\\\\nji+p4F4st95pQHSU8ntMPJzDs4fEL6j9sHCDOOezLiSzOjEstRQymAHpM1pdGf4gF8nayJ0Ezo3g\\\\ntc0lrgeYPUJ8V3LjWvDfMepvSa4Qg414GtvGVuBklpIWl2nfVTv3\\\\\\/wAJP1K8jSDBXrn0XONoOKOF\\\\na7g67OFTJThxijl5Pp3DBZ8Dn4O8l587XuAndnvHNytDQ40rXCSme4faicMt+WcfBLivTleOumzt\\\\ntiGfbC9i+hzJq7PbrH+rcnn5sb\\\\\\/JePGMIdyyvTfokca2+y2i\\\\\\/WyqfM6pfVslhhggfK5wLMH7I23H\\\\nVPn74dixusorfS+4VfR8X22\\\\\\/Rx4grqYQSOH+9YT+LSPkuo\\\\\\/6PRuY+MB1DoT9Cn+2uy3XtM4Pntlu\\\\n4VqZZmuEtPU1crYSxw6hucnIyFhvQgn4otvaPxLZKF1HSyik1VcNcxxAfHIG6fZOQckrDDLfH030\\\\ny5+z3rWU4qKSWL9ZpC8VVPBDLV6QfElskafUuJbPK4Zxs7k7HxXqW5cZX7hsh13sPeUYb7VdbXmW\\\\nNvjqbjU0eeCFyHjC2nintG4L4ksxbNGypmpZjE7UBHK3O\\\\\\/uLfqsqXFLrb553y2SWa6VlDM0tlppX\\\\nROa4bgg4Va4Fdo9K7g+o4Z7X6yrMPd0N4iZVQuA2Lg0MkHv1NJ+K446Mg8t16PHl1SVW93RnGySH\\\\nZzjfGyd7vxCbZTysL3OjcInuOh5GzsYzg+W3zV70LdEnmhhOlhRaD4KlG8YR4JKWIz4JQYTtjCWw\\\\nSBgEnkFdcE8NVXGF\\\\\\/t9tpWF89ZK2NoH3Rnc\\\\\\/AZK6N6PnZfT8ZX2qu94iDuH7NEaqpDxlkhaNQYfL\\\\nbJ8guzehz2XM4g4ru\\\\\\/FTaYR0IfJDRjThrGl2XOHuGGj3lc+fJJuQtd+7R9udKzs17B6W02xndxVE\\\\nkdtjA+04AZefMnH1XQuxTsvh7N+yqxU0tKGX25D1use4DXlx9hmfANI28cqdxxwAztJ7eeGrPNGH\\\\ncM8M0AuNVCR7L6h73CNp88NB9y6sIG3TiaSV5a2mo8NGTgZH+a4r2x0icnVl1eov7XQNttBFTt30\\\\nDc+J6qQQoUl\\\\\\/t8ZINSzbw3T1NXU9e0mnmbKBz09ESuWzK968wf6QWYDsps8Jxl91Y75McvnhVP0O\\\\nHm7C9wf6Q7iJ2vhSxMfhpbLWvb\\\\\\/gb+BXlGq7N54uzb\\\\\\/XCqL4mSVraaljI2kGDqd89l2cdmOP923H\\\\nPx3GKJwUeERGSjaDkDmul0RbcKcOzcVcQ26004JlrJ2QjHQE7n4DJUjju3U1q4yvtHRRiGkp62WK\\\\nJjeQaHYC7r6H\\\\\\/ATLjea\\\\\\/iapi1Noh6vSk8u8d9o\\\\\\/AbfFcA4lrXV9\\\\\\/udU45dNVSyE+95KxmXVyWfBW\\\\ne1Vj4ox5ojukyHDNuZ2W1K9lxw7wtU8ZXSK00jg2adryHEZxpYXflj4q3s8MtPbaeOb+sa3BXT\\\\\\/R\\\\nF4bZdOObhXSM1to6Jwa4jYOedP4ZWIu9Ibfda+mcMOgqZY8HyeQsplvOz4OY9+pX1MDamB8TuThh\\\\nRrQwxW2Fh5tyM\\\\\\/EqWTsfJR6A5pIyeuT9StvJ+9pBO6znELya6Ifqxn6n\\\\\\/JaI8ysvfpQbng9GABFK\\\\nk2acxXRjeQkbgrTk81iu9ME8MoJBa8LZMkD2NPiFMLG+izuUEWUFSn0qhqBSQtjeAWRDAUy4X6O7\\\\n09PE12oAjKz00pkjIz7I+qiUOqKNxzjfYLyeTL8XhdO1zemNoa+B8WBkZ2WYv9ydUVjHSnA5KRdq\\\\nyaq7qbXjRthVVzozWaHOfgNOfeuTmzm5spGhs95fRQ9zG3Ifywrq6XJk9NFqy2UYzsslbqjupI5A\\\\nAQ3optTcHVUup4AC5\\\\\\/1t4WWlcWrrqairOGyCRqLcElcgtEwobpU0QPsastC09xuEojbExxEZG5ys\\\\nhVNbS3yOYHIdsSq5eS8nFDnZfXDiGW3UZpqclr388eGVHHGzLPw9VUrn71A6\\\\\\/wDXmq9zobrcHNe\\\\\\/\\\\nQGZzhYrjKeOStipqUF7GnBdldn0+X\\\\\\/xxUx+VhdONaWitNPGyqDJQQXAZ8d1h+Pnw3mlir7eQ+ZmH\\\\nuc3xVNx\\\\\\/FS0RhbG5wm0guGVRcLcRmlqRSze1DKdJyeWV0W2TcVpDhv8AUVNbEJXu1N23Kc4qvffu\\\\ngixpe3cFXl+4OFOPX6fBaXA7eCy3F8TGy0b2EEnOcfBRMpnlKvWlzZ5y63momOXPJc4+Q2\\\\\\/Jbbha\\\\npp7hwi6nAEddC8ykY9rJP8lgJZG0Nuoac\\\\\\/2jmsPu5lauzj1LimQBv6OWMnblnCx+vx6+Lp+O7Tkn\\\\naYz0xlZWCo4mqYHZL4pHSuyOuAB+KfkdzUKNvecR3qc9ZiwKTK7den9PNcWP9nXwzWEUnEk+KWOM\\\\nffePkN1tvRdtUN77duFIqhofBBLNVOa4ZH6OF72\\\\\\/UBc7v8\\\\\\/e1rY8+zE36ldR9FZsw7S6mqp4TLUU\\\\n1qqjGGcw57e7B+GtaZ3WNPLvK716YUMHH\\\\\\/YxYOJ6L24aasJcWn7LX+w4fxNC6F6G9IKXsCshDdLZ\\\\nKiqePMd85v8AyrK2bguS4dh\\\\\\/HvAssjp321zqilkeMkteO9Z\\\\\\/ia8La+iNK2TsC4bYBpdDJVxuHn6z\\\\nKfzC49zp7ML2nTXYNO3LK+efpQej1fuHu0G5XWy2WorLDcX+sRyUcfeCJ53cwgbjfJ5dV9D\\\\\\/AIJD\\\\niQNiQUscrjdwY5dN2+QNrmvHAl\\\\\\/pLgyOagrqV4kZ3rCwnB3BB6HkvWV74UofSj7ObdV2t8NHemn9\\\\nC+oBAjkGz43HGcHp8F6Q7Q7PwPBSvr+K222nhYNRlrSwZ92efwXnbin0suzXgl4o+E7TV3h0LvYN\\\\nM5tNT58QS0k\\\\\\/JPLLLOyyd46bz4SarJ9n\\\\\\/oK3+G9tqOLjTTUMe4o6KXIlP7TtsDkvTVl7NKfhigbS\\\\nwxUVloYx9hmlrcea8gca+mV2i8Zh8VqEHDlM7k2iaXyY\\\\\\/fP8lxfi3ijjO\\\\\\/Ey3i8XKsB2\\\\\\/TzPx8uS\\\\nLjcu+Vc8+pyn7Y+ll94y4CoIIqO4cWWumqoWYDn1TNWPMZWF4T4t7JuD+PblxMzjKyR3CupxTTOj\\\\nqQBJhwIcR47c18z6lsoe5zmuc7xO5UB8r2uJLTn3Jzjk8M7y8lmrH2Jp+3rs8qCO74ytDv8A8poT\\\\n1ZSWXimrprxw\\\\\\/c6JtwjcC6ejkY9s7M7tkAPteR5hfG1tSQd8fFXFpv1ztrhJR1lTSu6Ohkc38EdE\\\\nZzluL6vdrvYvbe07hj1Cuoo56mMyCnqcDvIQ451MJ5b9F5uqPQMeM6bzVt99M13\\\\\\/ADLE9g\\\\\\/pY1vD\\\\njYrXxVeLy6B0nsXKKdswib4Pie06h5ggr31wfeZOIaCmr6W822\\\\\\/WqduptVSxGN3LbYOcCc8xsp\\\\\\/L\\\\nDxXVhz9u8eMh6DDITme\\\\\\/VePKjA\\\\\\/Naub0VbbW9nsHCraic1NPUSVUNcYhra9wAcMfq4AXsfuwR4jw\\\\nQEAbuGgeeFNyyvtt+vNftfO2X0I7qxxH+sNPz+9SuH5qI\\\\\\/0KL213s8QW\\\\\\/T1zC8L6OOp2O+0xrvgm\\\\n\\\\\\/wCj6dw3gjP91P8AU5PkfrY\\\\\\/D51s9Cq7u+3xDRDw0wPVlb\\\\\\/QolEo9b4jZo6iGmOfqV9Af6LpSf8A\\\\nw0X8ARtttM05FPGCOWGhP9TP5P8AXx+HC6HsUg4U7E6ng+1U7pKi7xeoGXT7QbLgSzOPTDNR+QXU\\\\n+BuC6Ds74UpbVbqfENLEGhjObsDl7ytTpPTICg3OsqaRgFJQSVsxGQ0Paxo97j\\\\\\/IqPLnyzuTNcL2\\\\nSusFmuVZUDvL3dqp1XOOeguw1jM+DGtaPmquDs2uldUSTXS+mCJzie4t8ek4z1kdk\\\\\\/IBc57dvSpd\\\\n2Q07qR0Vpl4gLNTLdFM+pc3\\\\\\/AOo4BgZ7sErzBc\\\\\\/Tx7TbjUukpn2+giPKKKmDgPiTlV02s\\\\\\/1emaj3\\\\n3Tdl3C9O8PktTKyUffrXumJ\\\\\\/iJCkVvAHDVWwB1ko4iNg+CIROHxZgr51T+mp2syNOL1BH7qRm30V\\\\nVVemh2t6HsPEbWgjBxSx\\\\\\/wAk+mo\\\\\\/V9tv2xdml77Ze3292Lg6pkraO2tbStmuFS58VNgZcwPdk\\\\\\/bL\\\\nsAZVD6Td0u\\\\\\/DNHwx2e3OghoJbPRRPmNJP3sUp0BrXDYEcicHxVb2Iel1VdkTJIqvhyC+Cepkqqip\\\\n9ZMU8kj+pOlw29y7hW9tPYF6Q1TE7iuilsl9liEDKqryzQeQHeNOCAfEBXLZZueGs5ZNarxHgO+z\\\\nupFFSS1VRHDGwySyODGMHNxPIL2PcPQX4cvUIruGuKJpqZ+7ZKfu6puPgQfqn+zX0QW8J8aU1wrr\\\\nobv6u4OgpvUzF7f6zjqOwWt5sZHZj+XeN12WcFv7Oexfuqwthlt1vnrKuQchKQ5+D44yB8F8+Ki2\\\\n1dTQ1FTFA98EJaJZRyZqOAvqB272C5x9jlZYLJTme53d7KR8g2axrjl7neQAwvEXbtDbeB6K38BW\\\\nMtkjoh39xqh9uecjbV4Y8OmVjxWy\\\\\\/wA0XLr3rw4hpDdugQjZ3k3k38VfcN8G3Xi6eritlOJDSwPq\\\\nZpHnSyNjRuXHooNjs9RX1dPRQsMtVPII2tAzqc44C7NzwXvT1P2E8F1dq7CeJrpB3kVwudLUSwyQ\\\\nkh7WRsOkgjkcglcEkDiXOfI+Z73F7pJHanOJ3JJPNfRTs54UoezbsmifdXYorZa+7nkDM+w1n6V+\\\\nP4ivnbxNLS0tfXOoJ21lHBUyNimi+zJGHEAj4YK5+HLeWVVc51dM9IsztMb9+hKZocCih\\\\\\/dCVUSB\\\\n1G+RpBGgkEe5JpNqSEfsD8F1j2e1LJXsCa4TjO4xv4bLVlY2pl7ysqnf+YQlfgr6R3DvYXN5P5fF\\\\naixVff2+PO7m7FZZ7u7cHfdOx2+qtOG6nu6iSHodx+KXjwU8tGggRhBNs+glPWOdRF7jsNvehFW6\\\\n4SdgcckuGz1hpYYhTyDPP2Ump4brTVtaynl0Dn7JXkat7aeJLPCPqLsA8kl36eJw3AyryOwVeABT\\\\nOx7k9\\\\\\/q9V6CBTOGf2Vy8mFs8DLUZOinLZTEfHYqdIfYI6qdJwlWsl7xlM\\\\\\/Oc7NT7OHK94BNM\\\\\\/wB2\\\\nlcvJw5XvInKz5ZWondFQzF+xbuCs7UyOmtonZhz25IXRangyuq43sdTO0u5gtVTH2e3SMOiFM7uj\\\\ny9lVhxZya0ncjmPDdxmlNXWTHDSCA1Qo521Blc9oa9rtQPit7a+yi90tXUxOpn+rvyR7Jwmr72OX\\\\nmooCKaF0c\\\\\\/TS07r08cLjdSdlXLHXlxe80NNxBeWuLsPxoIHIbrOcT8Jiyztkil1DUDsdxuu42LsE\\\\nv7YXOqIXCYknUW7p6r9HC711NI+TvHTE5aCFWs9iZ4T255Z6xs9qbTVXsh7MAkLm98sj\\\\\\/wClHsLy\\\\nWscC3fbGV6Sk7Br\\\\\\/AD00EToSzuznIGFk+0HsPunDFumvU2owRljXjHiUcfHlMlTPG3W3E+Jnvkra\\\\nWKPYxDUd+v8A0F0S0U7poWV79maMZHTZZS0Wtt74ypqJ7tIqKlkAJ5DOAPqV37iTsmqOBuzi61VS\\\\n4tjpacu+JIDfqQtOfDLK9mnJlMctX28407NPrEgOTJM9+fHdE8k5ypQi7uBjeobhVt3m9XoZnA4d\\\\npwPeV3YzpkehjOnFkqmUzVM0m51vJHuzt9F3f0NHt\\\\\\/7TLvE4e06zS6c+Iew\\\\\\/kuJWqmElU3UNTWjr\\\\nuuleilUyR+kRY6cPcIaqKqp5GA+y\\\\\\/NPIQ0jruAfgp5P2VOV1I9XdnHGD6vtouFEKZzrBcqEUHrhb\\\\nhklTFrcWtPX2XOGfJaP0b6Y8OM414SO39CXuURg8+7lAkb+KxXaxU1nDdgpWWGke+8Q1sTqGnpGY\\\\nIcx2p2AOQ0h2fetX2Z3uGr7abpcYSGU3FVlprixh2\\\\\\/TRARSNx+sCMH3Lgxu4y5senLfy7NdLjDaL\\\\nZVV1QHmGmjdK8RML3EAZwGjcnyC8adtvpvcS2iqfbuF+H32aJ+Q2vvFM4Su82MJAHxyvawGN+q5j\\\\n6QPY1SdtPAFRaS2KK6QO9YoapzBlkgB9nPMNdnB+HgnjZL3YWbjy7ZPRz4i7YIaTiLtD4trZ31cT\\\\nZWU0DgXMYRkDcaW8+QC3Z9EThBvD9VQWW1yVd2lYGQ1VbOXOY4kAvOMDYZPJdD7Nn1X+pVpgrozD\\\\ncKSEUtTE7mySP2SD8lv7FLJbZHVjqaaSCJjnPMbCTgDJx4r1rhjMNx5lzzuWqpOzj0YuBuzuGMx2\\\\n1t1rtID6qvaJDnrpbjAWR9Lm4cJ8K9mNTbJrdRC53BuikZHA0PjwRl+QNvBWfG3pecEWnhqoqbBc\\\\n47vd3ANgo2scDqPV2egXjDjz\\\\\\/W3tPuE13u1W508mS1lQ85A6ADoPJcuOFvevT+n4cuS9XoXCtV2W\\\\nxdinEsF+pnScbvncbfIwOy1mlmnBGwGdecrz7UwgvJHJWd1hqrdVSU9VG+KZrsFrmkFb7ss4Hh4g\\\\npqqW40LpGSYbCXAg58QuiYvUww1a5BUUhIBwdl7v9EyltHGHY9TQ3C10NVNQzyUxdLTMc4t2cMkj\\\\nfmuM8SejHxAKV1VaLNcamLGTF6s8ux4jbdelfQX7N6yzcG8VUl8tlXbpzXxOiZUxOidjuzkgOA2y\\\\nplmGW68r6zCW7lb2D0dezHjCkLK3hKhiqWc5KbMRI8fZIUjg30cafsmq5K3gS\\\\\\/XKjLh7dqukoqKK\\\\nXyIADmnwIOxxzXRP9XKqy1rJqbVNCDvp5465C0oGBhcnNJMt4+Kz4bvHVR6F08lJE+qibBUFo7yN\\\\nrtYa7rg9QpOnKMDZKXO3I0jyQ0+CWggE6fFAtwEpBAN5ABOduq45xxVdovaQ2otPCUMfCdncSyW+\\\\n3AH1iVvI9zGN2g\\\\\\/rHfbouyn3Jiqk7qF8hP2QSnPJXw8O1foN0c13nkvPF9dcJnSEvljiaHOPXd2V\\\\nU3X0Lqeu4vtll4Vkr5Gd0Za+ur3NMcIz7ONLRvz23XryG21Vxnc+KF8mo5LsbfNbSyGnpKX1QywC\\\\nri2niY8FzSdxqHuI5r0c5jjjJPLzZllct3w8zx\\\\\\/6Pfg80zWyX66ul0jU5rYwM+Q0rzB2wei7W8M9\\\\nrMHBPDsr7vVVbGyUwfpY5wIzg8gCN1777WfSE4X7MrNVPFwprjd2txFQwShx1eL8H2QPyXzj7Q+1\\\\n7iG\\\\\\/cav4pN0qqS6l5dDU08ro3xDkAxwOQANtlljjt6PBxXLvZ2ci4k4YrOGrzWWysb3VZSyuhljJ\\\\nzpe04I29ypzHI07jI8loLtXVF3rp6qqmkqaiZ5fJNK4uc9xO5JO5J8VD9XLhnH0WnRHZfpsFzwB2\\\\ns8U9m11ZXcP3qpoJm7Fgdljh4OadivVvZb\\\\\\/pDLnHWMg44s9LV05w0V1sjMcrfEuaXEO+GF4ufSON\\\\nRjB3x0XvzhL0Y+BOIOzqwev2NsFykoo3zVdM5zJXPLcknfCznD12x5eef6WVj0dYOPrF2q8LOufC\\\\nVwo7rUiJz6eKZ+NEuCGiRnMDK84Wf0Drhe7w+48YcTCeSokdNUigb7b3Hc+04Hr5LF370U+K+Aa9\\\\n157OOJKqGeL22Qid0EwxzAc3Z3uK6Z6Onpow8Wyx2HjkRUFwa3DLsAGwPx\\\\\\/vejCfHkssuPLi8N+L\\\\nn34UfpQcPcLdhfZRTcJ8LQCjul5mb37w7MrqdoJc5x5+0dI+aw3od9h8\\\\\\/F\\\\\\/E3+slfTvjtlAf9nc8\\\\nYEkviPEAH5r1vfPR34P424wl4su5nvtTOxvcx1M3e00bdsaGcsfTddFs9ko7DQRUdDTR01PGMNZG\\\\n0NA+AWfVrHUdM5NT+VTxfb7eeBb1R10zKS2Ot80M00hw2OMxlrnH3A5XyhuTKaKCop4nxuja57Gu\\\\nZ9l+CRke\\\\\\/GV9YO0Sno6ngLiSG4TRU1DLbaiOeaY4YxhicC4nwGV8roOHnXow2610\\\\\\/rM80gigigbq\\\\n1vJwNI81rw+1cV720xwD2ecQ8d2fiWW1RxeoWSidWVdRMSGsbgkMGObjpOPcocLNMEfk0fgve907\\\\nObX2Heizf7a1sFLXTW1wrak4a6ed7cYLubsZwB8l4OeNLQPABbceXVarjy6raac7AOywgcRUzg8n\\\\nPLx7srcTnSxx8ASsNIdIa\\\\\\/GwO\\\\\\/uWn8tMvMOOw4YO4Oyap6r1KuhPXOAT1CcznB6JmeITxlv3hu0+\\\\nBSqcpfMbtrw9rXDcEZQVFaL231FgkPtt2IPRBDSXc8vsl67bWAYDPoiNztrSfs\\\\\\/JcSqeIn0z2B05\\\\ny7fmn6e6vq2ahMQPIrC5YTvt8r+fw7Ibzbmt5t+Sbdf7cBu5v0XJmmV43mcfim6mN8bdRlfv5qP1\\\\nOOexrkvp1h3E1ubtnPwTLuLraz7wHwXGLhUGkczMrva65VHUX5orBTumdk7jJROXivstcnw77Jxr\\\\nbAPtqO\\\\\\/ju3Aj2\\\\\\/wXndnFEU1fJSmU6xy3VPceNoqKpdBI8h7TjmtOvj+R08vw9NSdoFsaPt5wmJO0\\\\ne1j730C83TcTAWySra5ztIzpyshU9qbGA\\\\\\/o9\\\\\\/inMsL7T0c3w9dSdqFrYB7ZOPIJl3atbARgn5LyD\\\\nD2sQyVTY5Y9LSdypF67RaenliFIO+1eafVhD6Oa+nrCbtatoGxOfcsJ2u9o1DfeAbpQsJ1va0tz4\\\\nhwK4B\\\\\\/2iPDHGSHSRy3UZnF0nEdDcIBGGuEZ04U3l45NjHDmmUumHkqn0dydUxO0yxzd4xw6OByPw\\\\nXpPto7aaTi7s+joKUOH9KPicQcey1pEh+rQF5ea980IfIcvdufepttrKmte2KWUvp6VmImkD2c81\\\\nrNV7PJxXkywynpMmORyws5xFKP0UPUnUfgtHKcjyWTu8nf17+oaNA\\\\\\/P8Vb0L6PWWAMhdJjdxU3sb\\\\n4ui4G7WrHfZ\\\\\\/6iir2mXA5McCx30cUdPEIKaNgG4aM+9Y7uDHXVbcj+sKnKb7FlN9n0k7QomRcX8N\\\\nV0WJKSpme6ORu4OuI4KwklHxDwh2z8KXmMMk4QdVvpj3Yw6mkqB7TXfsueNQ8yU96N3HMvaZ2UNo\\\\nq0sluvDT2wEjm+Eg92456+yRt4LoN6oTc+z\\\\\\/AIkiYNU9PAKyLyfEdY\\\\\\/4V5lnRno8tZcXfzHY288H\\\\nolYAUCw3IXmzW+4N+zWU0dQPc9gcPxVh4JuJVMsVsbdjIac97OTK7S32C4AAud5nAHmr5rWhgDWt\\\\n0ctI5KPnBzgJqghbbaVsERc6NpJ9s5OSSSc+8lazO61WGXHu7j5ddqHA83Yj26z2u4YdbnTPmpZf\\\\nuuifnQd+ozg+5dr4c7O57nCyorZ2wQvAc1jN3EHqur+mj2IP7V+BIrpZaGWr4otTmiCOHGqaJzgH\\\\ntPjjmPivItv7U+1js8o46C4cOyzRwARt9eoZdQA6amkZXTx8sk09D6TmnFNZx6Cn7HOGKxzX1dH6\\\\nzKBgPkxlPDs5oLY+CS3O7l0Dg5sT92bHIHkuA\\\\\\/8ApY8VwYE\\\\\\/DFGH9NpW\\\\\\/iVHrPSU4\\\\\\/vURgt\\\\\\/D0FO\\\\n+QFokhppZHjPUZOM\\\\\\/Bafqa9vQy+q4bHsG8emZwpwhUNoLzbbiyvYNL4qMRyBuOu7m4C7dwTxTBxr\\\\nwvb73S01TR01bGJY4qtobIGnkSASBn3r5ndjnY\\\\\\/xV2mdqdsm4ps95FqmqBJXVjqZzfZ58yOpwF9Q\\\\naRkFvpIaamjEcELBGyNuwa0DAAXNllHz2eMuX4RLe8NHmo2nqUNRccoysLdtMMemAgggpaC2CAOU\\\\nCMoYQBoIIIAuZ3Se4ZOdEgyx3MJaInG\\\\\\/VOdqVm4kCNsTAxjQ1o6AL55enHe7pwh21vmo6yeljr6C\\\\nCQdzK5mS0aDnHuX0KZUAjDufivJ\\\\\\/+kC7JTxRwPR8YUML5KyyksqNA2MDjufgcfNbzKeXLJ05d3mP\\\\nh\\\\\\/hJ9XirvMvrMr\\\\\\/aEIcSN\\\\\\/E9UntH4Dn4ktUIt8DWT02SxgbjUNttvctj2PdpvBDrZRQV9SyC76Qy\\\\nQVgwzI8DyXdrdcbbXRh9JJSzx\\\\\\/rQua4fRd\\\\\\/VjrUfT8eOHJj2rwRa+zi93K4NgkoJaaMOAlllGloH\\\\nXC7vwf6N9P2g1YoLRRls7GanSd5gAcsnK9ET0VFWRls1NHI09C0LE8TcZDsLhm4gs1WyCpeDGKOb\\\\nDmy55DHPGeaXVJC5ePowtxvdgrn6AnGNFdqCWmkoqqjfPGybTN7cbdW7iMbgL2mezya10MENG9s8\\\\ncEbYw07HAGNljfRh45497U7NPxLxUKWktMgMdDBT0\\\\\\/dmU5GZCSSdI5BdzJDB7S55yZYXs+X5N8mX\\\\ndwvi+O8RUE1us9E+pvtUx0VNGfZbGSMGR56Nbzz7kfYV6NFg7IOFp6Orhp71da9oFfVSxBzHj9Ro\\\\ncPs\\\\\\/iuzPhY6ofOGNEjgGl+NyB0R935rHl5byVtxccwitsNhouGrZFbbdEaeiiz3UOokMBOdIzyHg\\\\nFPc1OgYSXjquduzXaHa23ngPiKgkGWVNuqIvnG4Lxv6HnZ7JeuPH8UTNjjtdjjd7T9tUz2EDHT2Q\\\\nS7PuXs\\\\\\/jmr\\\\\\/o\\\\\\/gu\\\\\\/1XPubfUSAeJEbiFwO1cMQ8B9lFq7PaSrlpb3eqN1fc54nATRRkDVg4wCSWsG\\\\nRy1K5l041eO7vGe2X9Jnimm7Sezq73GEl1joXCK3kPOJ5u8AfMQNsDADc\\\\\\/tHqvHUx3OF6B44nisH\\\\no1WO2GVrZaiVsTGPd7bg2Rxdt5YGfevPszvaO66ODxXbMZjJIh10gipZnHow\\\\\\/gsa4AsAPIham9ya\\\\nbdLjm4YWXduuhN8mozhuk\\\\\\/dOMpe5SHHQ8Hk12xSicIKG3RPDiWOwDuR5oJ0OQTZ2PoJf7g1t1DNX\\\\nss2V1wzVmWE6jhuVz6sndPVyyE83FaDhep1ZbK\\\\\\/EYC8fk494aeVXQY7g1szxnIaM5T5qRXW50gIH\\\\nPCw01xMLZGsOdZwFsrXaJP6DilcHAkE4WVwmMOMjfqqsia0yML4gdnAcgqS92+WqZHWU7hqHMhbe\\\\nrrWU8RZLGZGnbAC59xHVmheXUxdGwnJYRhTOOb3DrI3OSemrRUsy2dnPzVRxFcG3VrKoYEo+1hXN\\\\n8uDagRzNxqAw4LHyvHrbw37LjnHgujDHcm\\\\\\/QjZWio9a4aqW55NwuZ1WdTxvzK2thqhAyene7DXjZ\\\\nUV9tApHCRv2XK8LMbYq92QqW+0fFFQOMcrdy7B2yU9OzDyOaRSsPfD3rbzERbVb3ywFxK0PZhb++\\\\nqamV+CxoHP4rM1MunRHyyrC13mSy00rYzgv8FwZzK46x9hDvFIyluVZDF\\\\\\/VslcGjy5j8U5ZqcNo3\\\\nSY3e4n4clGqIp2OdUztLWy7jPPPmr2Cn7imij\\\\\\/VaAvZ45ZjOry9DivVIr6x\\\\\\/cwyPP3QSsjDEZqlm\\\\nrm85K0\\\\\\/ETi2mDBze8D4BVFrp9dSXH7oWk8uj2nObgLIXKMxXOpxyJDvotlI3Cy1+ZpuGcbFgKQro\\\\nPo7dqk3Zd2i0dS52q115bSV8Rxh0bjgO97Sc\\\\\\/Ne46x5svaFZqYvzaLoJaOQfdcJI8s+ox8V8yNWl\\\\n+xwF9Cexapf2vej1YnMnD79bAafWTu2WJ5MRz5tDD81yc+PeZIy7bny652S1BfwHbKUv1PtwfbX+\\\\nIMD3RYPwYFsmrk\\\\\\/YZe\\\\\\/X5eL6V47qoiurqmWnPOJ8rQ57T5h+oLqwOVhXKU5EQgAjIwkDQGU1PSxT\\\\nbSRMkHg9oIUlEW55oCpk4ctcxzJbaR58XQNP5J2G0UdMQYqOCPH6kTRj6KwLclGG4KAbY3Aw0YCW\\\\n1uOfNHpBSgMoAkoHKSggFE4RakSMc0ANSGryQciQCgcojzQ6IkAEEMIIAnAkbLPcYcDWXjq3PoL5\\\\nSOraSRpY+Hv5I2uaeYOhwz8VokRGUB5n4o9AjszvIc61tuNikPLuKl0rR8JMn6rn9f8A6P2426Uv\\\\n4e46mp24wGzRuafm0r2sWfFJ0+W6qZX5E7PETfRM7aLW3RQceU72DYap3DPzBSuFPQz45quObVd+\\\\nObhQcR22mmEk1G6seO9aNw3ONhnGR1C9uaPFDCfXTtuU1abtkUVtttPSU9LFQwxMDGQQAaIwOQbs\\\\nlU9O6IufJK6aV2A6R22cZxsNgd+gGU4GgDxTNw9ZFBUmj0Gr7p3c959kvwdOfLOFNtqJjJ4Po1nu\\\\nAJuIp+ELdJxbDBTcQua81cVKQY2nW7SAQSPsaeR55WgCShhJeNkpE44CAq79RQXK01NPVOc2lc3M\\\\nunqwEFw9xAwfIrgJpRfOK4+NwJHVdwjNPIzVlrabOYgG9MYGcc9RXbu0APPA\\\\\\/EDYy5r32+doc0ZL\\\\ncsIyPcDn4Lh3DxrbLWw2Oqqoq6COibJTzsj7t2lpDMOAJB6HIUZfw7fpsZbbXFPSb4Osli4ftlXR\\\\nUQhrZ7hkyGV7sDS4uDQ4kNBJzhoAXnKYblelvS5qtNHwvT9Hy1Eh\\\\\\/utYP+ZeZ5ftLu+n\\\\\\/Y6M5Jl2\\\\nUvE0wjomgnALxlZ\\\\\\/UCMg5Ct+KJA31dh5Ek4xlULiBlzA5p\\\\\\/dO66HPle5yRutuPqiY4uaD1SIqhry\\\\nGk6XeBR\\\\\\/Yl8Q78UtlueS0EPignpWnv2j4cdXapD7DTvuFHljZC5rIX+0DggLqM9jbIwMjGhvXAWG\\\\nuNtZb74I2xuLc5Oy8yXd7vFqXwvZ\\\\\\/wClLhG2b7DSDyXXRFHDAynDRpAwAsdwpTNbK6VrC0NHLCuZ\\\\nbnKysc4sJjbywFzZW5Ur57It5s7ISZWgOLd8YWQ4vt9FcrRI8xtZK0b7LQN4sdW1z6V0LtzjdpVd\\\\nxLZpH0+qNjyHcxhHeWHbtwG50b4S5uDpVDJREyg53XXeMLGY6GLu4HZxuQ0rDM4fqpHH9BJjx0Fd\\\\neN6oJWce79M0csdQplxY6st7SDnRspr+GqsPJFPKf7hUyg4frXtfG6mlwd\\\\\\/sFGU+F9nNK+keH6mj\\\\nmotNGWy+1zyugTcI1nrRaaSUtzn7JTF24LqjG2SGllaRzGgq9+kslUN1zRkeHVWdot\\\\\\/fu72QZaw7\\\\nDxKI8OXCOduumexhOnU5pAWjho2UtO2NvILo4OL8uqoyqquEAqRHERnVIByUmSPTlOGLXXxjmGtL\\\\nvyS6hmhpJOAN8rqyu67\\\\\\/AKeaw2xt8n724GIcom7+8oWmHTDI7HN2E1VgSyum5ucckhWFtaHUYwcn\\\\nJyso6cabkbusvxKwtqYXgYy0tWtlbzWd4lpy6kbKDgxvByfA7KqvLwzWnfJXq\\\\\\/0Ee0KK13+78JVD\\\\ntLbi0VVM4uwBIwYLceY\\\\\\/BeTSJw4glgHjhXHCHE1dwRxBb75bnD16hnbPGXcnEHkfIjZZ5zqmmWW8\\\\npp9M6C0w8Ldtc9RAO6h4joC+Vg2BnhcPa95a76LpjTlcoj4speOeGeAeNLfpIfWU+trCD3QnxFIw\\\\n\\\\\\/uucB8F1Rp+YXBXOealJLTsjyEgJyJKO4SUAEEEEAM4QCCCANEgEOSACCCCACCCCACCCCACCCCAC\\\\nCCCACCCCACCCCACCAQQA6oIIIAjukuO\\\\\\/ijcdIwo9VUR0dNLPK7TFEwyPPg0DJSDmfE3bBDa79xHQ\\\\n+qtqqG2QMgIDvamqnjIjA67EDHiVybhOgrrbxFQUlwm76pitLnP\\\\\\/AGNUwIZnrpG2fJDhvhOWS6XT\\\\ni68VstU+4Vs11p6FgxHB3h1M25ueGkAeHRbin4MrLdFLf7piGuqw2IU5\\\\\\/sIhktb798k+PuUV6fFj\\\\nMNb815r9LeqD7xwzTg7xU88hH77mD\\\\\\/kXniQ7ZXS+37jWl4w7Rq0UMzZ6S3xso2ys3DnDJdv13cuZ\\\\nvOTuvR4Zrjmzyu7uM1xI7VXQDwaSqwjIU+++1dAPBis+G+GP6YJmqNTKUctOxef5LZy5ZTDdrNPj\\\\nbIMEByLQ5n7Q8DzXRK7gW11bAGRupXjk+F2D8c81k79wvWWSJ87CKulZzfyePeEaYTmxt79lN3zO\\\\nrgPI7IJt1M2Y6pM6v2eSCW23Vk+2DaK1D7sX0Ud9hsU0mt8EDn+JAXJncTuY4NNQcnkmJuN4qd+l\\\\n9SWu+KxnT6r57qy+HaI6C0wNwxkLR5ABE6ltJBBbDv7lxuDi4VMLpGVJLRzSYuKmytc9lSXAc0ax\\\\nTvL4dfba7G12sQ04f44GU46ntD9nCAjw2XEHccQufp9a3B8E7LxBIym7\\\\\\/vz3fiizGeaN5fDsNRbr\\\\nDLgPgpnAeICaFp4dA2pqQf3WrhrOOIZ5NAqzq8N0mfi8RyFnrDiUfjPad5\\\\\\/DuTbbw8DvTUhH7rUD\\\\nQ2Bh9mnpGj91q4azicyRl4qDgKFVcaMhc0OqXZKe8PGz\\\\\\/P4d8dR8O8\\\\\\/V6XPLZrUTqbhwMw6Glx4F\\\\nrVwIcVsfsKk5xlMVfFbKePU+pKW8PG03r+F36QtxtLKe12u2U8DX6zUSviaAeRa0HHvK4fKzPRXd\\\\n3uD7tVy1D3F2o+znoOiqZWruwmpp0Yyyd1fDCDUzu8AG\\\\\\/n+aj3WPVTlgyC\\\\\\/Dfrv9FYwt\\\\\\/SVA6hw3\\\\n+AUK4AmeFvPm5Zf1PVl6eHbH3Cj9Uk2B7p32fLySLbN3MxiccNfuM+PgtBWUzJ2OY8ZaVlaqIwyu\\\\niJHeN3B8uhRljq7ieDl32vlbStVZc6b1ikmjG5LdvfzUykqxVRAE\\\\\\/pWjDgimBGUnf5jAOe142IyN\\\\niOoKjzShgOpwCtrxbW09S46Rpky4EfVVb4gM7BLaJa23Zx2ycUcBwS26gr3GyVMsUktHPlzWObI1\\\\n+tg+672eYX1Qs91ivdrpLhA4PgqoWzMcORDgD+a+PDACcdCvpX6JXGcfFvYtaGiTVU20uoZgeYLd\\\\n2\\\\\\/4SFy8uOpuMc5ru7Y07Y6pSbYcuCdxtlczMY5IjzRjkiPNAEggggAggggB1CBQQQAQQQKAzHaBf\\\\nKmzWKodRNeasxPdGIxkkhpIA8STsrXht1W7h+2Ory41xpYzPqGD3mkasjxzlTpYI5iwyMDnMOppI\\\\n5HxS+qSrZqSDQQCCaQQRFGgAggggAggggAgEEEAEWMI0MIAIuSNEXAIBL+a5x21cQS0dlobHSH\\\\\\/b\\\\nL3OKYkHdkA9qV38Ix\\\\\\/eXRSVz2awx8U9qNXc5yJaGzUcdDE3P\\\\\\/rDyZJfkwxD4oVjrfdU8GcMy3W9e\\\\nuVcEtPbLcQ2mhewtbPKAPbwebW9OmfcvP\\\\\\/pn+khT0rH8F8MV2uuacV9dTSf1PjG0j73Q+HJdo9Jz\\\\nt0oOxngOpZFOBxFXwvht9OwZLCRjvT4BvPfmcL5cXG4S1tTLPNI6WaVxe97jkucTkkla8XH1d66J\\\\nlc8uqrayHNPK7q6Qknx5KeSSq6y7UDD+sSfqp4IXdGuPhX0lnfeeIXAj\\\\\\/Z4wDIfEeHxW+hijgjay\\\\nNoaxowGgYAWc4TGa25O6amhaYKo8zmu86BCyXHVxEcEdG04Mh1P9w5LVSSCNpc44aBkk9FzHiC4i\\\\n53SeZhJjyGs9wCdHFj1ZKwnHRBDlsgpeg+isDO9kpgT7RaCs7fxouD2q7sZfJcACctYOSz\\\\\\/EUuu5\\\\nyY33wvH4v3aeMDa91PRGJmfb809Ya7u5zG8+y7ZRqKjNTE7I3HQpgU0kby7OHNK3urLBKkXimNHX\\\\nOcwey45C0lhLrja5YZOg8VEdBHXWsSH2pWtyk8GzO9anicfunZc+V6sf7KrLXSiktNxLwDpzlSjU\\\\nte+KpI2IwVY8WY1OGNwqe0iKpp+4cfcnu3HdP+TjbgaOuDHNzDIUu7UDqjRJT+0OZCk32zOFsZJB\\\\n7T2YVNbq6rbUMhdtnbdY2b\\\\\\/KHLoqeGWhqInPGQ8YSLi8TtMLBk59p2eQU26zOlxC4YmG4Ph5qD3e\\\\nlv1J812fT8HXZnl4TnlPSM8Y5N5KPIMqY8EjZR5G817UYK\\\\\\/2o6x36soA+IUWrGapo8GH8VNqIzIN\\\\njgjcHwKhzyd5PE\\\\\\/GnLHAjzUWflt045747jUCZmyy\\\\\\/EdKTUwSs2e4afgtdKzJWcuzg+pwNwwYHv6p\\\\n566WfFj1ZKB8stLK2RrCSOZb1Cs4ayKti1MOCfunmokzcqnqJXQVILdm49rC55a9Tq6e9SuJI8QR\\\\nO6tdn3hZ6QbahyVnUyOqA0ueXAcslV74i0kfdPJC990cAl3sjJPIL2R6C9XPYr9fuH5Zy+KspG3B\\\\njDybIxzWOaPg8fJeUuFbY2vuep4yynxIR4nou2dkvF7uBe0SxXYu00\\\\\\/rApqnzikOl3yOk\\\\\\/BRnj1S\\\\nubkz76fQuMgc0806go8b2vAIOWkZBCfacAY5LgItERkoA5CNAIOyCM80SACCCCACCB3QxsgAgggg\\\\nAggggCHLKGcdEeEEAQQQ5IIA0SNF1QB5RIFAIAIIdUaR6F8ED80EPimQJDj9EonAI5ppxSFQr3d4\\\\nLFaK241J0wUsLpnnyaMrl8nH9i7GOzVt94pr2009we+vki5yzTSHVoY3qQNLfgqb0vuMavh7szho\\\\nbdUinrLrWMgLi0OIiaC95wee4aPivEnaVfrp2kv9b4hr5bpXQw93DK8BgiAyQGtaA0D3BbYYXObL\\\\nemO7a+1i4dr\\\\\\/ABzcb9XF8cUryylpS7UKeHJ0MHjgcz1OVzoSEg55jZLc\\\\\\/X7Wd1HkBB1jnjceK7Jj\\\\nqajrnjs1dqP+wQ\\\\\\/uqaDn4KDa9qCDzYFMCtrPCx4Qb7VeT1lx9Fo+QWe4UwI6zHMzb\\\\\\/JWtwr47dSS\\\\nTyn2WDOPHyTeXyd86z\\\\\\/G95dTxtoI9nyt1Pdn7ueXxWIJyU\\\\\\/X10twqpJ5XZe859w6BRxzSdnHj0we\\\\nPigtPYuFW11vbPMMF5Jbk\\\\\\/dQVSM7zYy6e5+GZCWzTnksvXZqLm4jfLls6ezVdFSOjEL84zs1Z6ls\\\\nlc64McaZ+NWfsrx8J3tebU+SNtup4HEY1c01eoY3UglhwcjJwpV\\\\\\/oKyfu42U8mG9QFBpKOucDA+m\\\\nkI5btTmN1sRX2G4GOo7l7vZccbq5ZC223dj24LZBjI5Klr+Ha+jl1theQdxgclaWuKqq42tlheHs\\\\n33ajPH3FbNcV0xFUHAZY9uViHF9FWHGcA7YW64gpq+Wohi7h+kj7WFX1todRNYZaV8jzzIblLGWT\\\\nwrZNmuE0ge0glpGcYTFRWwVEx\\\\\\/RlszOQxunYro6la7TRS\\\\\\/wKCyr9dqJphGGA4ACrj4evLvNIt14J\\\\nlc6R7nOOXHmUw4YCkSFMuGQvYkkmozRpSo7xkKVM3IUVwwVpCRpGnxVNdaZxLZ48h8Z3x1HJXch3\\\\nwo0ozlFm4cumcqK2eRukBrf2hzVVM3GR1V7caMxZkYMtJ3A6KmlBHmua79vS4umzeKslbjKoZ294\\\\nXO8VpHwvlcGMxqcdieipZqSSOcwEe1qDcjqqw7d2f1GXjFPo+DnVNphqYpSyeQFxY\\\\\\/7P+Sjt4PuM\\\\nkmHMjjb+uXg4+AXQaem7ilii\\\\\\/UYApVvofXKyOM8s5PuS7Ix5MsZqIlNwDDbrPFUUbHCtEYMrR\\\\\\/bD\\\\nHLHj4KtjDZWuG+lwII6j\\\\\\/NdP0FrQByHILMcRcPkPfXUkeZCMzRN++P1gP1vxST7ey+wTjQcb9mVp\\\\nrHkmrpW+o1QJyRJGAN\\\\\\/e0tPxXRmkLyH6J\\\\\\/aJDZOIqnh6qk0Ul4Ilp3Hk2oa3BB8NTQB72heum5Hw\\\\nXBnj01vKfzhGktw7qlLNRJQSiMoaUAhGgiygDQyiRoAIIsoDdAGiPNAoc0GCGUOSPqgaEj96CCD0\\\\nARFGggaEgjReJQAAQKAKNICwhy8kZ80nKYJed03I7YjO6W47Ll3pEdo7ezXsvudfHMY7hVYo6MN3\\\\nJkecZA\\\\\\/ZGT8Ep3o8PLHpQ8fxcadpVQylnbJbrTH6nE5rstc8HMjh057Z8lyS38OT8QuJe51PQHZ0\\\\nmMOl8m+Xmrm08MvqHtnrx+h+02F59px8X\\\\\\/y+a0+zRjGANgAvSxnTNRy5Xdc7vfYzZqyH\\\\\\/YC+3zgb\\\\nYOprj5grknE3CVx4XqXQ1sJDD9iZgyx\\\\\\/uP5L067ksX2jUEVypIoJhqY9rh7vNVr4XhyXFyO2EG3w\\\\nY\\\\\\/UClAqut87WN9WJw+Ilm\\\\\\/XBUzPvT29THvJVpw3VR04uLnvEbWPDi5x2Aws3xJf3Xio0sJbTRn2G\\\\nnqfEqsu0pFxeA46SNwDsSopdlG9ueYTruVOZUm30b7hWRQRtJL3Y26DqVEa5bPgi2kMfXPA9rLI\\\\\\/\\\\nzKByZdOLVsaI2BjRhrRgAIJL544zh72tPgSgr1\\\\\\/Dzn1WNstB\\\\\\/s2YSRaLKHZEUWfguEQdoomLf07x\\\\nq8XFT6jjN1PFrMz8eOpce8Z7cWsvh2d1msshyYYj8kh1isvSGL6Lhre0cOOBUP8A4kJu0UxjPfv\\\\\\/\\\\nAIk\\\\\\/xL8vh3KWx2WXYwxn3AJv\\\\\\/Vywt5QxtPjgLhTe0wPdgVLv4ko9orngkVDtv2kanyW8vh3N\\\\\\/D9k\\\\nkI1QxnHI4TUvDVim+1DGR7guEHtNw\\\\\\/Sal38Sci7RXSuw2qd\\\\\\/En2g3l8OscTcJ2SGw3GSnpou+bTy\\\\nFmw56TheUKNzRJK3O5wV025docjYZGesSSktI0NOc58VyV1QYa1znAsydwtuOdlYW+4sn+9Nv2CU\\\\nH6hkHokPOy2aGHnHNMOwRnKfeMplzcBViEZ4wVHkA3UqTlhRn7ZVBFmAIIKpa62ZcXQ4B\\\\\\/VPJXsj\\\\ndsqLI3KVkvlWOVxu4pKa3GnJe\\\\\\/DnnYY6BRqm3MmuFLKTgNkaHHyyruRqhzRAggjI80rO2odytu6v\\\\nHtHwV\\\\\\/w5QCOAznm\\\\\\/Ye5ZqyzGqjEUpzJG4NcfEdCuhRwthiYxoADQAFhZpr5MOYmnNPgpThsdk04n\\\\noFJstdbJNQ1QuNrL452SCbRGcFrwch7fPYbdV7O7Eu0lvajwBQXeXu2XFhdBWwsyNErXEHbpqADv\\\\nivKxGemPcrPgbiis7NeLGXygEktHPhlyoIztUM\\\\\\/XDeWtvQ\\\\\\/BY8mPVF45ae12kZGOSdB2UChr6e5U\\\\ncFXSTMqKadgkjljdlrmnkQVMachcbc4gkZS+aAQQgjci5oAYQQROOBlAE4hu+cb4R6vHZUvEPC1F\\\\nxNHGKh09NPGcx1VHKYpW+Wocx5HI8lQSWLi+yOb\\\\\\/AEXeYLxTg\\\\\\/1FyYWSe4Pbt8wkqSNznx5ItSx1\\\\nDxLxO+oFPV8MSQnrOydj4vnnP0SL7x\\\\\\/Pw3SiorrTVCInTqhifLv56QcIaTC3w2mpG0rj8npGWoSG\\\\nNltuUsg+7HQTk\\\\\\/8AApdN2xXC6s1W\\\\\\/ha9SAcnG3vAPxdhHcdFdWygsNwpd+MLpdmvuVmZa7VpJJqJ\\\\nmmYnG2GNzjfxK22olCL2ui0EQ5Iso2CkWQiyiyjYKJ2RZSdSGpIFEpDngDmiL0xPUR08T5ZXtjiY\\\\n0uc9xwGgbkkpg3crlTWqinrKudlNSwsL5ZZHYaxo5kleJe1jjubtW4phudQx8FsoC5tuonHkDsZX\\\\nj9cj5Babtq7W5e0C6SW+31D28NU7gGtYS0Vjhze8dWg\\\\\\/ZB2OMrmD36iuvi49flWGeXolzsJKGehQ\\\\nzgeK6WJLuSzHGkZ9Xhk6AkLTuOeapeKIe+tMpA3YQ76pnHnK\\\\\\/wALqG81bd24eXNPLY7ppl6qGs05\\\\nacciRutJx\\\\\\/RYdTVTW+Mb3D\\\\\\/D+axbtiUtPQ48t4wJ5HSyiRxy8uySlA4TZOxHijjdrG+xHNNptMoa\\\\nR9dVRwRj23nA8vNbuWujslNFQUje9kjGCXfZb5nz8lj7HBUtmNQx5hjLdIIHtH3Hp71dDDfEdVrh\\\\nhb3rzebl3dQqRoneZJSZZDzc5BESgujUce69ayT6GtAyHN5Bay3VAudu7sn2wFnrlHG6pLWjDgVN\\\\nsDnwVPLLTsQvBzm8dx0RCuFNJQznVkAnKfiLKygeAcuAWgvNtFdAHD7WFm7Y31OaWF4J8lMy68f5\\\\nLWlIx5jqGt5e0reICKU6xlrhyTwskLZu9ky5xOdPQKQ+nbkFjiwjbZdX6OWXlHV8KKqs8lRU6mfo\\\\n2HqVJprXHStyXF7\\\\\\/ABPJT5BIDtJn95qjufI07sD\\\\\\/AHHC3wwmKLaDzgYzsFVXSldMGyMGSBgjxCnu\\\\nmB2e0sPmkuIIO+VsSkoa3uwI34AzsfBWGSW+OVAudv8A7WMEDqAmaW4ugaGP3ZnAPgmFi8b8kw4c\\\\n06JBI3IOQeqbI5pwGJBlRpMlS37AlR3bjwVbCJJywo0u2VMe0AlRZQHJhEfv1USUFTnsx1UWQZQD\\\\nNLUuo52y\\\\\\/cJ0v93j8F0ix3FlXTNjLgZWjx5hc1fsMK1sNe6DSxp0yRbtJP2gs841xvp0QjfYpBbl\\\\nNW+vZXwh7QA4fab4FSHBc\\\\\\/hZhzd0TRpPindPNIIweSQdA7IO0l\\\\\\/AtxNJWzH\\\\\\/AFfqXF0rS0uNO\\\\\\/8A\\\\nXbjfB6j4r09SVUVZTxT08rJoJGh7JI3Za5p3BBXiAOwcdF1Lsi7W2cIOFpuz3Os8rx3cxOfVCee3\\\\nVh+nxWHJhvvG2OXp6U1+SMSbKOyeOWFs8cjZIXN1tkB9ktxnOfBc34m9IPhXh+plpYH1F4qo3aHt\\\\noItTGnqC8kN+q55LfDTenUC7OUnWcrhc3pS0oJ7nheueOhlqY2fzUN\\\\\\/pVSNk24Qe9ni25tyPh3f5\\\\nqujL4Lqjv5duhnK4TT+lXb3Y9Z4ZucHj3ckcg\\\\\\/EfgtPaPSL4JusjWSXCa3Pd0raZ8Y\\\\\\/iwR9UrhlP\\\\nMPcdPylxyaHg4Bx0Kp7TxLa78zVbblS14xn\\\\\\/AGeZr9vcCrPUOpwpNN9bjIw6Ee8KM5wLyQMDomy9\\\\nAOz1SpyaOajvufmiyD5psvCGsJKOZCLIBSC\\\\\\/PIJuapjpoy+aRsbBzc9wAHxRoj+pEXjfqufcR9uv\\\\nBPDEroqq+Q1E4\\\\\\/saMGd3u9kEfVc9uPpaUoe8WvhmqqY8+zLW1LacHz0hrirmGV8QuqPQJeiMgbuc\\\\nBeSb76SXGd0a5lHJQ2Zh\\\\\\/wDd4O9eP77zj6LAXTjHiG+uJufENzrQfuGoLGfwtwFrOHK+U9ce4bjx\\\\nfZbQSK67UNIQM4mqWNOPcSsvW9uvAdCS2Tiakc4HlAHzH\\\\\\/A0rxdoh1au6aXeLhkn4lL74gbbe5aT\\\\ngnyj9R62rPSU4Hpw7uKuurT07mglaD8Xhq4t229vw43hpbTbKC40dlAdLVzSsbmZwI0s0tcTpG5P\\\\njt4Ll7pHEbn5pBdnCucWMu0XKihrI62Jk0LxJE\\\\\\/drm8ijOyp5JDZ7gHjJpKuQAgcopCOfud+KtNa\\\\n2QUTncJOo+5FqRE5TMrOUxVQipgkjP32kJ3KbmlbBG6R50saMk+CA5hcKNpdLTzsDmnLXNPVcxv9\\\\ngms9Q4hrnUrj7En5FdXulS2qrppWj2XOyMqmudZT0sBE4EmrlFzLvgq1tWGdwcqDS4gAEk8gOquL\\\\ndZQ14mqGhzgPZZzAU2K3wRVUs7Ywxz3ZawHIYPAKSCtcMPdTy89z7TwVjZApOrblhDUt3JsaCLUg\\\\nmT2tV29ssPfs\\\\\\/rOahW+v9XqQX7b7q6jsl4onaZKKZzOp0FR7rwxWMb3rKOUDrhpXidPqurelvHc4\\\\nJIxuPcq6WKE1Mk7WgZwA78VW0NrlJ1SvcwdBlT\\\\\\/V2xs08wOhOVXFw9OXUjLLcMyTMzu5qZfMwjY\\\\\\/\\\\nRSHNbvhoz7k07YbLuZorp2F2NQz5pDiDuMEJ97QTyUeSFpOQC0\\\\\\/s7IhESDIxjIPRR3wMGS0aT4hP\\\\nHXGd8vb49UkkHkcphGY12SCchVlfbWuDnxDDuenoVcOao8zXBuWk6gc48UBmxNJTOOklh5FpU2C4\\\\nsmGH+w76KZUQR1rNxpf443HvVHV0EtOTkFzf1gEgtSQ4ZGCCmXNG+FSiplh+y8jyynm3lzR7TAfc\\\\nmEuTKZeAOiL+kaeTm7SfNDvWSfYe13uOVW+wRZcgnHJRZOSnSBRXhMIj2ZGybDnwyNewgObuM\\\\\\/gp\\\\nTm7KM9u6fk2mtlwdCY54nYa7BcM8x1C2FPNHVQtlYdj5rldFcfUJAx7XPie4AaNy0nyWqsl6ipqp\\\\nrO89h2zmO2I88Lmymm8u41ZGyQ5ufenA9srQ5rg5pGxHJESPcs0mOuUAQAQeqU8Jp2MIVKKPi7iq\\\\nF77BDfquPhYU4LqBpbguc52Wh+NYbtnSHY8k0XBrNLQGsGwAUK2ymc1cpzpdO4N9zQG\\\\\\/iCpD3c8o\\\\nknpWwdL0zsmXyZ5lE8piQqyOuec89k2+UjHgmnSYGfBVFw4ipqRpAeJpP1WHKQXEE\\\\\\/qtQJ4HPpp2\\\\n8pYHmN4+LSCt5YO3XjLh+SINu\\\\\\/8ASdIz\\\\\\/wBVuEbZM\\\\\\/8A3AA8fElcHrOKaycnQ7uW\\\\\\/s8\\\\\\/mq88TVLZ\\\\nNJrTq\\\\\\/V17\\\\\\/JFwl8q6rPD3Bb\\\\\\/AEqOHn21r663XKnrw32qeGISMLv2X5Ax78LO3b0rq2Vrm2vh2OA8\\\\nmyV1Rn\\\\\\/C0fmvJ1Ffb5UPDaeknmH680ehnzP5K7jju1UzFTVRUjeraZup38R\\\\\\/ksv0sVddddvPpIdo\\\\nMzXSQ3S02qEcy2i1gfF7ioVF6WHHVJGRDLbuI5hsAKLumE+b2uAH1XM4bJS4Pfh1a7OdVW7vce4H\\\\nYfAKwaGxsDWgNaOQaMBP9PH4LrrrlV6T\\\\\\/Gtzt4Yyktlmnc0apIAZ3N92v2fmCsBxJxdd+LpRJerp\\\\nVXJw5Nllwwe5jcNHyVGX7FFr5KphjPRdVp0PEYwxrWDwaMJJkJ6prUURWkI6XeaIvx1TWpFqJSGj\\\\nhkPNFrJ8EjpugEwUX5RaknkiJSBurpm1tLJC\\\\\\/YOGx8D0PzVNY+KWXJjGVDe5nOxHTPUfNXmrZc6u\\\\nkRob3XQg6R3nesA22dvt8cohOjZRB3xWYsHEWvFPVPGfuyOPPyJWikmZFGZHva1gGS4nZA2XJI2N\\\\nrnuOABklYq\\\\\\/cQvrnvhidin5fveaF+4idWSPhhdpgG2QftLE3e+hgdBSkOm5OkG7Wf5+SuQrUm63d\\\\ntBiNrRJUOGWszsB4nyWdkkfPKZpnd5Mebug8gE2NskuL3n7T3HJd7yiyV0Y468sblstDlz5pOpFl\\\\nasys7oJqSZkLC57g1o6lZ643uSd7mQuLI+WRzKRLma7U8Ly0vyR4boLJE5OUEjfbR1ztkpGoRkfu\\\\nhYztN4nt9Pazb6NgNVUgZc0bMYDv8Ty+axc97fHGHteSMZzlZuerlrHmWc5kdufLyXmYXHO9i1lP\\\\nJmQgHYJl7iU685KaeuhZpwwmXjCeJ5pst2KvXYGXDITLgpBCaeM8lGwYcMJiWMk5bsfoVKLCEggY\\\\nVwIerVkcnDmEhwyn54dRDm7OHIplru8BB2cOYSCNUMLCJG7\\\\\\/AKw8QkZDo\\\\\\/EFSXtKiuj7p2B9lx28\\\\nij+Qrau1RTkub+jd5clSVlvlpwXOALR1BWseMbKFVRCSF7COYTDGyeKbZUPik1NOCN\\\\\\/epFRG1hPi\\\\nCosnJLYXNPWMqmjBAd1CD24VCyV8L9bHEEK3gq2VbARs7qFU7AT+WyjyZUl7MhMytwrCK8Eg\\\\\\/RWd\\\\nJUNuUJinaHSxgZyOfmFWvSI6h9NMJowHEbFp6jwU5TasbppaG4V1ob3dNM2SDVnu5xnHuPNX9HxE\\\\nalhMtJKzT9p8WHt\\\\\\/n9Fl4qhlTC2Rhy1wynIqh9NIHMcWuHULm0203DpQ5oLTkHyTE0gbG5x5NBKr\\\\n7VfG1rhFJhsv0Kcu8hittU9ux7pwHySBmzs7q00\\\\\\/iW6j8Tn808eqKnaIaWJg+6wDHwSXlOGbecpl\\\\n5ynHHBTLjpTEUd1objXOc1kjIYujQ7n71WR8JyE\\\\\\/pZ2gfsjJWpkcCSmuWUBRjhC3OcHzRuncB95x\\\\nA+QVjRW6joW\\\\\\/7PTRQ+bWgFSSQOabzuQE\\\\\\/IKc7fxSeiInCBOUKAnw5ItWBz3RIJbA87Iaik5Q1IIr\\\\nJKLKTkoZKDKygSk\\\\\\/FEgFZ3RFyJBAGTsgk6kC4lBA4+CwnGzDDfqSQfZmgLT72nP5rdLK8dwB0Nvn\\\\nxvHMW\\\\\\/BzT\\\\\\/IILyzGrCfmuNRNCInyudG3k0nZRsrP3a8ukkfT07gIx7L5BzJ8B\\\\\\/Nayb7Fbo5d7x3m\\\\nunpzjctfJ+IH81SYDAGtGGhAnAwAizlb44yMbdlZQyElDOAtEjLkmSVsTXOccADJKGpUN+uWf9nZ\\\\n45cQkEW6XJ1bKdJIiHJv5qAkF2PJO0sLqmZjBzccKbQlU9vmqY9bANOcboLRwuhpImxBwAb5oJl2\\\\ne5aSeectY6UlgG48vBWGVCo4zGZS4aTqIx4YUrVvhefxzUXld0ZKQSlEpB5rVJJA3TROyePIpkjm\\\\nr2DZGEkjCWQklpUA08JpwT5GMplwTnkGi3PNRposEubnUPqpjmpp7diU7AjZD2gjkUxMwPaR4pcg\\\\nEEgefsO2PvRSEcwAiUIrSXRkH7YOCmZBsnJ26HiRvXZyQ8b+ScuwzF5pRHUEjk7dVDmZOCtLfovY\\\\nY\\\\\\/ryWdkG5BS9hDkyOfLxTTJXwPDmOII3T8ozlRJMtGenUIC5pLg2pGlxDZPDxTkg2Pis05\\\\\\/Ig\\\\\\/FT\\\\nae86MMlJcP1iqlCwf1TDiM4TglbMNTCHDyTEhyq8hJt9aKWXuj\\\\\\/VyHPucrVzlmZN24Vxb6z1mmAc\\\\nR3jPZd+Syzx9tcb6SxO6F4e04cDkEKyqr+K2hZTuae9lc1hPTGQqaSQaee6bpR3lwpG55zN\\\\\\/FZtG\\\\n8cdvLkmHlLe5R5HElEBL3b7Jp7kp5TRO6DpDnJBPMo3HdNvPRBCccnySM7oOOSk5RtQyUNSTnbCI\\\\n7IIonKSgXItWyANDKTkoIGysoZ8wkoZQQZQB3RakDsgDJygk6kRO6CKygThJyggASs9xy7Rw5PKc\\\\nfonxvyentgH6ErQOICz3HUXrPB91jxzgJ+W6BXL7zezK801LKRj+slZ\\\\\\/wg\\\\\\/mqho0jA2ARNa1kbGs\\\\nGGgDCBXZhNRjbsZOAiLklDqrSPUgd0R2CQ94Y0uccAbnKNjaNc64UdM4\\\\\\/wBo4YasnI\\\\\\/JLnHJUi51\\\\nvrdS9+fYGzfcoQIJLnHfwUWkWATueXgnGPLCCCWkdQl0lFPWv0xNIHVx6LUUFvioYmgNDn9XnmUp\\\\n\\\\\\/AZ0UtTINQhkcD1wUFreSCrV+Q9yQSGSNr3bl41H3ndO81Hb7IA8NkvXhc0mppVPB2yIu3TLn6Tu\\\\ncBNSV8UZI1ZI8EEkE\\\\\\/JIdyTDLhE84LtPvCfa9rxlpDh5J7OEOQ1JTh5JHXCkEEJDm7pxybdkoI08\\\\nps8k64Ankm3DZVsIs8YfGWncFRoiTlrty3ZTHjZQ5\\\\\\/0ThIOXIqYDcjQ7II2Kih\\\\\\/slrt3Dmpbtiok\\\\n4DHtf0OzkwgXZne0p\\\\\\/ZOVlpOZHVbCqYHRPb0IOyyFQ0tefFVNBDkG5TJhNRI2NuxccJ+RP2qHXUO\\\\nkI2YNveUBSVtMaZ7mZyQcbKFI8txn5q6vjR60XN3GME+apJevRBjhr5KQksdtzLVYRXaGce0dDsc\\\\niqGbI5bKJLJpO\\\\\\/zVQmskkBBxySrfV+rVbQfsyewfyKyMV0qKcYEmW+BGVIbfmFuJAWu8Wp3vDnat\\\\n5Idz+KXbjm60I8ZPyKraC4R3GkjnjOQ4b+R6hWFkfqvlGD4uP+Fc1btvI7CjudnKXI74phxQoT3J\\\\npxSnFNPdsgvInSJtxREpJKFASk5CIlFsgtgTlBJJwUNSC7jyhlJ2QygD1IakWUWdkAonKJJ1IZKA\\\\nUgSkg4QLj4IAakerdJRakEXqRE5SdSBKDAlVHFYzw1cx\\\\\\/wDLv\\\\\\/BWpPiqvib2rBcQN\\\\\\/8AZ3\\\\\\/8JQV8\\\\nOGRP1QxnxaD9ErKZpDqpYf3B+CWSu2Xs56MnBRasJOQgXITsouz5KjvlyxmnZ\\\\\\/ecPwT92uZpGBjC\\\\nO9d\\\\\\/hWfhp5rhNpbuebilTMFpm6kN8uqs7ZZn1B1uHdx+J5lWVHZYYA0yDvHjHuCsgMDA2HglJ7Ao\\\\nIY6ZgZG3S0J3KRnCAdvuqLR0O\\\\\\/6ygmHVEbDhzgCglsae5u8HPKiVF0bF7MeHO8fBQqp9RO4NY32B\\\\n5qOaScDJaD7iFzrOyVck273kpoPISXxSx7ljsJDXAFBHg\\\\\\/xSmTPi3a7B96Yc8Hoi1YRsLimugcAJ\\\\nefipmoOOQcjxCzYk0lSqS4dwcE5b4JBck5SXDY7pDZWvaHNOQUC7KAS47pBOQUZISTyOyYNH5piV\\\\ngc0g8iMJ5yaccJQIbQQNJOS3ZN1EYewtPIpyf9G\\\\\\/WOR2KS86gmFcx+sFh+2w4Ky9xZoqnjzWrnYG\\\\nSh+Ptez\\\\\\/ACWdvMYFU48s74ThqiUZzgbq2ZH6pRgAe3jp1KjW+m7+oJxlrNyp02ZJ9vss5+9HslPd\\\\n6fFI3A3ack+9Zed2M7La1De9ZI3GQ4YwsTVAskc3qDhMIMzt1DmO2FJnPNQZX5VwI0xG+NvckUcD\\\\n62qhgB\\\\\\/rHac45JMrtRwtn2Y8Oi61tTVSj9FAzSw\\\\\\/tlRl2OTaYKCK2RsNLHoDfttb98eJ8SrPh+ds\\\\nl\\\\\\/oXNdqa5khB+ASKmN9PI9jhhzTjCj8PwCLi2lLCQx0UpLDyB9ncLL+W+nQS7dNOcjc7dNucN0jJ\\\\ne5MudlKe7KacQgwLkguRE5CSTgoGxkpJKGUnKElZScos5RahlBlZQykl2yLJI5oBSGUnKL8UAZJz\\\\nsjyk8kCcIA8lAk4Rct0AchAGT\\\\\\/8A8RZRagURICAUThDOcpBdsAQiyR5IA9WVXX8j+hK\\\\\\/P+4f\\\\\\/wAJ\\\\nU4nZVfEbtNiuB8IH\\\\\\/gUC+HC6M4ooP3B+Ccc5N05xTQjppH4IOfjddk8OajLlWXS8MpmFkbg6XyOw\\\\nUa7XxsYdFCSXHYu\\\\\\/kqDJe7LjnqjZaOSSvqZCXOLiTuTzK1FppPVKRmRh7tzsqSz0xqarJGWM3P5L\\\\nTA7IkMoc0eopGr5oZTIvUmpJHF2iPGrqf1UmSV2Qxh9sjOTyA8UqNoiZpBJ8z1KAU2NjBjSD5nmU\\\\nExLWRxP0uJz5DKCOw09tNnICd77KrGT7BPCZYKWGsEJD4YpftNBPj1UZsxShKlojNRb9IzE7UR90\\\\nqC6TGQQQR0Ktu9TM8EdQN\\\\\\/tdCEgrg7KHeYKbqI3Uz9Ltx0ITRk+qAsqS4GF4afsHnlW7ZA5oIOxC\\\\nywf5qfQXDuj3bzlp5HwRAuiUnXsm9YOcJJei0Dc7PVNPSi5Ic5EBiTS5rmncFRw\\\\\\/W0g7EbFSXNBU\\\\nWQBj8jODsUwYnAewt3Cz14BkljcNyW4wPFaOXfKpqhgFyjDh7BaXNz4opm4YhR0v7RGT5lNaAyPB\\\\n5ncnzUmq\\\\\\/rIx90u3\\\\\\/JR5ggkOUrH3xgirpCBgHB+i102yyvEDNZE2eZ0geSoKCZ+QVAmcN1LkPNV9\\\\nQ7HLwRKDJGvON\\\\\\/Bd04GtH9C8MUsTm4mkBlk95Ow+WFy\\\\\\/s+sDb7fmCVmqngHeP\\\\\\/ILtrsDYDAHIBZ5\\\\n3bXGM7xRbssNUzmNngLO2N3\\\\\\/AOpaQnpFL\\\\\\/yrd1MbZonscMtdsQsZR0bqPiyGN3IRSkHxHsqfTRrH\\\\nOxlMvdlKldgJhzkjgOcmycoFyQXIKjJSSeaInKSThBDzhESiJQQAQRZRawgxlDKQXdUM7IBecIiU\\\\njUiLvJALc5AnOOiRqREoMsk4RZx1Sck7pOtAOF2yTq2SdQUSvqe6ayJrv0kp0N8h1KAcgeaid8n3\\\\nG+w3z8SpBKaiY2GNrGgBrRslZQWxlxwqfip5bw5cz\\\\\\/8ALv8AwVqXZVDxpN3PC10cTj9A5BVxlvsw\\\\nxjwaPwVJfLm5j\\\\\\/V4j09pwP0Tl1u3cxtihd7eAC4dNlnXPLnEk\\\\\\/FdbnGXDP5ogckdUlTbVTesVjMj\\\\nLRuUQL200wpaYZ2c7c5U7UCMpsDYDkhkqp2BzVt1SZJQxmevIDxKI4x7kxE8zPMn3GnDR+adoPsB\\\\naCTu525P5JE9QYgABqe7ZrfNG6QMaXHYDfKZhHeOMzgQ4\\\\\\/ZB6BT\\\\\\/AGB6GJsbMY1E7knmSghnxKCf\\\\nY9vXTKnYbp9s4I5rONuUgx\\\\\\/s8h+IUllykyM07x8QsBpfsmHinBPtzVIy5gD2mPb7wn4q9kmweCfD\\\\nKCXAmCMSjxVc2oB64S2zphOc4StLXbg9CqyrpHQu1M9pg3x4KQJt0DPlIKvvvNG2XzSq6m7tpkj+\\\\nyNy3wUESgjOVIaC3V4cO7cfa6FTzJ8lkmzYOxwVeUVeKiMNJw4efNAWJeEnUmhJklFrwnuAtzufi\\\\nmJRraR4pTpNSac5PyZnXqB8RsVXXVjnxNkZ9uM6gpcrgyYHkH7fFNyjWD4Ja7aCDITU04I5kZHkV\\\\nFL+8iDh1HJOxO0SuYTtk4\\\\\\/NRXnupnM5h3tN\\\\\\/MIJDrpMaWA4c84H5qnrIPW2SYGWhulhz1VrX0rKj\\\\nTrzlvLB8VDlwxmluwHJVuhhqrLXOHUFV0m8mPBXN\\\\\\/iMNS8j7+4VHMdAJwdRRTdd7JaQQcPT1Bbh0\\\\n85w7xaAAPrlbCQ5cVW8JUIt\\\\\\/DdvhHMRBx6bnf81PlduVhvbfE28qmuDh\\\\\\/TttOPa7uYZ\\\\\\/gVo55VRc\\\\nf\\\\\\/3m2O\\\\\\/ZlH0b\\\\\\/JKmnyPBCYc5G52U04hABzkguQcUnmmQE5SS4BB42zlNoOQsv8EnUUknCGUGWH4S\\\\nS4JtzsFHqQOxROyAOEjUhlAGTuhqSSUWUFssu35ItSTqRavcgbKzsiyEknA5os7IBT5Gsa5zjgDc\\\\nkqqtma6qlrXfZyWxe7xTF\\\\\\/rnkMoof6yXGT4BWdLA2lpoom8mNAQaQXBESkkoZ8EEBKzHaLIGcF3Y\\\\nk84gPm4LSl2yx\\\\\\/arLo4JuA6uMbf8bUJy8PPcrsJgbjzKXO7c+9IyMLpjAa0dnpvV6fLh7T91RUMH\\\\nrNVGzHsk5J8lrAAAAOiuAbShkIAgc0mR7YmF7uQ32TBMztbxGN8\\\\\\/a8gnOmE1C3A1kYc\\\\\\/2iPDySZ5\\\\nnDSxn9Y7YeQ8VIG4GaUDYxsOT5nwTqbjaImBo3A8UouICcBRcPFBJL0EyemGzjKfbVAHd2FWMmtc\\\\nLT3tbJO\\\\\\/wjeAPojHEdFCzTBQsef15G6j9VzXk+Ia2jrA84aS89dIz+ClRUU9VypJHeZbj8VnjxbW\\\\nHLYtFO3wZt+CQL\\\\\\/WSbvqpPcDhR12hro7XVxgYfGwZ3Er84HwS5G90DqrKdp95KxpuD5R7Ur3D9px\\\\nKV34znAKjqyDXMrqNrcTVmHdRGzP5oG428YIlqnnyAA\\\\\\/BZVtSAE6KjON0rlTaCS4Ubm4ArN+usD8\\\\nlFYaMcoJnfvSKsbP5pxk\\\\\\/mp3SWeumI\\\\\\/8K7\\\\\\/+VOQzQROy2CQHx71VrJ\\\\\\/NOifZGzWnrsZOdE490v8A\\\\nkjbVxnmaof3gfyVY2YnkU41z87BzvcEBYishz\\\\\\/WVI94B\\\\\\/JB9VF0qHD9+P+Shshmf\\\\\\/ZnfxTooJiNy\\\\nG+9PdGwll79oDaiAnoSC3BQe6bA9mN\\\\\\/mx6H9G5+08H4JDrSw\\\\\\/af9EdVCDU96HPd6vIOoI33VdXVT\\\\nQGuw4PYc4LSPers2+NvJ7x7jhRJaQgnTPLnwJyPqq66FQ+qZKAWvBz4lQKqTAJOw8VZ1VLL+pDN5\\\\nPaAVTV1NE5pbLRyMB\\\\\\/3RyPor\\\\\\/U7BSXSmNbC+Y5GkEsb4jxWbpojV1sMY3D5GtHzWuq6SOSIxR1hi\\\\nyNIbIACB8d1V2+1SWq60dS8Coghka5zWcyB4ZT64I7bE0QQRxjk1oakSu5qop+LrZWsB9abTO\\\\\\/Uq\\\\nP0Z+qnmdszA5jw9p5FpyCs42lJL+eVVXB3\\\\\\/edvP\\\\\\/ANQf4VPc7zVfW\\\\\\/8AjaJ3PDnD\\\\\\/CU6pJeTlNOy\\\\njc9Nlw8UAHOwmy8nyROdukFyZlFySXJJKLUgbK1HKGUjO6GrdBbKOEnUURKLO\\\\\\/NBFZRJJKIuQC9W\\\\nEWUnPmgSgytSLKRqQygDLkzWVjKOmfK84DR8ynCVkuJ7l6xUNp43Zaw+1g8ygJdhDrnXS1spyWnD\\\\nR4FaMHG6rrJSepUEbSMOd7Ts+KsNSezo85QSHO22KIEnqjRFPOywfbDNo4Pkbn7czB9c\\\\\\/ktu4+a5\\\\nt21VAbYKSLVgvmzjPgP8055Tk4pK7U\\\\\\/6pBQJ3KU0anAeOy6IxXtgptELpT9p2w9ytc4CZp2CGnjY\\\\nNsAJeSqhF5TEv6aUR59lvtO8z0CXI8MYXOOwSKfV3Qc4Ye72jlFoOSSCNjnHYN3KagY7Jlf9t22P\\\\nAeCS93fSaAfYYfa8z4J0nZLyCs4RavNJ1IiVQL1IKPJVxRO0ueAfDKCWw7DHVFo2T7ao+Kp2z7c0\\\\n8yUrhC3bUkdU8yq89lURynmno5SlsLZtV5p9tSehVS1\\\\\\/LdPseehCNhatqfNOtqD4qsYXEqfTUE8w\\\\nBI0N6EqNmlMqNxupUHeSnDWk+aVTW1kTgXZeVZM9gDGw8AjYNQ2+Q41uDR81NhoYm7ucXFNiX5+9\\\\nOCXCNwJjBGzGGNCeEwHgFXiUlL73KNknifffdLMoPiq\\\\\\/vUffbcsI2Ex0oTb5AVGMuyS6RGwXI\\\\\\/mo\\\\ncsmxS5JMDKiSP5oBqd+yr53jdSJ5FXzv3KAiVbWytIc0OHmFTz0bWnMT3wn9k7fJWk7ydlWzyHdA\\\\nQppKiPOsNmb+zsfkdvqo0N\\\\\\/dRTYgqZaGQfcJwPkdlIqHZCqLrSsq4jnGsDY4VHts6DtAlhGLhEJW\\\\nf72Ae18W\\\\\\/wAlcyXmmr5LfLSzNmY6bT7J3GWnmFwuOsmov6uR2n9R24VlbLt3ldDURn1eshdrYebX\\\\nY8fFPa5l8u6vfsmy5UNg4tivgMMjWwVjf7PVkPHi0\\\\\\/kroHITjWUondILkRekkqgMnxRakknmi1HC\\\\nCLJ3SUWpEUAonCLUkk4RakGVnJ5oJGrO+EZdtgIBSIkBIJPiUWrdPQ0Xq8ERdtzTepJkkDGlxOGj\\\\ncklHY9It6uIt9E9wd+kd7LQstZKQ19xbqPstOpxKZu1zdcarXuGDZrVoOGKM01IZXD2pfwTL+68z\\\\ngY\\\\\\/BGHDzTerbkhqUmUST1RF2EWpILkAbnbFcj7cKsF1ugGcgOefoF1hxXEu2mp7y\\\\\\/wBLCPuwZ+ZV\\\\nY+UVz0DA81IoIu+q429M5Uc8lYWNualx6gLeMWgyEMpGUl8gjYXEclZCkIlmawHLW+078kc0vdt5\\\\n5cdgPNN07DGwl+8jzlyDR3kpk20jZv5pEdjaI2AZyevmUerdJJwMpBcdSc7GWTnoodfcW0bdI9qQ\\\\njYeCFdWto48nd55NWcmndO8vccuKVoCSV0ry5xyT1QTW\\\\\\/igp2HdI36lKiBOOq9E9kPo7cA8WCN1T\\\\nx3T3eowC+ioj3LgeoxIA4\\\\\\/JeoOGOxLgjhTSbfw7SNkbymmb3jvm7K+Z5\\\\\\/unFxXUlt\\\\\\/wenzstlluF\\\\n0kEdJR1FS89IYi78FuLH2F8cXws9V4brQ1\\\\\\/J80fdt+ZX0UgiZAwMjY2Ng5NYNI+QTuQvNz+8Z\\\\\\/04\\\\nHp4jtPog8bVjY3VJoqEH7Qkm1EfALZWj0KqvWDcOIYY2+EEJcfqvVpcGtJJAA3JPRQKniKipsgSd\\\\n48fdYPzXByfd+aecpA4nbPQ84cowDNeK2dw+8I2tWjpvRd4NiYBIa2YjmTPp\\\\\\/ALY1fFsz9oWiMfM\\\\nqonuU9S\\\\\\/MkrnZ8SvL5PvfL6ytCtb2A9nlL\\\\\\/WUkjyP1qt\\\\\\/wCRSXdkPZzDkCzvl\\\\\\/8AyJf\\\\\\/APSn94MY\\\\n2Sg4YXHl94+py8ZX\\\\\\/JKCr7MeCmf+F4WgePGWrmH\\\\\\/ADKiuHZnZfaMHCtKB4sqpT+LlvO922KAecFZ\\\\nf7r9Vf8AuX\\\\\\/JOK3Xs8oIZMvtclI3oGvdj6kqtPAlseDp75n9\\\\\\/K77ryDnChVNooa7+up2Od+sBgrX\\\\nH7t9TPOd\\\\\\/wAhwaTs9pXH2KmVh8wCos3Z7IB+irGn95q7NW8EQSuJpZjH4NeMhUVXwlc6cEiETNH+\\\\n7cM\\\\\\/JduH3n6if1m5RNwLcWfYdFJ7jhQZ+ErtEP8AwznfunK6ZMH07i2Vjo3Do4YTPfjxXdh98+on\\\\nmShyWqtVfTg66WUY\\\\\\/YKq5i5mQQQfcu2GYZ5qLURw1LS2SNjx+00FdmH3+\\\\\\/1Yf8k4hK\\\\\\/PmoMxIyuy\\\\nVfC1pqs66RjT+tH7KzVz7OKWXUaeqfEejX7hehx\\\\\\/e\\\\\\/ps+2W4HMJ37qtnk3K2F24ButIHGKIVTPGN\\\\nwz8isVXRSUsjmTRuicDyeML2eL6jh55vjylCNO\\\\\\/JUKofhpOemUqeoYxuXODR4k4VBcrt3xdBStdK\\\\n7k4sG3z5LoClnd7Xkm6dz3VcDWH2i4HboBzT8doqJT+lc2BnXfLk6K2jtUbmQfpZTzLeZPmU9hbV\\\\nNa6hidUMf3UkXtseObSuu22sdV26lnccukiY8+8tBXneorZKyTVK4EAeywcgvQNqxFa6Nh+7Cxvy\\\\naFUa4+E\\\\\\/UiymTJ5ojIraHi7BRF+CmO8QMnRA7Hu88kRdlMa9uaLvEA9qwhqTPeIjJ4o2ez2rZFr2\\\\nTPeZ5Iu8wgbPakNXkme8HiidIPFBHi7ZZvie7d2DSRnBP2z+Sn3a7Mt1M5xP6R2zB4rDTTvnkdI4\\\\n6nOOSVUgTbdTGurYoh1OT7uq3rB3bGtGwAwAs1wvRmOJ1S4Yc\\\\\\/ZufBaDWPEJUz2pAuTPefFDvAkR\\\\n0uScpvvAk95ucoBbzsuB9qlQ6bjGpBORG1rAPAYXdXyLznxtWtrOJ7lK05aZSAfdsrxZ5KTKtrCN\\\\n5Dz5BUpk2V1YT+hkOPvYW0rJcat0zrM0xA\\\\\\/q2c\\\\\\/N3+SRPPpAY37bth\\\\\\/NKZiGMNGMDxVbBU7idLG8\\\\n3Hn4DqlABjQ0DACjwOMj3y52ds3PgnSdjulKCiE3NM2CNz3EADqg6QNBJOyobnXmrlEbMljTsPEp\\\\n2gzV1LqmYvcfcE3BBJUP0RtLj49AplFaXPOuf2W89PUq4iYyFga1oa0eCjvTQIrEwsHePJd1xyQV\\\\nj3g8EEaJ2SHInBp6hkjgctLHaXj81v8AhTt6424IlY2kvdRNC3\\\\\\/1auPfMP8AFv8AVcmdnIyAfNIe\\\\n1znahLK0+T8\\\\\\/ivM5OLDl7ZzZvY\\\\\\/C3ptt7sN4hsBccY723SDJ\\\\\\/uvI\\\\\\/Fb+zel52f3RwZPPX2lx61tL\\\\n7Ofexzgvn0KmojwBIyQftDBU2ndVTsBY1rs9GyDP1Xl8n2r6fLxLBt9Davto4a4lm7q2cQUUsQ9n\\\\nHfBpcfccFPQXGOoaHRyMlad9TCCvnbP38R\\\\\\/S08gHjpz9Qplq4guNAe9t9dWUxaecEr24+RXzv1H+\\\\nm8s8rlhy\\\\\\/wCYH0K9Z94HmgJ8kb7Lw7b+2vjO2ANj4hq3tBzifEn4haOj9J3jCnAEslvqvOWlIP8A\\\\nhcF5Of8Apz6zH9tlD2F3wyh6wF5Tg9K6\\\\\\/tb+ltdskPiwSN\\\\\\/5ipMfpYXMEd5YaNw\\\\\\/YnePxC5Mvsf3\\\\nCf0b\\\\\\/wDMJ6kE46IzUaQXFwAHUleZovSzm+\\\\\\/w6z+7Vf5J7\\\\\\/0sI5WlknDjntcMEesjB+ij\\\\\\/Zvr\\\\\\/wD6\\\\n\\\\\\/wDmf+zejv6Tp\\\\\\/8A3iP+MJyK4QvOlkzHk9A4ErzE\\\\\\/wBIqx1GO84Tlb4mOqaPyU2h9JSyW92uDhao\\\\nY7lq9YaSnftH1s\\\\\\/7d\\\\\\/4\\\\\\/9jT0qZR47pLqhrR7RwPNeba\\\\\\/0rZ3E+pWEMb0M84J+iyt29Im\\\\\\/XXIfTQs\\\\nYfud67H0C0w+y\\\\\\/XZecNf+YNPQfHt9opoo6WItkqGv1Oc3GAMHbPxWJ9ZHxXGJO1q8vdlsFDH5lr3\\\\nf8yju7VL485ElLH+5AT+JXp8f2T6uTWp\\\\\\/kO2mqSJasRty44HiVwSt48vlYCHXSWMHmIWtZ+AVHU3\\\\nSWck1NdPNnn3s5P5rsw+w81\\\\\\/fnIHoap4hoKduqWtgiH7cjR+az1w7TrBR5aK01Lx92Bhd9eS4W+6\\\\nUUefajJ8tyoNVxHG0ERxvf8ADAXoYfYOOfvztDsVf2w0jWO9UoKiR\\\\\\/QzlrB9CSsDxXxvcOKIO4mj\\\\npqWLVkOiaXP\\\\\\/AIisRUX+oeCGxsZ5ncquq7jPO3D5XEeA2Xq\\\\\\/T\\\\\\/a\\\\\\/pvp8pnhj3\\\\\\/uSwqfUqZ2qol75\\\\n\\\\\\/g9xcfkoM99iY0tgiOByLvZCrHuyeW\\\\\\/ikGMyOwAXOPIAZJXsyHoiqrZ6skSSEt\\\\\\/VbsFFI2OBgLoP\\\\nC3YtxJxPI1xpf6OpjuZ6sFox5N5ldX4f9HOw21zJLlUVF0mGCY8hkRPuG5+a0x4sr6V015yoLbUV\\\\nssJZDI6F0rYzMGksaSepXfI3aI2Nz9kALVdplFSWjhKjoqKmipKd9bC0MhYGgYOenuWOMvgE8sOi\\\\n6aYzSRr80WseSjmXISe8UqSDKAh3yil+URfjdASu9HvRd7uoxkCIyICSZcIu8GVH7xFr3ygJPebo\\\\njJvsoxkRd5lASTJlMVddHR07pZHYa36pt8oY0knAAyVjb9ezXymNhxC07Y6lOdxsV1urrjVGV2w5\\\\nNHgFGp2uqZWRs3c44ChGTKv+FqbvJnVDuTBhvvRewa2nY2nhZG07NGE4X4UbXjrkId4Ugk95hEZF\\\\nH7zdF3mUBJ71JMuyY7wJJkHwQDrpF5luk\\\\\\/e1tS\\\\\\/q6RxPzK9HSz6I3O8ASvMk8up7j4klXizzJLle\\\\nWWQMo3uJwA47\\\\\\/BZ8ux1Vvaz31OGHaNrsu8z4LRktoTk96ebuXkEqZ+tujP2vwTevySIpA8l\\\\\\/Q7D3\\\\nKgkh+MAcvBAyJh8zY26i7ATOt82ecbP8R\\\\\\/kjwArpZJcwRbuPN3QBFR29lKNR9uT9Yp1gEYAaNkZd\\\\nlHkHdaIvymS5Hq80wdyEE1qQQe3v7jr0Kaljpp+GLo17cktpqwdPAOC4TxH2McacL6zX2CqaxvOS\\\\nFveN+i+mGPJIewPBDhqB6Fcmtte1fJqeJ8EhjkY6J42LJAWn5FM6nA7fML6nXjgaw36NzK61U1QH\\\\nbHUwLnd89FXs+vD3uFrNG53Wndox8kdBdMfPttbUM2E0jR4BxTsV8rYCdM78eBOV7AvPoQWWZ73W\\\\n+81VOPutkw4D6LFXr0Ir\\\\\\/A0m23enqj+rK3T9chT0Uuj+XmytuL694dMGucBgENwVG0Ndvrlb5CQr\\\\ntdf6I\\\\\\/aLR\\\\\\/YttPV\\\\\\/\\\\\\/TmAz81Q1Po4do9HIQ7hSeUDrHPGR\\\\\\/xJdFLprmg1N+zUSD34KAlnB2nz72rb\\\\nVPYzxvSEiXhiuaR4aHfg5Vs\\\\\\/ZzxPTajLYK5uPCEn8EumjprOGaq6TM+LUYnrB\\\\\\/axfwlWMvDN2gdi\\\\nS1VrD50z\\\\\\/wCSZNmr2g6qCrGP\\\\\\/l3\\\\\\/AMkum\\\\\\/BavwYZU1P3pGfAH+acFXVbe3GR7ilf0fVEEGiqvjTv\\\\n\\\\\\/knYrVWEezQ1Z91O\\\\\\/wDkn034LV+CX1MrsaZNPwykiSY86k464aFJZYrk93sW2sd7qd\\\\\\/8lYQcGX6p\\\\nH6OzVrv\\\\\\/ALDh+KOi30fTVS1zyQDPIR78J3I5l8h98h\\\\\\/mrpnZ1xNJ9ix1R9+lv4uT7ey\\\\\\/iz\\\\\\/4JJk\\\\\\/\\\\nrTxD\\\\\\/nVTC\\\\\\/A6a59PVyGR47x+MkbuKbMocNzldAp+wbimqlJmipqUOOf0k4OP4cq3pvRwuL2g1F3p\\\\novEMY52PwVzizvo+muTGTGw+iS+Qld4ofRytkRzV3WqqPKNoYPzWmtvY1wnbQ0G3GqePvVMhdn4c\\\\nlc4Mj6Xl3S+WQMa0ve47NaMk+4LWWLsg4o4ha17LeaOF24kqzo293Neo7fbaK0Q91RUsNIwchDGG\\\\n\\\\\\/gpJeMLacEl7q6dOMcNejnSUwbLe611U\\\\\\/wD3NN7LfnzXS7LwZYeHomtoLVSwPYMd73QMh97juVc6\\\\nx1SXSALfHCY+IqQNY8UkvASXPB5c0092600bAdsdURRWWIHAfXNz8GuKxpetH2zSEy8NtHI1jifg\\\\nwrJukByFw8v7gfLzjcotZUYvSTIsQkmTfmhrUbX5otfmgJJk35ou8yOajd4PHdGXoCR3qIygKMZE\\\\nRegJJkPikukIUcyYCqb3eW0MJjY79M7kPAeKBs1xFezg0sLsdHkfgszryfFMyTa3FxOSfFJ1ZCoJ\\\\nLXZd5rc2mL1S3xR4w7GXe8rJcP0nrdWHuH6OPcrYasKQkh6MP81G148UNeUBIMnmkmRMF+3gEXeD\\\\nxyg0gyZ3SXSKP3n\\\\\\/AEURk80Ej3up9XtNZIDgticc\\\\\\/BecJHb+a7vxxVCHha4OJ5x6fmcLgjzuVeLL\\\\nIC7wWloWNjpIw0YBGT5lZYODjlaWnkDKaMk4AaOquMz80ulobyLjpCL1jPsxgOI2z0CjOiNRMJJC\\\\nQ0cmg\\\\\\/ipBcABgbJ72Btj3Bf7bvE9PcnNXmmdeEesYRAd1Ii5M690TpQ3fP1VA+XJJfgc1EkrYWfa\\\\nkAKYddYByeXe4ICeXgdUFWm8Q55PQRsn3TKB5IILJuQidyQQTIkjZADIygggwGyCCCDht8bHbloJ\\\\n8wmX0FNIMup4ne9gQQUioknDlrqTmS307j5sCiT8D2KVx1WyH4AhBBEtHpH\\\\\\/AOz3h9z8f0bH8z\\\\\\/N\\\\nG\\\\\\/s24e\\\\\\/+HtH94\\\\\\/zQQT3SEezTh0sz6gP43fzQb2Y8O\\\\\\/8AuP8A\\\\\\/Y7+aCCvG3R+hns14db\\\\\\/AOzwfe93\\\\n8043s54eH\\\\\\/s5nzP80EE93RFDs\\\\\\/4fjBxbITv97JXE+NbdBaeJK2mpmlkLH+y3OcIIK+O23ucUD3EJ\\\\nGs6kEF0+jFncos7EIIJmaccBJPJBBAogUw8nWUEE4uuedrjA6nszz9ptWcfwFYYk5QQXBzfuQSXH\\\\nGUAc4QQWAESk6igggQCTjPmiJ3QQQA1HdDOUEE4CJHFrCfAFYKvqX1FVI95y4lBBE8ki5ygDuggj\\\\n2bWcLNAoXuA3Lzk\\\\\\/AK61FBBR7HsWcItRyggrMCSWpOUEEgBJzhIc4hBBAZXtIeW8Lz46uaD81xGQ\\\\nnKCCqMMvIhthXlGTKIg45DWaseaCCqJTDsiJOEEFXsCLjsi1FBBUSBWVkkWQ3A88KskqpZR7TyUE\\\\nEUGdRyeqLOThBBIAeaCCCQf\\\\\\/2Q==\\\\n\\";
		try {
			String Request = request.getParameter("Request"); // 获取请求信息
			JSONObject jsonRequest = JSONObject.fromObject(Request);
			String ModifyQuery = jsonRequest.getString("ModifyQuery"); // 获取请求修改的信息（jsonarray数据格式）
			JSONArray ModifyArray = JSONArray.fromObject(ModifyQuery);
			response.getWriter().println(UCDI.Modify(4, ModifyArray));
			logger.info("请求通过用户id修改相关信息--Success");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				response.getWriter().println("{'statu':'RequestData Exception'}");
				logger.info("请求通过用户id修改相关信息--Fail");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@RequestMapping("/OnPassage")
	public void getinforms(HttpServletRequest request, HttpServletResponse response) {
		ShareJL sharejl=sharedao.findShareJLByshareid(1);
		System.out.println(sharejl.toString());
	}

}
