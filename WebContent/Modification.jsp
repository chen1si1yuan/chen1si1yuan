<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
     <script type="text/javascript" src="${pageContext.request.contextPath }/JS/jquery-3.2.1.min.js"></script>
                  <script type="text/javascript" src="${pageContext.request.contextPath }/JS/json.js"></script>
<script type="text/javascript">
 function test()
 {

	 var s={"UserName":"7448558","UserPwd":"546546","Email":"1441565921@qq.com","CAPTCHA":"94143"};
	 var st=JSON.stringify(s);
    var a={"RequestTime":"","LoginType":"","RequestType":"Register","User":st};
	 $.post("web/UserRequest.do", {"Request":JSON.stringify(a)},
	 function(data){
	 alert(data);
	 }
			 );
 }
 function test1()
 {
	 $.get("web/OnPassage");	 
 }
 function test3()
 {

	 var s={"RequestTime":"","ShareId":1};
	 $.post("web/GetComment.do", {"Request":JSON.stringify(s)},
	 function(data){
	 alert(data);
	 }
			 );
	
 }
 function test4()
 {

	 
	 var s={"RequestTime":"","ShareTime":"2018-03-25 15:45:00","ShareText":"今天天气好","ShareUserId":1};
	 $.post("web/PublicShare.do", {"Request":JSON.stringify(s)},
	 function(data){
	 alert(data);
	 }
			 );
	
 }
 function test5()
 {

	 
	 var s={"RequestTime":"","ModifyQuery":"[{\"ModifyInfo\":\"HeadImg\",\"Value\":\"\"}]"};
	 $.PUT("web//UserModification/4.do", {"Request":JSON.stringify(s)},
	 function(data){
	 alert(data);
	 }
			 );
	
 }
</script>
</head>
<body>
<form action="web/sendEmail.do">

  <input type="submit" value="Submit">
</form>
<button id="ss" name="ss" onclick="test1()">y</button>
<button id="ss1" name="ss" onclick="test()">z</button>
<button id="ss2" name="ss" onclick="test3()">t</button>
<button id="ss3" name="ss" onclick="test4()">comment</button>
<button id="ss4" name="ss" onclick="test5()">test</button>
</body>
</html>