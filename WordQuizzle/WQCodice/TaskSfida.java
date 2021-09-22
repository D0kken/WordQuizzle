package wq;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;



public class TaskSfida implements Runnable {
	
										//VARIABILI	
	
	private boolean sfidante;									//true se il thread corrente gestisce lo sfidante, false altrimenti
	private int tempoSfida=Variabili.tempoSfida;				//durata in ms della sfida
	private SelectionKey key;									//SelectionKey dell'utente gestito dal thread corrente
	private SelectionKey keySfidato;							//SelectionKey dello Sfidato
	private ConcurrentHashMap<SelectionKey, String> Connessi;	//hash contenente gli utenti al momento collegati
	private HashMap<String, List<String>> matches;				//hash contenente le traduzioni richieste tramite http
	private String nick;										//nome dello Sfidante
	private String amico;										//nome dello Sfidato
	private List<String> dizionario;							//dizionario di parole
	private List<String> traduzioni;							//lista contentente le K traduzioni delle K parole randomiche
	private List<Integer> index; 								//lista di numeri random 
	private Integer punteggioSfidante=0;
	private Integer punteggioSfidato=0;
	private Integer timeOut=Variabili.timeOut; 					//ms dopo i quali la richiesta scade
	private Integer maxTry=Variabili.maxTry;					//numero massimo di tentativi di riconnessione, prima di considerare terminata la sfida
	private Integer numeroParole=Variabili.numeroParole;		//numero di parole per la sfida
	private GrafoAmici grafo;									//grafo delle amicizie
	private TaskBackup Backup0; 								//thread che esegue il backup #0
	private TaskBackup Backup1;									//thread che esegue il backup #1
	
						/////////////////////////////////////////////////
										//COSTRUTTORE
	
	
	public TaskSfida(
				boolean a,
				List<String> wordlist,
				SelectionKey b,
				String nome,
				String amico,
				GrafoAmici c,
				ConcurrentHashMap<SelectionKey, String> hashconnessi,
				List<Integer> i,
				List<String> trad,
				HashMap<String,List<String>> match,
				Integer punteggio,
				TaskBackup backup0,
				TaskBackup backup1,
				SelectionKey keyAmico) 
	{	
		this.sfidante=a;
		this.dizionario=wordlist;
		this.key=b;
		this.grafo=c;
		this.nick=nome;
		this.amico=amico;
		this.Connessi=hashconnessi;
		this.matches=match;
		this.index=i;
		this.traduzioni=trad;
		this.punteggioSfidato=punteggio;
		this.Backup0=backup0;
		this.Backup1=backup1;
		this.keySfidato=keyAmico;
	}
						/////////////////////////////////////////////////
										//METODO RUN


@Override 
public void run()
{

	this.key.interestOps(0);
	
										//GESTIONE SFIDANTE
	if(this.sfidante)
	{	
		try{ 
										//SFIDA NON ANDATA A BUON FINE
			if(!sfidante())
				{	
				SocketChannel client = (SocketChannel) this.key.channel();	
				ByteBuffer buffer=ByteBuffer.allocate(128);				
				buffer.put("errore".getBytes());
				buffer.flip();
				this.keySfidato.interestOps(0);	
				client.write(buffer);
				this.key.interestOps(SelectionKey.OP_READ);	
				return;
				}
										//CALCOLO DEL VINCITORE
			vincitore();
			
										//INVIO RISULTATI A NICK SFIDANTE
			risultatiSfidante();

										//INVIO RISULTATI A AMICO SFIDATO
			risultatiSfidato();
		
	
			System.out.println("Sfida terminata "+this.nick+" ha totalizzato: "+this.punteggioSfidante+" punti ");	
			System.out.println("Sfida terminata "+this.amico+" ha totalizzato: "+this.punteggioSfidato+" punti ");	
			
		

		}
		
		catch (IOException e) {e.printStackTrace();}
	
		catch (InterruptedException e) {e.printStackTrace();}
	
	}	
	
	
	
										//GESTIONE SFIDATO
	else 
			sfidato();
	
				
	
}//fine run




															/////////////////////////////////////////////////
																		//METODI PRINCIPALI

