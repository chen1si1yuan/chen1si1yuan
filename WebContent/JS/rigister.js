 $(document).ready(function()
		 {
	 $("#register").click(function()
			 {
			$.get("csy/register.do");
			
			 });
		 });
 
 function VerifyUsername()
 {
 	$.get("csy/VerifyUsername.do",{username:$("#username").val()},function(data)
 			{
 		if(data=="true")
 			{
 			$('#adderrmsg').attr("class","alert-danger hide");
 			
 			}
 		else 
 			{
 			$('#adderrmsg').attr("class","alert-danger");
 			}
 			});
 }
 
function detection()
{
 if($("#realname").val()=="")
 	{
 	alert("真实姓名为空");
 	$("#realname").focus();
 	return false;
 	}
 else if($("#cardid").val()=="")
 	{
 	alert("身份证ID为空");
 	$("#carid").focus();
 	return false;
 	}
 else if($("#username").val()=="")
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
 else
 	{
 	return true;
 	}	
}
  