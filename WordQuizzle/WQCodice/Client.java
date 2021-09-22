package wq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;
import com.google.gson.*;



public class Client  {
	
										//VARIABILI

	private static Integer TCPort=Variabili.TCPort;			//porta utilizzata dalla select
	private static Integer portaUDPclient;
	private static Integer portaUDPserver;
	private static Integer portRMI=Variabili.portRMI;		//porta utilizzata per RMI
	private static Integer tempoSfida=Variabili.tempoSfida; //durata della sfida in ms
	private static Integer timeOut=Variabili.timeOut;		//tempo in ms dopo i quali la richiesta, se non risposta, viene rifiutata
	private static InetAddress serverAddress=null;		    //indirizzo del Server
	private static String accettazione=null;				//variabile di appoggio per la risposta alla richiesta di sfida
	private static boolean richiesta;						//indica la presenza di una richiesta di sfida
	private static DatagramSocket socketUDP=null;			//socket per ascolto delle richieste UDP
	
	

	public static void main (String[] args) 
	{
		
	
	if(args.length!=0 && args[0].compareToIgnoreCase("--help")==0){
		System.out.print("Comandi:\n"
			+ "-registra_utente <nickUtente> <password> :registra l'utente\n"
			+ "-login <nickUtente> <password> :effettua il login\n"
			+ "-logout :effettua il logout e chiusura del client\n"
			+ "-aggiungi_amico <nickAmico> :crea relazione di amicizia con nickAmico\n"
			+ "-lista_amici :mostra la lista dei propri amici\n"
			+ "-sfida <nickAmico> :richiesta di una sfida a anickAmico\n"
			+ "-mostra_punteggio :mostra il punteggio dell'utente\n"
			+ "-mostra_classifica :mostra una classifica degli amici dell'utente"	
			+ "-accetto: per accettare la richiesta di sfida"
			+ "-rifiuto: per rifiutare la richiesta di sfida"
			+ "-exit: per chiudere il client");
		
		return;
		}
		
		
		
									

	try 
	{	
											//SETTING UDP

		    portaUDPclient=null;
			Scanner scan;
			System.out.println("Scegli porta UDP");			
			while(true) 
			{

				if(portaUDPclient==null)
				{
					try{
						scan=new Scanner(System.in);
						portaUDPclient=scan.nextInt();	
					}
					catch(Exception InputMismatchException ){
				
						System.out.println("Scrivere numero di porta UDP");
					}

				}
				else 
				{
					try 
					{
						socketUDP = new DatagramSocket(portaUDPclient);
						break;
					}
					catch (SocketException  | IllegalArgumentException e2)
					{
						
						System.out.println("porta non valida, scegline un'altra");
						portaUDPclient=null;
					}
					
				}

			}

		
		InetAddress inetAddress=InetAddress.getLocalHost();	

											
											//RMI

		Registry reg = LocateRegistry.getRegistry(portRMI);	
		GrafoAmiciRMI grafo = (GrafoAmiciRMI) reg.lookup(GrafoAmiciRMI.SERVICE_NAME);
		System.out.println("Sei un nuovo utente? Y/N" );	
		String letto=null;
		String esito=null;
		boolean nuovo;
		
		while(true)
	 		{
				letto=scan();
				
				if(letto!=null && letto.compareToIgnoreCase("Y")==0)
					{
						nuovo=true;
						break;
					}
				if(letto!=null && letto.compareToIgnoreCase("N")==0)
					{
					nuovo=false;
					break;
					}
				System.out.println("Input non valido");
	 		}
	
									//SE NUOVO REGISTRA UTENTE
		String nome="start";
		if(nuovo)	
		{	
			System.out.println("Benvenuto, effettua la registrazione" );
			while(true)
			{
				while((letto=scan())==null)
					System.out.println("Input non valido");
		
				if(split(letto,1).compareToIgnoreCase("registra_utente")==0)
				{	
					esito=grafo.registraUtente(split(letto,2), split(letto,3),inetAddress,portaUDPclient);	
				
				
					if(esito.compareToIgnoreCase("effettua login")==0)
						{
							nome=split(letto,2);	
							break;
						}
					System.out.println(esito);
					
	
				}
				else if(letto.compareToIgnoreCase("exit")==0)
					return;
				
				else
					System.out.println("Input non valido");
			}
		
		}
							//CONNESSIONE AL SERVER
		SocketAddress address = new InetSocketAddress(TCPort);
	
		SocketChannel client = SocketChannel.open(address);	

		while(!client.finishConnect()) {}

		ByteBuffer buffer = ByteBuffer.allocate(128);
		
		
									//LOGIN
		
		if(!nuovo)
			System.out.println("Bentornato, effettua il login");
		else
			System.out.println("Adesso effettua il login");
		
		while(true)
		{
			
			while(true)
	 		{
				letto=scan();
				
				if(split(letto,1)!=null && split(letto,1).compareToIgnoreCase("exit")==0) 	
					 return;
	
				if(split(letto,1)!=null && split(letto,2)!=null && split(letto,3)!=null)
						 break;
				
				System.out.println("Input non valido");
	 		}
	
			if( split(letto,1).compareToIgnoreCase("login")==0) 	
			{		
				if(nuovo && split(letto,2).compareTo(nome)!=0)
					
					System.out.println("Nick non valido");
			
				else if(login(client,split(letto,2),split(letto,3),buffer))
				
					break;
			}	
					
			else
				System.out.println("Input non valido");
		}
		
		nome=split(letto,2);
		
								//CREO THREAD CHE ASCOLTA IN UDP
		TaskUdp threadUdp = new TaskUdp(richiesta,portaUDPclient,socketUDP,timeOut);
		threadUdp.setDaemon(true);
		threadUdp.start();
		
		  
	      
										
		  									//LOOP
		while(true)
			{
							//CONTROLLO CHE LA CONNESSIONE SIA APERTA
				
				if(!client.isOpen()) 
				{	
					return;
				}
			
					
				aggiornaValoriUDP(threadUdp);
				
								//GESTISCO LA RICHIESTA DI SFIDA 
				if(	richiesta
				 && accettazione!=null
				 &&( accettazione.compareToIgnoreCase("accetto")==0 
				   || accettazione.compareToIgnoreCase("rifiuto")==0))
				{
	
				threadUdp.setGestito(true);
				accettaSfida(client,nome,buffer,threadUdp);
				}
		
			
								//LETTURA DELL'OPERAZIONE DA ESEGUIRE
			
				while((letto=scan())==null)
					System.out.println("Input non valido");
												
	
											//LOGOUT
			 if(isLogout(letto) && client.isOpen())
				{	
					if(logout(client,nome,buffer))
						{
							
							client.close();
							return;
						}
				}
				
						
										//AGGIUNGI AMICO  
				else if(isAggiungiAmico(letto) && client.isOpen())
				{
					String amico=split(letto,2);
					aggiungi_amico(client,nome,amico,buffer);
				}
		
				
				
									
										//LISTA AMICI 
				else if(isListaAmici(letto) && client.isOpen()) 
				
					lista_amici(client,nome,buffer);
			
							
				
										   //SFIDA 
				else if(isSfidante(letto) && client.isOpen())
				{
					String amico=split(letto,2);
					sfida(client,nome,amico,buffer,threadUdp);
				}
				
				
				
										//ACCETTA SFIDA
				else if((split(letto,1).compareToIgnoreCase("Accetto")==0 
					 ||  split(letto,1).compareToIgnoreCase("Rifiuto")==0)
					 &&  client.isOpen())
				
					
					accettazione=split(letto,1); 
			
						
			
										//MOSTRA_PUNTEGGIO
				else if(isMostraPunteggio(letto) && client.isOpen()) 
				
					mostra_punteggio(client,nome,buffer);
		
	
			
										//MOSTRA_CLASSIFICA
				else if(isMostraClassifica(letto) && client.isOpen())
				
					mostra_classifica(client,nome,buffer);
		
				
							
				
											//EXIT
				else if(split(letto,1).compareToIgnoreCase("Exit")==0 && client.isOpen())
				{
						logout(client,nome,buffer);
						client.close();
						return;
				}	
				
				
				else if(split(letto,1).compareToIgnoreCase("help")==0){
					System.out.println("Comandi:\n"
							+ "-registra_utente <nickUtente> <password> :registra l'utente\n"
							+ "-login <nickUtente> <password> :effettua il login\n"
							+ "-logout :effettua il logout e chiusura del client\n"
							+ "-aggiungi_amico <nickAmico> :crea relazione di amicizia con nickAmico\n"
							+ "-lista_amici :mostra la lista dei propri amici\n"
							+ "-sfida <nickAmico> :richiesta di una sfida a anickAmico\n"
							+ "-mostra_punteggio :mostra il punteggio dell'utente\n"
							+ "-mostra_classifica :mostra una classifica degli amici dell'utente\n"	
							+ "-accetto: per accettare la richiesta di sfida\n"
							+ "-rifiuto: per rifiutare la richiesta di sfida\n"
							+ "-exit: per chiudere il client");
						
					
						}
						
				
				else System.out.println("Operazione non riconosciuta..riprova");
					
								
			}
						
	}

	catch (ConnectException ce) {
	
		System.out.println("Host non raggiungibile");
		return;
	}
	
	catch (InterruptedException e)	{e.printStackTrace();} 
	
	catch (NotBoundException e) 	{e.printStackTrace();} 

	catch (IOException e) 			{e.printStackTrace();}

} //fine run
									
	
								  /////////////////////////////////////
											//METODI PRINCIPALI
		