	/**
	 * 
	 * 
	 * gestione della sfida lato Sfidante
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public boolean sfidante() throws IOException, InterruptedException { 
	

									//INIZIALIZZO UDP
		String str=("Sfida "+this.nick);
		Allegato sfidato=this.grafo.getMap().get(this.amico);
		int port=sfidato.getPorta();
		InetAddress address =sfidato.getAddress();



									//INVIO UDP
		try 
		{
			byte[] buf = new byte[str.length()];
			buf=str.getBytes();
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
			socket.send(packet);



							//RICEVO RISPOSTA UDP 
			buf=new byte[128];
			packet=new DatagramPacket(buf,buf.length);
			socket.setSoTimeout(timeOut);
			
			System.out.println("Attendo risposta...");
		
			socket.receive(packet);
			
			String received = new String(packet.getData(), 0, packet.getLength());
			
			socket.close();

	
							//RICHIESTA RIFIUTATA 
			if (split(received,1).compareToIgnoreCase("Rifiuto")==0) 

				{	
					rifiuto();    
					return false;
				}



							//RICHIESTA ACCETTATA 
			else if (split(received,1).compareToIgnoreCase("Accetto")==0) 
				{
	
					random();
					
					if(!leggiTraduzioni())
						{
							System.out.println("Sospensione della sfida.");
							SocketChannel clientSfidato = (SocketChannel) this.keySfidato.channel();	
							ByteBuffer bufferSfidato=ByteBuffer.allocate(128);				
							bufferSfidato.put("errore".getBytes());
							bufferSfidato.flip();
							this.keySfidato.interestOps(0);	
							clientSfidato.write(bufferSfidato);
							this.keySfidato.interestOps(SelectionKey.OP_READ);				
							return false;
						}
			

					TaskSfida taskSfida = new TaskSfida(
														false,
														this.dizionario,
														this.keySfidato,
														this.amico,
														this.nick,
														this.grafo,
														this.Connessi,																						
														this.index,
														this.traduzioni,
														this.matches,
														this.punteggioSfidato,
														null,
														null,
														null);
					
					Thread threadSfidato=new Thread(taskSfida);
					threadSfidato.start();
				
					accetto();
		
					threadSfidato.join();
			
					this.punteggioSfidato=taskSfida.getPunteggioSfidato();
					
				}
			
		}

						//TEMPO SCADUTO 

		catch (SocketTimeoutException e1)
		{					
				timeout();
				return false;
		}


		catch (IOException e) {e.printStackTrace();}
		
		
		return true;
		
	}//fine  sfidante

	


	/**
	 * 
	* 
	* gestione della sfida lato Sfidato
	*/
	public void sfidato() {

		accetto();
	}//fine  sfidato

	

	/**
	 * 
	 * 
	 *  gestione della sfida nel caso in cui venga rifiutata, sia lato Sfidante 
	 */
	public void rifiuto() {

   	 
		String risposta="Richiesta rifiutata";
		SocketChannel client = (SocketChannel) this.key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(risposta.length());
		buffer.put(risposta.getBytes());
		buffer.flip();  
	
		try{client.write(buffer);}
		
		catch (IOException e){e.printStackTrace();}
		
		return;
	 
		
	}//fine  rifiuto
	
	
	

	/**
	 * gestione della sfida nel caso in cui venga accettata, sia lato Sfidante che Sfidato
	 */
	public void accetto(){
					

		int i=0;
		String parola=this.dizionario.get(this.index.get(i));
		String risposta=null; 
		String tradotta=null;
		int bLetti=0;
		SocketChannel client = (SocketChannel) this.key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(128);

		if(this.sfidante)
			risposta="Richiesta accettata "+parola;

		else 
			risposta=parola;


		buffer.put(risposta.getBytes());
		buffer.flip(); 
		List<String> list=null;
		try 
		{
			client.write(buffer);
			buffer.clear(); 
					
			long startTime = System.currentTimeMillis();

			while(i<this.numeroParole && System.currentTimeMillis()<startTime+this.tempoSfida)
			{

				bLetti=client.read(buffer);	
	
				if(!(System.currentTimeMillis()<startTime+this.tempoSfida))
					break;
	
				if(bLetti == -1) 
					return;

				else if(bLetti>0) 
				{
					buffer.flip();
					tradotta=getString(buffer);
					list=(List<String>)this.matches.get(parola);

					if(contiene(list,(String)tradotta))
		
						incrementaPunteggio();
		
					else 
						decrementaPunteggio();
					
					i++;
		
					if(i==this.numeroParole)
					{
						buffer.clear();
						buffer.put("Attesa".getBytes());
						buffer.flip();
						client.write(buffer);
						buffer.clear();
						break;
					}
					else
						parola=this.dizionario.get(this.index.get(i));
	
					buffer.clear();
					buffer.put(parola.getBytes());
					buffer.flip();
					client.write(buffer);
					buffer.clear();
				}

				Thread.sleep(500);
			}

		}


			
		catch (IOException e){
			System.out.println("Disconnessione da host remoto");	
			this.Connessi.remove(this.key);
			this.key.cancel();
		} 

		catch (InterruptedException e) {e.printStackTrace();}
		

		return;
	}//fine  accetto
	


			
		
