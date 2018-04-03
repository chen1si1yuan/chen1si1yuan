function verification()
{
	 document.getElementById("VeriCode").src="${pageContext.request.contextPath}/csy/VerificationCode.do?"+Math.random();  
}


 
 function checkcode()
 {
if($("#username").val()=="")
	{
	alert("用户名为空");
	$("#username").focus();
	return false;
	}
else if($("#pwd").val()=="")
	{
	alert("密码为空");
	$("#pwd").focus();
	return false;
	}
else if($("#verificationcode").val()=="")
{
alert("验证码为空");
$("#verificationcode").focus();
return false;
}
else
	{
	return true;
	}	
 } 
 
 $("#form1").ajaxForm(function(data)
		 {
	 if(data=="true")
		 window.location.href="main.jsp"; 
		 else
	 alert(data);
		 });
 
 $(document).ready(function()
		 {
	 $("#register").click(function()
			 {
			$.get("csy/register.do",{username:$("#username").val(),pwd:$("#password").val()},function(data)
					{
				alert(data.key)
					});
			
			 });
		 });