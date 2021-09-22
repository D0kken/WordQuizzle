package wq;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.*;

public class Server{

	private static ConcurrentHashMap<SelectionKey,String> Connessi; //struttura dati che tiene conto degli utenti connessi
	private static Selector selector = null;	   			    	//creo selector
	private static Integer portSelect=Variabili.TCPort;				//porta utilizzata per la select
	private static Integer portRMI=Variabili.portRMI;				//porta utilizzata per RMI
	private static GrafoAmiciRMI grafoAmiciRMI=null;				//grafo delle amicizie
	private static GrafoAmici grafoAmici=null;						//struttura dati contenente informazioni su utenti collegati
	private static TaskBackup threadBackup0;						//thread che gestisce il backup#0 dei dati
	private static TaskBackup threadBackup1;						//thread che gestisce il backup#1 dei dati
	private static String nomeDizionario=Variabili.nomeDizionario;	//nome del file dizionario contenuto della stessa directory del server
	private static List<String> dizionario =null;					//dizionario di parole

	
	public static void main (String[] args) 
	{


		try
		{									//RMI/LEGGO BACKUP

									
			grafoAmici = new GrafoAmici();
			grafoAmiciRMI= new GrafoAmiciRMIimpl(grafoAmici.getMap());
			Connessi= new ConcurrentHashMap<SelectionKey,String>();
			LocateRegistry.createRegistry(portRMI);
			Registry r= LocateRegistry.getRegistry(portRMI);
			grafoAmici=leggiBackup(grafoAmici);
			r.rebind(GrafoAmiciRMI.SERVICE_NAME,grafoAmiciRMI);	

										//LETTURA DIZIONARIO
	
			dizionario=leggiDizionario(nomeDizionario);
		
		

			
										//START SELECT

				
			
			ServerSocketChannel	serverChannel;	
			serverChannel = ServerSocketChannel.open();
		
	
			ServerSocket ss = serverChannel.socket();	
		
			InetSocketAddress address = new InetSocketAddress(portSelect); 
			
			System.out.println("Select in ascolto alla porta "+ portSelect);
			ss.bind(address);							
		
			serverChannel.configureBlocking(false);		
			
			selector = Selector.open();   			
			
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		} 
		
		catch (BindException be){
			System.out.println("Errore di binding.");		
			return;
		}
		
		
		catch (NullPointerException npe){
			
			System.out.println("Select non esistente.");		
			return;
			} 		
		
		catch(NoSuchFileException nfe) 
		{
			System.out.println("Dizionario non esistente.");
			return;
		}
			
	
		catch(ExportException ee) 
		{
			System.out.println("Errore RMI");	
			return;			
		}
		
		catch (IOException ioe) {ioe.printStackTrace();}
									
		
		threadBackup0 = new TaskBackup(grafoAmici,0);
		threadBackup1 = new TaskBackup(grafoAmici,1);
		
		threadBackup0.start();
		threadBackup1.start();
		 
		 
		 								//LOOP SELECT
		while(true)
		{

			try{
				
				selector.select(1000);
			
			   } 
			
			catch (IOException e2){
			
				System.out.println("Errore nell'avvio della select sulla porta "+portSelect);		
				return;
			}
			

			Set<SelectionKey> selectedKeys = selector.selectedKeys(); 
			
			Iterator<SelectionKey> iterator = selectedKeys.iterator();
			
			
			while(iterator.hasNext()) 
			{  
				SelectionKey key= (SelectionKey) iterator.next();
				
				if(key.readyOps()==key.interestOps())
				{
								
					iterator.remove(); 
					
					try 
					{		
				
						if(key.isAcceptable())
							{
								accept(key);
								backup();
							}
									
						else if(key.isReadable()) 
						{	
							if(read(key))						
							backup();
						}
					
						else if(key.isWritable()) 
				
							write(key); 
					
					}	
				
					catch (IOException ex) 
					{ 
				
						if(Connessi.containsKey(key))
						{
							String nome=Connessi.get(key);
							
						
								if(Connessi.contains(nome))
									grafoAmici.logoutUtente(nome, Connessi, key);
							
							
		
						}	
					
						System.out.println("Utenti in comunicazione"+Arrays.asList(Connessi.values()));
					
						key.cancel();
					

						try
						{
							key.channel().close(); 
						}
						
						catch (IOException cex) {} 
				
					}
			
				}			
						
			}
			
		}
		
	}//run