	/**
	 * 
	 * richiesta di sfida scaduta
	 * @throws IOException 
	 */
	public void timeout() throws IOException {

		String risposta="Richiesta scaduta";
		ByteBuffer buffer = ByteBuffer.allocate(risposta.length());
		
		buffer.put(risposta.getBytes());
		SocketChannel client = (SocketChannel) this.key.channel();
		buffer.flip();
		try 
			{
				client.write(buffer);
			}
		
		catch (IOException e) 
			{
				e.printStackTrace();
			}

		return;
	}//fine  timeout
	
	
	

	/*
	 * 
	 * riempie this.index con K numeri random
	 */
	public void random() {
		
		this.index= new  ArrayList<Integer>();
		
		for(int i=0;i<this.numeroParole;i++)
		
			this.index.add((int)randomFill());
		
	}//fine random
	
	
	
	
	/**
	 * fornisce un numero randomico non già fornito
	 * @return
	 */
    public  double randomFill(){

    Random rand = new Random();

    int randomNum = rand.nextInt(this.dizionario.size());
   
    while(this.index.contains(randomNum))
	   randomNum=rand.nextInt(this.dizionario.size());
    
   
    return randomNum;
    }//fine randomFill

	

	/**
	 * tramite una get http aggiorna la variabile this.matches con la lista di parole tradotte 
	 * @return true se va a buon fine, false altrimenti
	 * @throws IOException
	 */
	public  boolean leggiTraduzioni() throws IOException
	{
		
		HttpGET con=new HttpGET();
		this.matches=new HashMap<String,List<String>>();
		String esito=con.sendGET(this.dizionario,this.numeroParole,this.index,this.matches);
			int start=10;
			int incremento=0;
			int conto=10;
			int Try=1;
			while(esito.compareTo("Connessione_assente")==0 )	
				{
					if(Try>this.maxTry)
						return false;
				
					if(conto==0)
						{
							incremento+=10;
							conto=start+incremento;
						}
					
					try 
					{
				
					System.out.println("Tentativo di riconnessione "+Try+"/"+this.maxTry);
				
					while(conto>0)
					{
						Thread.sleep(1000);
						System.out.println(conto);
						conto--;
					}
					
					
						esito=con.sendGET(this.dizionario,this.numeroParole,this.index,this.matches);
					} 
					
					catch (InterruptedException e) 
					{

					e.printStackTrace();
					}
					Try++;
				}
			
			if(esito.compareTo("GET_fail")==0)
				{
					System.out.println("Traduzione non disponibile");
					return false;
				}
		
			return true;
	}//fine leggiTraduzioni
	
	

	
	
