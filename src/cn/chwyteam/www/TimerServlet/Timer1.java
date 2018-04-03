package cn.chwyteam.www.TimerServlet;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServlet;

public class Timer1 extends HttpServlet{
	public static HashMap<String, String> map=new HashMap<>();
	public static HashMap<String, String> cachemap=new HashMap<>();
	
	public void init()
	{
		Timer timer=new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				cachemap.clear();
				cachemap=(HashMap<String, String>) map.clone();
				map.clear();
			}
		}, new Date(), 1000*60*5);
	}
	
		
	

}
