package wq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import com.google.gson.Gson;


public class HttpGET {

	/**
	 * 
	 * @param lista: dizionario di parole
	 * @param k:	numero di parole da scegliere
	 * @param inde:	insieme di "k" numeri scelti a caso
	 * @param HashMatches: struttura dati su cui riversare le traduzioni delle parole
	 * @return "ok" se va a buon fine, messaggio di errore altrimenti
	 * @throws IOException
	 */
	public String sendGET(List<String> lista,int k,List<Integer> index,HashMap<String,List<String>> HashMatches) throws IOException 
	{
				
		String parola=null;
		int i=0;
	
		while(i<k)
		{
			parola=lista.get(index.get(i));
			String GET_URL = "https://api.mymemory.translated.net/get?langpair=it|en&q="+URLEncoder.encode(parola, StandardCharsets.UTF_8.toString());	
			URL obj = new URL(GET_URL);
			HttpURLConnection con = null;
			int responseCode=0;
			try{
				con =(HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
			}

			catch(UnknownHostException e)
			{
				System.out.println("Connessione assente");
				return "Connessione_assente";
			}

		
												// successo
			if (responseCode == HttpURLConnection.HTTP_OK) 
			{
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

			Traduzioni jsonObject = new Gson().fromJson( response.toString(), Traduzioni.class);
		
		
											
			List<String> list=null;
			for(int j=0;j<jsonObject.matches.length;j++)
				{
				if(!HashMatches.containsKey(parola))
					{
						
						list = new LinkedList<String>();
						list.add((String)jsonObject.matches[j].translation);
						HashMatches.put(parola,list);
					}
				else 
					{

					list= (List<String>) HashMatches.get(parola);	
					list.add((String)jsonObject.matches[j].translation);
					}
				}
		
			
			} 
			else 
				return("GET_fail");
			
			i++;	
			}
			
		return "ok";
		
		
		}//fine sendGET

	
	
	}//fine classe

	
	

