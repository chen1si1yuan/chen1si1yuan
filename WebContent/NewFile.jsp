<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath }/css/bootstrap.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath }/css/home.css">
</head>
<script>
	function login() {

		var s = {
			"UserName" : $("#UserName").val(),
			"UserPwd" : $("#UserPwd").val()
		};
		var st = JSON.stringify(s);
		var a = {
			"RequestTime" : "",
			"LoginType" : "",
			"RequestType" : "Login",
			"User" : st
		};
		$.post("web/UserRequest.do", {
			"Request" : JSON.stringify(a)
		}, function(data) {

			var json = JSON.parse(data);
			if (json.statu == "true") {
				window.location.href = './UserMain.jsp';
			} else {
				alert(json.statu);
			}
		});
		return false;
	}
</script>

<body>

	<div class="video_class">
		<div class="videos_res">
			<video preload="metadata" autoplay loop> <source
				src="./video/ygsq.mp4" type="video/mp4"></video>
		</div>
		<div class="bg_class"></div>
		<div class="text_class">
			<section id="welcome">
			<div class="welcome-message"
				style="position: absolute; top: 40%; left: 20%;">
				<center>
					<h1>在途登录</h1>
				</center>
				<form style="position: absolute; top: 50%; left: 30%;" method="post"
					action="${pageContext.request.contextPath }/web/select.do"
					ction="csy/login.do" onsubmit="return login();">
					Username:<input style="color: blue;" id="UserName" name="UserName"
						type="text"><br> Password:<input style="color: blue;"
						id="UserPwd" name="UserPwd" type="password"><br>
					<center>
						<input class="btn" style="color: blue;" value="submit"
							type="submit"> <a class="btn"
							style="background-color: white; color: blue;"
							href="${pageContext.request.contextPath }/Register.jsp">Register</a>
					</center>
				</form>
			</div>
			</section>
		</div>
	</div>

	<script type="text/javascript"
		src="${pageContext.request.contextPath }/JS/jquery-3.2.1.min.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath }/JS/json.js"></script>
	<!-- Main Style Sheet -->
	<!-- Modernizr -->
	<script
		src="${pageContext.request.contextPath }/JS/vendor/modernizr-2.6.2.min.js"></script>
	<!-- Respond.js for IE 8 or less only -->
	<!--[if lt IE 9]>  
  <script src="JS/vendor/respond.min.js"></script>  
<![endif]-->
	<!-- Google Analytics: change UA-XXXXX-X to be your site's ID. -->
</html>
</body>