						//////////////////////////////////////
									//METODI MAIN
	
	
	/**
	 * accetta connessione da client
	 * @param key
	 * @throws IOException
	 */
	private static void accept(SelectionKey key) throws IOException
	{	
		
		ServerSocketChannel server=(ServerSocketChannel) key.channel();
		
	
		SocketChannel client= server.accept();	
			
		client.configureBlocking(false);
			
		client.register(selector, SelectionKey.OP_READ , ByteBuffer.allocate(128));
				
		System.out.println("Accepted connection from " + key);
			
			
	}//fine accept
	
	
	/**
	 * legge e gestisce la richiesta proveniente da key
	 * @param key
	 * @throws IOException
	 */
	private static boolean read(SelectionKey key) throws IOException
	{

	boolean bool=false;
	
	SocketChannel client = (SocketChannel) key.channel();
	
	ByteBuffer buf = (ByteBuffer) key.attachment();


	int numRead=0;
	while((numRead=client.read(buf))>0) {}

	
	if(numRead == -1) 
		{
			client.close();		
			if(Connessi.containsKey(key))
			{
				String nome=Connessi.get(key);
				if(Connessi.contains(nome))
					grafoAmici.logoutUtente(nome, Connessi, key);

						
			}
			key.cancel(); 
			return false;
		}
	
		
		String input = getString(buf);
		
		String op=split(input,1);
	
		
		if ( (op.compareToIgnoreCase("login"))==0) 
		 		
			{
				login(key,input,buf);
				System.out.println("Utenti in comunicazione"+Arrays.asList(Connessi.values()));
			}
		
		
		    	
		else if ( (op.compareToIgnoreCase("logout"))==0) 
			
			{
				logout(key,input,buf);
				System.out.println("Utenti in comunicazione"+Arrays.asList(Connessi.values()));
			}
		
	
		else if ( (op.compareToIgnoreCase("aggiungi_amico"))==0) 
			
			{
				aggiungi_amico(key,input,buf);
				bool=true;
			}
			
		
		
		else if ( (op.compareToIgnoreCase("lista_amici"))==0) 

			lista_amici(key,input,buf);
		
		
		
		else if ( (op.compareToIgnoreCase("sfida"))==0) 
							
			sfida(key,input,buf);
			

		
		else if ( (op.compareToIgnoreCase("mostra_punteggio"))==0) 
			
			mostra_punteggio(key,input,buf);
		
		
		else if ( (op.compareToIgnoreCase("mostra_classifica"))==0) 
							
			mostra_classifica(key,input,buf);
		

		else 
		{
				
			buf.clear();
			buf.put(("OperazioneNonRiconosciuta"+op).getBytes());
			key.attach(buf);
		
		}	
		key.interestOps(SelectionKey.OP_WRITE);

		return bool;
	}//fine read
	
	
	/**
	 * effettua l'operazione di scrittura nel buffer di key
	 * @param key 
	 * @throws IOException
	 */
	private static void write(SelectionKey key) throws IOException 
	{
		
		
		SocketChannel client = (SocketChannel) key.channel();
		ByteBuffer buffer = (ByteBuffer) key.attachment();

	
		buffer.flip(); 
			
		client.write(buffer);

		if(buffer.hasRemaining()) {
			
			buffer.compact(); 
		}
		else
			buffer.clear();
		
		key.interestOps(SelectionKey.OP_READ); 
	
				
	}//fine write
	
	
	/**
	 * effettua il login
	 * @param key
	 * @param in messaggio letto dal client
	 * @param buf
	 * @throws IOException
	 */
	private static void login(SelectionKey key,String in,ByteBuffer buf) throws IOException{
		
		String nome=split(in,2);
		String pass= split(in,3);
		String esito=null;
		Integer port =Integer.parseInt(split(in,4).trim());
		buf.clear();
		
		if(!Connessi.contains(nome))
			esito=grafoAmici.loginUtente(nome, pass,key,port);
		
		else 
			esito="Utente gia' connesso";
		
		if(esito.compareToIgnoreCase("Connesso")==0)
			  
			Connessi.put(key, nome); 
		  	
		buf.put(esito.getBytes());
			
		
		key.attach(buf);
			
	}//fine login
	
	
	/**
	 * effettua il logout
	 * @param key
	 * @param in
	 * @param buf
	 * @throws IOException
	 */
	private static void logout(SelectionKey key,String in,ByteBuffer buf) throws IOException{
		
		String nome=split(in,2);
		buf.clear();
	
		if(Connessi.contains(nome))	
			{
			if(grafoAmici.logoutUtente(nome,Connessi,key))
				
				buf.put("Disconnesso".getBytes());
			
		  
			else
				buf.put("errore".getBytes());
			
			}
		else
			buf.put("errore".getBytes());
			
		Connessi.remove(key);
		
		key.attach(buf);
			
	}//fine logout
	
	
	/**
	 * crea un amicizia tra due utenti
	 * @param key
	 * @param input
	 * @param buf
	 * @throws IOException
	 */
	private static void aggiungi_amico(SelectionKey key,String in,ByteBuffer buf) throws IOException{
		
		String nome=split(in,2);
		String amico=split(in,3);
		buf.clear();
	
		if(!(nome.compareTo(amico)==0) && Connessi.contains(nome) && grafoAmici.getMap().containsKey(amico))	
		{
			if(grafoAmici.aggiungiAmico(nome,amico))
		
			{	
				buf.put("Aggiunto".getBytes());
			
		  }
			else
				buf.put("errore".getBytes());
			
		}
		else
			buf.put("errore:argomenti non validi".getBytes());
			
		
		key.attach(buf);
			
	}//fine aggiungi_amico
	
	
	/**
	 * invia al client un json contentente la lista amici
	 * @param key
	 * @param in
	 * @param buf
	 * @throws IOException
	 */
	private static void lista_amici(SelectionKey key,String in,ByteBuffer buf) throws IOException{
		
		String nome=split(in,2);
		buf.clear();
	
		if(grafoAmici.getMap().containsKey(nome) && Connessi.containsKey(key))	
		{
			
			Gson gson=new Gson();
			String json=null;
						
			if(grafoAmici.getMap().get(nome).getnAmici()>0)
				{
					json=gson.toJson(grafoAmici.getListaAmici(nome));
					
					buf.put(json.getBytes());
				}
			else
				buf.put("vuota".getBytes());
		
		}
	
		else
					
			buf.put(("errore").getBytes());
			
			
		
		key.attach(buf);
			
	}//fine lista amici
	
	
	/**
	 * inizializza la sfida tra due utenti
	 * @param key
	 * @param in
	 * @param buf
	 * @throws IOException
	 */
	private static void sfida(SelectionKey key,String in,ByteBuffer buf) throws IOException{
		
			
		String nome=split(in,2);
		String amico=split(in,3);
		buf.clear();
		SelectionKey keyAmico=null;
		
		if(grafoAmici.getMap().containsKey(amico))
			keyAmico= grafoAmici.getMap().get(amico).getKey();

		if(nome.compareTo(amico)==0)
			buf.put("errore: non puoi sfidare te stesso.".getBytes());
		
		else if(!grafoAmici.isAmico(nome, amico))
			buf.put("errore: utente non presente nella tua lista amici.".getBytes());
		 
		
		else if(keyAmico!=null && Connessi.containsKey(keyAmico))	
		{
			key.interestOps(0);
			
			TaskSfida taskSfida = new TaskSfida(true,
												dizionario,
												key,
												nome,
												amico,
												grafoAmici,
												Connessi,
												null,
												null,
												null,
												0,
												threadBackup0,
												threadBackup1,
												keyAmico);
			Thread threadSfidante=new Thread(taskSfida);
			threadSfidante.start();
		}	
			
			
		else
			
			buf.put("errore: utente non connesso.".getBytes());
			
		key.attach(buf);
				
		}//fine sfida

	
	