	/**
	 * 
	 * @param c canale di comunicazione
	 * @param nome nick client
	 * @param pass password client
	 * @param buf buffer su cui si appoggia la comunicazione
	 * @return true se è andato a buon fine, false altrimenti
	 * @throws IOException
	 */
	public static boolean login(SocketChannel c,String nome,String pass,ByteBuffer buf) throws IOException {

		
		buf.put(("Login "+nome+" "+pass+" "+Integer.toString(portaUDPclient)).getBytes());
		
		buf.flip();
	
		while(buf.hasRemaining())			
		
			c.write(buf);
			
		if(buf.hasRemaining())
				
			buf.compact();
			
		else
			buf.clear();
			
			
		int bLetti=c.read(buf);

		if(bLetti == -1) 
			{
				
				c.close();		
				return false;
			}		
							
		buf.flip();

		String in=getString(buf);
			
		System.out.println(in);
			
		buf.clear();
			
		return in.compareToIgnoreCase("Connesso")==0;
		}

	
	
	/**
	 * 
	 * @param c 
	 * @param nome 
	 * @param buf 
	 * @return true se va a buon fine, false altrimenti
	 * @throws IOException
	 */
	public static boolean logout(SocketChannel c,String nome,ByteBuffer buf) throws IOException {
		
		
		buf.put(("Logout "+nome).getBytes());
		
		buf.flip();
	
		while(buf.hasRemaining())			
		
				c.write(buf);
			
		if(buf.hasRemaining())
				
				buf.compact();
			
		else
				buf.clear();
			
			
		int bLetti=c.read(buf);

		if(bLetti == -1) 
			{
				
				c.close();		
				return false;
			}		
							
		buf.flip();

		String str=getString(buf);
			
		System.out.println(str);
			
		buf.clear();
			
		return str.compareToIgnoreCase("Disconnesso")==0;
	}//fine logout

	
	
