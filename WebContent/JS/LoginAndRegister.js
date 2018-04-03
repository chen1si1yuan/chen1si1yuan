/**
 * 
 */
function exit()
{
	
	$.get("csy/exit.do",function(data)
			{
	window.location.reload(true);
			});
 
}
