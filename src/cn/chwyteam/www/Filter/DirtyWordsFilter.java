package cn.chwyteam.www.Filter;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
* @ClassName: DirtyFilter
* @Description: 敏感词过滤器
* @author: 孤傲苍狼
* @date: 2014-9-6 上午10:43:11
*
*/ 
@WebFilter(filterName="dirtywords",urlPatterns="/*")
public class DirtyWordsFilter implements Filter {

    private FilterConfig config = null;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        DirtyRequest dirtyrequest = new DirtyRequest(request);
        
        chain.doFilter(dirtyrequest, response);
    }

    @Override
    public void destroy() {

    }
    

    
    /**
    * @ClassName: DirtyRequest
    * @Description: 使用Decorator模式包装request对象，实现敏感字符过滤功能
    * @author: 孤傲苍狼
    * @date: 2014-9-6 上午11:56:35
    *
    */ 
    class DirtyRequest extends HttpServletRequestWrapper{

       
        private HttpServletRequest request;
        public DirtyRequest(HttpServletRequest request) {
            super(request);
            this.request = request;
        }
        /* 重写getParameter方法，实现对敏感字符的过滤
         * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
         */
        @Override
        public String getParameter(String name) {
            
            String value = this.request.getParameter(name);
            if(value==null){
                return null;
            }
           
                if(value.contains("猪")){
                    System.out.println("内容中包含敏感词："+"猪"+"，将会被替换成****");
                    //替换敏感字符
                    value = value.replace("猪", "****");
                }
            
            return value;
        }
    }
}