	/**
	 * 
	 * @param c 
	 * @param nome 
	 * @param amico nome amico da aggiungere
	 * @param buf 
	 * @throws IOException
	 */
	public static void aggiungi_amico(SocketChannel c,String nome,String amico,ByteBuffer buf) throws IOException{
		
		buf.put(("aggiungi_amico "+nome+" "+amico).getBytes());
		
		buf.flip();
	
		while(buf.hasRemaining())			
		
				c.write(buf);
			
		if(buf.hasRemaining())
				
				buf.compact();
			
		else
				buf.clear();
			
			
		int bLetti=c.read(buf);

		if(bLetti == -1) 
			{
				
				c.close();		
				return ;
			}		
							
		buf.flip();

		String in=getString(buf);
			
		System.out.println(in);
			
		buf.clear();
			
		return ;
	}
	
	
	
	/**
	 * 
	 * @param c
	 * @param nome
	 * @param buf
	 * @throws IOException
	 */
	public static  void lista_amici(SocketChannel c,String nome,ByteBuffer buf) throws IOException{

		
		buf.put(("lista_amici "+nome).getBytes());
		
		buf.flip();
	
		while(buf.hasRemaining())			
		
				c.write(buf);
			
		if(buf.hasRemaining())
				
				buf.compact();
			
		else
				buf.clear();
			
			
			
		int bLetti=c.read(buf);

		if(bLetti == -1) 
			{
				
				c.close();		
				return;
			}		
							
		buf.flip();
		
			
		String in=getString(buf);
			
		if(in.compareToIgnoreCase("vuota")==0)
			{
				System.out.println("Lista amici: "+in);
				buf.clear();
				return;
			}

		else if(in.compareToIgnoreCase("errore")==0)
			{
				System.out.println(in);
				buf.clear();
				return;
			}
			
		Gson gson=new Gson();
		String[] json=gson.fromJson(in, String[].class);
		System.out.println("Lista amici: "+Arrays.toString(json).replace("[", "").replace("]", "").trim());
		buf.clear();
			
		return;
	}//fine lista amici
	
	
	/**
	 * 
	 * @param c 
	 * @param nome 
	 * @param amico
	 * @param buf 
	 * @throws IOException
	 */
	public static void sfida(SocketChannel c,String nome,String amico,ByteBuffer buf,TaskUdp threadUdp) throws IOException{
		
		try {
			
		buf.put(("sfida "+nome+" "+amico).getBytes());
		
		buf.flip();
	
			while(buf.hasRemaining())			
		
				c.write(buf);
			
			if(buf.hasRemaining())
				
				buf.compact();
			
			else
				buf.clear();
			
	
			
			int bLetti=c.read(buf);
			
				if(bLetti == -1) 
				{
					
					c.close();	
					
				}		
					
		
				
			buf.flip();

			String in=getString(buf);

			buf.clear();
			

													//SFIDA ACCETTATA
			if(split(in,1).compareToIgnoreCase("Richiesta")==0 
			&& split(in,2).compareToIgnoreCase("accettata")==0) 
		
			{
				
				 long startTime = System.currentTimeMillis();
					
				System.out.println("Sfida cominciata, tempo a disposizione " 
									+tempoSfida/1000+" secondi."
									+ "\n parola da tradurre: "+split(in,3));
				
							
				String out=null;
						
					
				while(bLetti!=-1 && System.currentTimeMillis()<startTime+tempoSfida)
					{	
				   							
					
				   		if((System.currentTimeMillis()<startTime+tempoSfida) && in.compareToIgnoreCase("Attesa")!=0)	
				   			{

				   				out=split(scan(),1);
				   				while(out==null)
								{
									System.out.println("Input non valido");
									out=split(scan(),1);
								}
				   				buf.put(out.getBytes()); 
				   				buf.flip();
				   				if(System.currentTimeMillis()<startTime+tempoSfida)
									
				   				c.write(buf); 
				   			}
				   	
				   		else if(!(System.currentTimeMillis()<startTime+tempoSfida))
							
				   				System.out.println("Tempo finito");
				   							   		
				   		else if(in.compareToIgnoreCase("Attesa")==0)
				   		
				   			System.out.println("In attesa che l'avversario finisca la sfida");
				   		
				   		
				   		
				   		buf.clear();
				   		bLetti=c.read(buf);
				   		if(bLetti == -1) 
				  			{
				   				System.out.println("Server down");
				   			
				   				c.close();	
				   				if(threadUdp.isSleeping())
				   					threadUdp.interrupt();
				   				return;
				   			}
				   		
				   		   		
						buf.flip();

						in=getString(buf);
	
							 
						if(split(in,1).compareToIgnoreCase("fine")==0)				
								break;
							
						buf.clear();
					
						if(in.compareToIgnoreCase("Attesa")!=0)
							
							{
					   		System.out.println("tempo rimanente: "
		   							+((tempoSfida-(System.currentTimeMillis()-startTime))/1000)+" secondi ");
					
							System.out.println("parola da tradurre:"+split(in,1));
							}
						
			
		
					}
					
					Integer punteggioSfidante=null;
					Integer punteggioSfidato=null;
					
					if(split(in,2).compareToIgnoreCase(nome)==0)
					{
						punteggioSfidante=Integer.parseInt(split(in,3));
						punteggioSfidato=Integer.parseInt(split(in,5));
					}
					
					else
					{	
						punteggioSfidante=Integer.parseInt(split(in,5));
						punteggioSfidato =Integer.parseInt(split(in,3));
					}	
					
					if(punteggioSfidante>punteggioSfidato)
						System.out.println("Hai vinto la sfida con "+punteggioSfidante+" punti, guadagnando "+(punteggioSfidante-punteggioSfidato)+" punti bonus.");
					else if(punteggioSfidante<punteggioSfidato)
						System.out.println("Hai perso la sfida con "+punteggioSfidante+" punti.");
					else
						System.out.println("Hai pareggiato con il tuo avversario con "+punteggioSfidante+" punti.");
					
					
					threadUdp.setRichiesta(false);
					threadUdp.setGestito(false);
					accettazione=null;
					buf.clear();
					if(threadUdp.isSleeping())
						threadUdp.interrupt();
					return;
				}
	
				else if(split(in,1).compareToIgnoreCase("errore:")==0)
				{

					System.out.println(in);
					
					threadUdp.setRichiesta(false);
					threadUdp.setGestito(false);
					accettazione=null;
					if(threadUdp.isSleeping())
						threadUdp.interrupt();
					return;
				}
										//SFIDA RIFIUTATA
				else 
				{
				
					System.out.println("Sfida rifiutata o scaduta.");
					threadUdp.setRichiesta(false);
					threadUdp.setGestito(false);
					accettazione=null;
					if(threadUdp.isSleeping())
						threadUdp.interrupt();
					
					return;
				}
			
			}
		
			catch(IOException a) 
			{
				
				threadUdp.setRichiesta(false);
				threadUdp.setGestito(false);
				accettazione=null;
				buf.clear();
				if(threadUdp.isSleeping())
					threadUdp.interrupt();
				return;
				
			}
			
		}//fine sfida
	
	
	/**
	 * 
	 * @param c 
	 * @param nome 
	 * @param amico
	 * @param buf 
	 * @param address 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
									
	public static void accettaSfida(SocketChannel c,String nome,ByteBuffer buf,TaskUdp threadUdp) throws IOException, InterruptedException{
		
					
											//SFIDA ACCETTATA
		if(accettazione.compareToIgnoreCase("Accetto")==0)
			{
			System.out.println("Sfida accettata");
			
										//INVIA ACCETTAZIONE CON UDP
			DatagramSocket socket = new DatagramSocket();
			byte[] buffer = new byte[accettazione.length()];
			buffer=accettazione.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length,serverAddress,portaUDPserver);
			socket.send(packet);
			socket.close();
			long startTime=0;
			System.out.println("La sfida sta per cominciare:");
			try 
				{
				
					String out=null;
					String in = null;
					int bLetti=c.read(buf);		
					
					
					buf.flip();
					in=getString(buf);
					
					if(split(in,1).compareToIgnoreCase("errore")==0)
					{
						System.out.println("Sfida interrotta, errore server");
						bLetti=-1;
					}
					else
						{
							System.out.println("Tempo a disposizione: "+ tempoSfida/1000+" secondi");
					
							startTime= System.currentTimeMillis();
						}
						while(bLetti!=-1 && System.currentTimeMillis()<startTime+tempoSfida)
						{	
			
				
							if(split(in,1).compareToIgnoreCase("fine")==0)
	
								break;
							
							buf.clear();
						
							if(in.compareToIgnoreCase("Attesa")!=0)
	
								{
									System.out.println("tempo rimanente: "+((tempoSfida-(System.currentTimeMillis()-startTime))/1000)+" secondi");			
									System.out.println("parola da tradurre="+in);
								}
							
							if((System.currentTimeMillis()<startTime+tempoSfida) && in.compareToIgnoreCase("Attesa")!=0)						
							{
								out=split(scan(),1);
								while(out==null)
									{
										System.out.println("Input non valido");
										out=split(scan(),1);
									}
								buf.put(out.getBytes());				
								buf.flip();
								if(System.currentTimeMillis()<startTime+tempoSfida)
									c.write(buf);
								else
									System.out.println("Tempo finito");
							}
							else if(!(System.currentTimeMillis()<startTime+tempoSfida))
							
								System.out.println("Tempo finito");
		   									
							else if(in.compareToIgnoreCase("Attesa")==0)
							
								System.out.println("In attesa che l'avversario finisca la sfida");
							
							
						
							buf.clear();
				
							bLetti=c.read(buf);
					
							buf.flip();

							in=getString(buf);
						
											 
						}
					
					if(bLetti!=-1)
					{
			
						Integer punteggioSfidante=null;
						Integer punteggioSfidato=null;
				
						if(split(in,2).compareToIgnoreCase(nome)==0)
						{
							punteggioSfidante=Integer.parseInt(split(in,5));
							punteggioSfidato=Integer.parseInt(split(in,3));
						}
						
						else
						{	
							punteggioSfidante=Integer.parseInt(split(in,3));
							punteggioSfidato =Integer.parseInt(split(in,5));
						}	
				
						if(punteggioSfidato>punteggioSfidante)
							System.out.println("Hai vinto la sfida con"+punteggioSfidato+"punti, guadagnando "+(punteggioSfidato-punteggioSfidante)+" punti bonus.");
						else if(punteggioSfidato<punteggioSfidante)
							System.out.println("Hai perso la sfida con "+punteggioSfidato+" punti.");
						else
							System.out.println("Hai pareggiato con il tuo avversario con "+punteggioSfidato+" punti.");
						
			
					}
						buf.clear(); 
						threadUdp.setRichiesta(false);
						threadUdp.setGestito(false);
						accettazione=null;
						buf.clear();
						if(threadUdp.isSleeping())
							threadUdp.interrupt();
						return;
					}
		
			
					catch(IOException a){System.out.println("IOException:Sfida interrotta.");}

	
				
				}
								

										//SFIDA RIFIUTATA
		
			if(accettazione.compareToIgnoreCase("Rifiuto")==0)
				{
					System.out.println("Sfida rifiutata");
				
									//INVIA RIFIUTO TRAMITE UDP
					DatagramSocket socket = new DatagramSocket();
					byte[] buffer = new byte[accettazione.length()];
					buffer=accettazione.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length,serverAddress,portaUDPserver);
					socket.send(packet);
					socket.close();
					threadUdp.setRichiesta(false);
					threadUdp.setGestito(false);
					accettazione=null;
					buf.clear();
					if(threadUdp.isSleeping())
						threadUdp.interrupt();
					return;
				}
		
		
	}//fine accettaSfida
	
	
	
	

	/**
	 *stampa il proprio punteggio
	 * @param c
	 * @param nome
	 * @param buf
	 * @throws IOException
	 */
	public static void mostra_punteggio(SocketChannel c,String nome,ByteBuffer buf) throws IOException {
	
	
		buf.put(("mostra_punteggio "+nome).getBytes());
		
		buf.flip();
	
			while(buf.hasRemaining())			
		
				c.write(buf);
			
			if(buf.hasRemaining())
				
				buf.compact();
			
			else
				buf.clear();
			
			
			int bLetti=c.read(buf);

			if(bLetti == -1) 
				{
				
					c.close();		
					return; 
				}		
							
			buf.flip();

			String in=getString(buf);
			
			if(in.compareToIgnoreCase("errore")==0)
			{
				System.out.println(in);
				buf.clear();
				return ;
			}
			
			System.out.println("Punteggio: "+in);
			
			buf.clear();
			
		
		}//fine mostra_punteggio
	
	
	
	
	
	
	/**
	 * stampa classifica degli amici in ordine decrescente
	 * @param c
	 * @param nome
	 * @param buf
	 * @throws IOException
	 */
	public static void mostra_classifica(SocketChannel c,String nome,ByteBuffer buf) throws IOException{

		
		
		buf.put(("mostra_classifica "+nome).getBytes());
		
		buf.flip();
	
			while(buf.hasRemaining())			
		
				c.write(buf);
			
			if(buf.hasRemaining())
				
				buf.compact();
			
			else
				buf.clear(); 
			
		
		
			int bLetti=c.read(buf);

			if(bLetti == -1) 
				{
					
					c.close();		
					return ;
				}		
							
			buf.flip();
			
			
			String in=getString(buf);
			
			if(in.compareToIgnoreCase("vuota")==0)
				{
					System.out.println("Classifica: "+in);
					buf.clear();
					return ;
				}

			else if(in.compareToIgnoreCase("errore")==0)
			{
				System.out.println(in);
				buf.clear();
				return ;
			}
			
			Gson gson=new Gson();
			String[] json=gson.fromJson(in, String[].class);
			
			System.out.println("Classifica: "+Arrays.toString(json));
			
			buf.clear();
			
			return;
	}//fine mostra_classifica
	
	
	
	
	
	
							//////////////////////////////////////////////
										//METODI DI SUPPORTO
	