	/**
	 * ritorna al client il proprio punteggio
	 * @param key
	 * @param input
	 * @param buf
	 * @throws IOException
	 */
	private static void mostra_punteggio(SelectionKey key,String input,ByteBuffer buf) throws IOException{
		
		String nome=split(input,2);
		buf.clear();
	

		if(grafoAmici.getMap().containsKey(nome) && Connessi.containsKey(key))	
		{
			
			
			String punteggio=grafoAmici.getPunteggio(nome);
						
					
					buf.put(punteggio.getBytes());
					
		}
		else
						
			buf.put(("errore").getBytes());
			
	
		
		key.attach(buf);
			
	}//fine mostra_punteggio
	
	
	
	/**
	 * invia al client un json contentente la classifica punteggi della sua lista amici
	 * @param key
	 * @param input
	 * @param buf
	 * @throws IOException
	 */
	private static void mostra_classifica(SelectionKey key,String input,ByteBuffer buf) throws IOException{
		
		String nome=split(input,2);
		buf.clear();

		if(grafoAmici.getMap().containsKey(nome) && Connessi.containsKey(key))	
		{
			
			Gson gson=new Gson();
			String json=null;
						
			if(grafoAmici.getMap().get(nome).getnAmici()>0)
				{
					json=gson.toJson(grafoAmici.getClassifica(nome));
					
					buf.put(json.getBytes());
				}
			else
				buf.put("vuota".getBytes());
		
		}
		else
			
			buf.put(("errore").getBytes());
			
			
			
		
		key.attach(buf);
			
	}//fine mostra_classifica
	
	
	
	
	/**
	 * 
	 * @param nomedizionario dizionario di parole
	 * @return lista contenente le parole del dizionario 
	 * @throws IOException
	 */
	public static List<String> leggiDizionario(String nomedizionario) throws IOException 
	{
			String contents = new String(Files.readAllBytes(Paths.get(nomedizionario)));
			String[] array=new Gson().fromJson(contents, String[].class);
			List<String> lista =new LinkedList<String>(getList(Arrays.toString(array).replace(",","").replace("[", "").replace("]", "").trim()));
			
			return lista;
		
		}//fine leggiDizionario
	
	
	
