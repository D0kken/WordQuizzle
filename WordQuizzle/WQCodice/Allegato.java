package wq;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.util.Set;

public class Allegato {
											//VARIABILI
	private String Password;
	private Set<Arco> archi;			//insieme di archi ovvero relazioni di amicizia
	private Integer punteggio;
	transient private boolean connesso;	
	private InetAddress address;	    //indirizzo dell'utente
	private Integer portaUDP;		    //porta su cui utente è in ascolto UDP
	transient private SelectionKey key; //chiave con cui la Select identifica l'utente
								
								/////////////////////////////////////
											//COSTRUTTORE
	public Allegato() {
		
		this.Password=null;
		this.connesso=false;
		this.archi=null;
		this.address=null;
		this.key=null;
		this.punteggio=0;
		this.portaUDP=null;
	
	}
								/////////////////////////////////////
												//METODI
	
	/**
	 * 
	 * salva la SelectionKey per l'utente
	 * @param key chiave dell'utente
	 */
	public void setKey(SelectionKey key) {
		
		this.key=key;
	}
	
	
	/*
	 * restituisce la SelectionKey dell'utente
	 */
	public SelectionKey getKey() {
		
		return this.key;
	}
	
	
	/**
	 * 
	 * @param port porta UDP utente
	 */
	public void setPorta(int port){
		
		this.portaUDP=port;	
	}
	
	
	/**
	 * 
	 * @return porta UDP
	 */
	public Integer getPorta() {
		
		return this.portaUDP;
	}
	
	
	/**
	 * 
	 * @param x indirizzo dell'utente da inserire
	 */
	public void setAddress(InetAddress x) {
		
		this.address=x;	
	}
	
	
	/**
	 * 
	 * @return indirizzo dell'utente
	 */
	public InetAddress getAddress() {
		
		return this.address;	
	}
	
	
	/**
	 * imposta il valore di connessione
	 * @param b
	 */
	public void setConnectionState(boolean b) {
		
		this.connesso=b;
	}
	
	
	/**
	 * 
	 * @return true se è connesso, false altrimenti
	 */
	public boolean getConnectionState() {
		
		return connesso;
	}
	
	
	/**
	 * 
	 * @return numero di amici
	 */
	public int getnAmici() {
	
		try {return this.archi.size();}
	
		catch (NullPointerException e) {return 0;}
	}
	
	
	
	/**
	 * 
	 * inizializza il punteggio giocatore o lo aggiorna aggiungendo quello nuovo
	 * @param x punteggio
	 */
	public void setPunteggio(int x) {
				
		this.punteggio+=x;
		
	}
	

	
	/**
	 * 
	 * ritorna il punteggio giocatore o lo aggiorna aggiungendo quello nuovo
	 * @param x punteggio
	 */
	public Integer getPunteggio() {
		
		return this.punteggio;
		
	}
	
	
	/**
	 * aggiunge set di archi associati
	 * @param x
	 */
	public void setArco(Set<Arco> x) {
		
		this.archi=x;
	}
	
	
	/**
	 *
	 * @return ritorna gli archi associati
	 */
	public Set<Arco> getSetArco() {
		
		return this.archi;
	}

	
	/*
	 * imposta la password
	 */
	public void setPass(String x) {
		
		this.Password=x;
	}
	
	
	/**
	 * @return ritorna il valore della password
	 */
	public String getPass() {
		
		return this.Password;
	}


}