	/**
	 * 
	 * @return stringa letta da tastiera
	 * @throws IOException
	 */
	public static  String scan() throws IOException {			
		
		System.out.print("-");
		BufferedReader rdr= new BufferedReader(  					
								new InputStreamReader(System.in));		
		String in=rdr.readLine();
		if(in.length()==0)
			return null;
		return in;		
	}//fine scan

	
	/**
	 * 
	 * @param buffer
	 * @return stringa letta da "buffer"
	 * @throws IOException
	 */
	public static String getString(ByteBuffer buffer) throws IOException{
		
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
	 * @return numParola-sima parola di "mystring", "Errore" nel caso in cui "numParola" sia oltre il limite consentito
	 */
	public static String split(String mystring ,int numParola) {
		
		if(mystring==null)
			return null;
		String arr[] = mystring.split(" ");
		if(arr.length<numParola)
			return null;
		
	return arr[numParola-1];
	}//fine split
	
	
	
	public static boolean isLogout(String letto) {
	
		boolean bool=false;
		
		if(split(letto,1).compareToIgnoreCase("logout")==0
		&& split(letto,2)==null)
				bool=true;
				
		
		return bool;		
	}//fine isLogout
		

	
	public static boolean isAggiungiAmico(String letto) {
		boolean bool=false;
				
				if(split(letto,1).compareToIgnoreCase("aggiungi_amico")==0
				&& split(letto,3)==null)
				
					bool=true;
				
				return bool;
	}//fine isAggiungiAmico

	
	
	public static boolean isListaAmici(String letto) {
		boolean bool=false;
		
		if(split(letto,1).compareToIgnoreCase("lista_amici")==0
		&& split(letto,2)==null)
			bool=true;	
		
		return bool;
	}//fine isListaAmici

	

	public static boolean isSfidante(String letto) {
		boolean bool=false;
		
		if(split(letto,1).compareToIgnoreCase("sfida")==0
		&& split(letto,2)!=null
		&& split(letto,3)==null)
			
			bool=true;	
		
		return bool;
	}//fine isSfida

	
	
	public static boolean isMostraClassifica(String letto) {
		boolean bool=false;
		
		if(split(letto,1).compareToIgnoreCase("mostra_classifica")==0
		&& split(letto,2)==null)
			bool=true;	
		
		return bool;
	}//fine isListaAmici
	
	
	
	public static boolean isMostraPunteggio(String letto) {
		boolean bool=false;
		
		if(split(letto,1).compareToIgnoreCase("mostra_punteggio")==0
		&& split(letto,2)==null)
			bool=true;	
		
		return bool;
	}//fine isListaAmici
	
	
	/**
	 * sincronizza i valori di alcune variabili tra client e taskUdp
	 * @param task
	 */
	public static void aggiornaValoriUDP(TaskUdp threadUdp) {
		serverAddress=threadUdp.getAddressServer();
		portaUDPclient=threadUdp.getPortaUDPclient();
		portaUDPserver=threadUdp.getPortaUDPserver();
		richiesta=threadUdp.getRichiesta();
		
	}//fine aggiornaValoriUDP
	
	

	
}//fine classe
