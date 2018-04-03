<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<%if(request.getSession().getAttribute("username")==null)
	response.sendRedirect("NewFile.jsp");
	%>
	尊敬的<%=request.getSession().getAttribute("username") %>,用户,欢迎您登录！！
</body>
</html>