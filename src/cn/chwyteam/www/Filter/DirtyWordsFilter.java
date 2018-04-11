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
* @Description: ���дʹ�����
* @author: �°�����
* @date: 2014-9-6 ����10:43:11
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
    * @Description: ʹ��Decoratorģʽ��װrequest����ʵ�������ַ����˹���
    * @author: �°�����
    * @date: 2014-9-6 ����11:56:35
    *
    */ 
    class DirtyRequest extends HttpServletRequestWrapper{

       
        private HttpServletRequest request;
        public DirtyRequest(HttpServletRequest request) {
            super(request);
            this.request = request;
        }
        /* ��дgetParameter������ʵ�ֶ������ַ��Ĺ���
         * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
         */
        @Override
        public String getParameter(String name) {
            
            String value = this.request.getParameter(name);
            if(value==null){
                return null;
            }
           
                if(value.contains("��")){
                    System.out.println("�����а������дʣ�"+"��"+"�����ᱻ�滻��****");
                    //�滻�����ַ�
                    value = value.replace("��", "****");
                }
            
            return value;
        }
    }
}