	public Integer getPunteggioSfidato() {
		return this.punteggioSfidato;
	}//fine getPunteggioSfidato
	
	
															////////////////////////////////////
																	//METODI DI SUPPORTO
	
	
	/**
	 * 
	 * @param buffer
	 * @return stringa letta da "buffer"
	 * @throws IOException
	 */
	public String getString(ByteBuffer buffer) throws IOException{
		
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
	 * @return numParola-sima parola di una stringa
	 */
	public String split(String mystring ,int numParola) {
		
		String arr[] = mystring.split(" ");

	return arr[numParola-1];
	}//fine split
	
	
	
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
	
	
	
	
	
	/**
	 * 
	 * @param list lista parole tradotte
	 * @param x parola da ricercare in "list"
	 * @return true se "list" contiene "parola", false altrimenti
	 */
	public boolean contiene(List<String> list,String parola) {
		for(int i=0;i<list.size();i++)
			{
			if(list.get(i).compareToIgnoreCase(parola)==0)
				return true;
			}
		return false;
	}//fine contiene
	
	
	
	
	/**
	 * calcola il vincitore, assegna punti bonus, li salva nel grafo delle amicizie
	 * e chiama i thread di backup dei dati del grafo amicizie
	 * @throws RemoteException
	 */
	public void vincitore() throws RemoteException {
		
		if(this.punteggioSfidante>this.punteggioSfidato)
		{
		System.out.println("vincitore"+this.nick);
			this.grafo.setPunti(this.nick,this.punteggioSfidante+(this.punteggioSfidante-this.punteggioSfidato));
			this.grafo.setPunti(this.amico,this.punteggioSfidato);
		}
		
		else if (this.punteggioSfidante<this.punteggioSfidato)
		{
		System.out.println("vincitore"+this.amico);
		this.grafo.setPunti(this.nick,this.punteggioSfidante);
		this.grafo.setPunti(this.amico,this.punteggioSfidato+(this.punteggioSfidato-this.punteggioSfidante));
		}
		
		else 
		{
		System.out.println("pareggio");
		this.grafo.setPunti(this.nick,this.punteggioSfidante);
		this.grafo.setPunti(this.amico,this.punteggioSfidato);
		}
 
		backup();
		
	  }//fine vincitore
	
	
	
	/**
	 * incrementa il punteggio del giocatore
	 */
	public void incrementaPunteggio() {
		
		if(this.sfidante)
			
			this.punteggioSfidante++;	
	
		else 
		
			this.punteggioSfidato++;
		
	}//fine incrementaPunteggio
	
	
	
	/**
	 * decrementa il punteggio del giocatore
	 */
	public void decrementaPunteggio() {
		
		if(this.sfidante)
			
			this.punteggioSfidante--;	
	
		else 
		
			this.punteggioSfidato--;
		
	}//fine decrementaPunteggio
	
	
	
	/**
	 * invia il risultato della sfida allo Sfidante
	 * @throws IOException
	 */
	public void risultatiSfidante() {
	
		if(this.key.isValid())
		{
			String risultatoSfidante="fine "+this.nick+" "+this.punteggioSfidante +" "+this.amico+" "+this.punteggioSfidato;
			SocketChannel clientSfidante = (SocketChannel) this.key.channel();
			ByteBuffer bufferSfidante=ByteBuffer.allocate(128);
			bufferSfidante.put(risultatoSfidante.getBytes());
			bufferSfidante.flip();		
		
			try {clientSfidante.write(bufferSfidante);}
			
			catch (IOException e) {System.out.println("Connessione interrotta dall'host remoto");}
			
			this.key.interestOps(SelectionKey.OP_READ);	
		}
		
	}//fine risultatiSfidante
	
	
	
	/**
	 * invia il risultato della sfida allo Sfidato
	 * @throws IOException
	 */
	public void risultatiSfidato() {
		
		if(this.keySfidato.isValid())
		{
			String risultatoSfidato ="fine "+this.amico+" "+this.punteggioSfidato+" "+this.nick+" " +this.punteggioSfidante;
			SocketChannel clientSfidato = (SocketChannel) this.keySfidato.channel();	
			ByteBuffer bufferSfidato=ByteBuffer.allocate(128);
			bufferSfidato.put(risultatoSfidato.getBytes());
			bufferSfidato.flip();
			
			try {clientSfidato.write(bufferSfidato);}
			
			catch (IOException e) {System.out.println("Connessione interrotta dall'host remoto");}
			
			this.keySfidato.interestOps(SelectionKey.OP_READ);	
		}
		
	}//fine risultatiSfidato
	
	

	
	/**
	 * 
	 * @param taskJson0 effettua backup #1
	 * @param taskJson1 effettua backup #2

	 */
	public void backup() {

			
			if(!this.Backup0.isAlive())
			{

				this.Backup0 = new TaskBackup(this.grafo,0);;
				this.Backup0.start();
			}
			if(!this.Backup1.isAlive())
			{

				this.Backup1 =new TaskBackup(this.grafo,1);;
				this.Backup1.start();
			}	

		}//fine backup
		
		
	
	
	
	}//fine classe
	
	
	
	
	
