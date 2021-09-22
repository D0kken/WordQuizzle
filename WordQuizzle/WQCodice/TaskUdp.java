package wq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TaskUdp extends Thread {

										//VARIABILI
							 /////////////////////////////////////
	
private boolean richiesta;		//indica la presenza di una richiesta 
private boolean gestita; 		//indica se la sfida è stata presa in gestione o meno
private static byte[] buf;		
private Integer portaUDPclient;
private Integer portaUDPserver; 
private DatagramSocket socket;	//per la ricezione del pacchetto di richiesta di sfida
private DatagramPacket packet;	//pacchetto udp di rispsota
private InetAddress address;	//indirizzo al quale inviare il pacchetto UDP di risposta
private Integer timeOut;		//timeout per accettare la sfida
private boolean attesa;			//true se questo thread è in sleep, false altrimenti


							/////////////////////////////////////
										//COSTRUTTORE
	public TaskUdp(boolean richiesta,Integer portaClient, DatagramSocket SocketUDP, Integer timeout) {
		this.richiesta=richiesta;
		this.gestita=false;
		this.portaUDPclient=portaClient;
		this.portaUDPserver=null;
		this.packet=null;
		this.socket=SocketUDP;
		this.timeOut=timeout;
	}
							/////////////////////////////////////
											//RUN
	@Override
	public void run() 
	{							//ASCOLTO UDP
		while(true) 
			{
				if(!this.richiesta) 
					{
					
						buf= new byte[128];
						packet = new DatagramPacket(buf, buf.length);
						
						try{ this.socket.receive(packet);} 
						
						catch (IOException e) 
						{
							System.out.println("Client terminato");
							return;
						}
				
						this.address = packet.getAddress();
						this.portaUDPserver = packet.getPort();
						packet = new DatagramPacket(buf, buf.length, this.address, this.portaUDPserver);
						String received = new String(packet.getData(), 0, packet.getLength());
						if(split(received,1).compareTo("Sfida")==0) 
							{			
								System.out.println("[Notifica!] Sfida ricevuta da: "+split(received,2));
								this.richiesta=true;
								System.out.println("Accetti o Rifiuti?");
								try 
								{
									this.attesa=true;
									Thread.sleep(this.timeOut);
									this.attesa=false;
								}
								
								catch (InterruptedException e) {this.attesa=false;}
					
								if(this.richiesta && !this.gestita)
								{
									System.out.println("[Notifica!] Richiesta scaduta");
									this.richiesta=false;
								}
							}
					}	
				else
					{
					
						try { Thread.sleep(1000);}
						
						catch (InterruptedException e) {}
					}	
		}//fine loop
		
	}//fine run
								
							/////////////////////////////////////
										//METODI
	
	public boolean isSleeping() {
		
		return this.attesa;
	}
	
	
	
	
	public void setGestito(boolean bool) {
		
		this.gestita=bool;
	}	
	
	
	
	public boolean getGestito() {
		
		return this.gestita;
	}	
	
	
	
	public void setRichiesta(boolean x) {
		
		this.richiesta=x;
	}
	
	
	
	public boolean getRichiesta() {
		
		return this.richiesta;
	}

	
	
	public Integer getPortaUDPclient() {
		
		return this.portaUDPclient;
	}

	
	
	public Integer getPortaUDPserver() {
		
		return this.portaUDPserver;
	}
	
	
	
	public InetAddress getAddressServer() {
		
		return this.address;
	} 
	

	
							/////////////////////////////////////
									//METODI DI SUPPORTO
	
	/**
	 * 
	 * @return stringa letta da tastiera
	 * @throws IOException
	 */
	public static  String scan() throws IOException {
		System.out.print("-");
		BufferedReader rdr= new BufferedReader(new InputStreamReader(System.in));		 

		return rdr.readLine();

	}//fine scan

	
	/**
	 * 
	 * @param mystring
	 * @param numParola
	 * @return numParola-sima parola di "mystring" ,null nel caso in cui "numParola" sia oltre il limite consentito
	 */
	public static String split(String mystring ,int numParola) {

		String arr[] = mystring.split(" ");
		if(arr.length<numParola)
			return null;
		return arr[numParola-1];
	}


}//fine classe
	