	/**
	 * 
	 * @param mystring stringa di parole 
	 * @return lista di parole contenute in "mystring"
	 */
	public static List<String> getList(String mystring) {
		//c'è anche split senza argomento
		String arr[] = mystring.split(" ");
		List<String> list = Arrays.asList(arr); 
	return list;
	
	}//fine getList
	
	
	
	
	
	
								/////////////////////////////////////
										//METODI DI SUPPORTO 
	/**
	 * 
	 * @param buffer
	 * @return stringa letta da "buffer"
	 * @throws IOException
	 */
	public static String getString(ByteBuffer buffer) throws IOException{
		
		buffer.flip();
		
		byte[] stringa=new byte[buffer.remaining()];
	
		while(buffer.hasRemaining())
		{
			
			buffer.get(stringa);
		}
		return new String(stringa,"UTF-8");
		
		
	}//fine getString
	
	
	/**
	 * 
	 * @param mystring
	 * @param numParola
	 * @return numParola-sima parola di "mystring" 
	 */
	 public static String split(String mystring ,int numParola) {

		String arr[] = mystring.split(" ");
		if(arr.length<numParola)
			return "Errore"; 
		
	return arr[numParola-1];
	}//fine split
	
	
	
	
	
	 /**
	  * 
	  * param grafo amicizie
	  * @return grafo salvato con json se riesce ottenerlo,  "grafo" altrimenti
	  * @throws IOException
	  */
	public static GrafoAmici leggiBackup(GrafoAmici grafo) throws IOException 
	{
		
		GrafoAmici grafo1=new GrafoAmici();
		if(Files.exists(Paths.get("backup0.json")))
		{

			String contents = new String(Files.readAllBytes(Paths.get("backup0.json")));

			try{grafo1=new Gson().fromJson(contents, GrafoAmici.class);	}
			
			catch(Exception e) 
			{

				if(Files.exists(Paths.get("backup1.json")))
				{	
					contents = new String(Files.readAllBytes(Paths.get("backup1.json")));

					try{grafo1=new Gson().fromJson(contents, GrafoAmici.class);}
					
					catch(Exception ej) 
					{				
						return grafo;
					}
				}

				else
					
					return grafo;
				
			}
			
			return grafo1;
		}



		else if(Files.exists(Paths.get("backup1.json")))
		{

			String contents = new String(Files.readAllBytes(Paths.get("backup1.json")));

			try{ grafo1=new Gson().fromJson(contents, GrafoAmici.class);}	

			catch(Exception e){	return grafo;}
			
			return grafo1;
		}

		
		return grafo;
	}//fine leggiBackup

	
	
	/**
	 * 
	 * @param taskJson0 effettua backup #1
	 * @param taskJson1 effettua backup #2
	 */
	public static void backup() {

		
		if(!threadBackup0.isAlive())
		{

			threadBackup0 = new TaskBackup(grafoAmici,0);;
			threadBackup0.setDaemon(true);
			threadBackup0.start();	
	
	
		}
		if(!threadBackup1.isAlive())
		{

			threadBackup1 =new TaskBackup(grafoAmici,1);;
			threadBackup1.setDaemon(true);
			threadBackup1.start();
		}	

	}
	
	
	
}//fine classe


