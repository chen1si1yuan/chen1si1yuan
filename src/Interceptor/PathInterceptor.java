package Interceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.chwyteam.www.DES.Base64;
import cn.chwyteam.www.DES.Cryption;
import net.sf.json.JSONObject;



public class PathInterceptor implements HandlerInterceptor{

	private static final Log logger =LogFactory.getLog(PathInterceptor.class);
	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object arg2) throws Exception {
		// TODO Auto-generated method stub
		response.setContentType("text/html;charset=GBK");   //设置回传信息格式，避免参数乱码
		boolean flag=false;
		String path=request.getServletPath();
		if(path.contains("UserRequest")||path.contains("test"))
			{
			flag=true;
			}
		else
			flag=true;
		if(!flag)
		    {
			Cookie[] cookies=request.getCookies();
			for (Cookie cookie : cookies) {
				if((new Cryption().decryption(cookie.getName().trim()).trim()).equals("UserName"))
				{
					logger.info(new Cryption().decryption(cookie.getValue().trim()).trim()+" 正在登陆");
					return true;
				}
				else
				{
					logger.info("已拦截并跳转至登录界面");
					//request.getRequestDispatcher("/NewFile.jsp").forward(request, response); 
					JSONObject jsonerror=new JSONObject();
					jsonerror.put("statu", "请先完成登录");
					response.getWriter().println(jsonerror.toString());
					return false;
				}
			}
		    }
		
		return flag;
	}

}
