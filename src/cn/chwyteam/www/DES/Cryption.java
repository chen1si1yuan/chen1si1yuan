package cn.chwyteam.www.DES;

import java.util.Arrays;
import java.util.Random;



public class Cryption {

	static String key="L5uzeRXSIY9VlnPHw4fcEbi6A3dWKhN0xmqJBgFZtkoCM82aODTs1U7rvjyQpG";
	static String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	
	public  String encryption(String string)
	{
		String str="";
		for(int i=0;i<string.length();i++)
		{
			int j=key.indexOf(string.charAt(i));
			str=str+alphabet.charAt(j);
		}
		if(str.length()!=string.length())
			return "";
		else
		return str;
	}
	
	public  String decryption(String string)
	{
		String str="";
		for(int i=0;i<string.length();i++)
		{
			int j=alphabet.indexOf(string.charAt(i));
			str=str+key.charAt(j);
		}
		if(str.length()!=string.length())
			return "";
		else
		return str;
	}
	
	
		
}
