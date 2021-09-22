package wq;

public class Variabili {

	
	public static Integer TCPort=9999;   				 //porta tcp in cui è in ascolto il server
	public static Integer portRMI=9998;					 //porta rmi in cui è in ascolto il server
	public static Integer tempoSfida =60000;			 //durata della sfida in ms
	public static Integer timeOut=10000;				 //ms dopo i quali la richiesta è considerata scaduta
	public static Integer numeroParole=6;				 //numero di parole da tradurre durante la sfida
	public static Integer maxTry=3;						 //numero massimo di tentativi di riconnessione per richiesta HttpGET in TaskSfida
	public static String nomeDizionario="wordlist.json"; //nome del dizionario da cui leggere le parole per la sfida
	
	
}
