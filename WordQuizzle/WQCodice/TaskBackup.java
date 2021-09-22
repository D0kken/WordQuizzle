package wq;

import java.io.FileNotFoundException;
import java.io.PrintWriter;


import com.google.gson.Gson;

public class TaskBackup extends Thread{

								/////////////////////////////////////
											//VARIABILI
private GrafoAmici map;	//grafo amici
private int backup ;	//#backup

								/////////////////////////////////////
											//COSTRUTTORE
	public TaskBackup(GrafoAmici grafoAmici,int i) {
		this.map=grafoAmici;
		this.backup=i;
	}
	
								/////////////////////////////////////
											//RUN
	@Override
	public void run() 
	{
	
		
		String json=new Gson().toJson(this.map);
			
		try (PrintWriter out = new PrintWriter("backup"+this.backup+".json")) 
		{
			out.println(json);
		}
		
		catch (FileNotFoundException e) {e.printStackTrace();}
		
	}
	
	
}
