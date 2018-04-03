
function deletefestive(id)
{

	 if(confirm("确定要删除吗")){
		 $.get("festive/delete.do",{id:id},function(data)
				  {
			  
				  });
		  alert("删除成功！");
		  window.location.reload(true);
		  return false;
	    }
	  

}

function update(id)
{
	  var name=document.getElementById("name"+id).value;
	  var date=document.getElementById("date"+id).value;
	  var intruduce=document.getElementById("intruduce"+id).value;
	$.get("festive/update.do",{id:id,FestiveName:name,Date:date,intruduce:intruduce},function(data)
			{
		alert(data);
		window.location.reload(true);
		
			});
	   
}
