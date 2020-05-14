package main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;

public class Retrive {

	static String projectName = "geode";
	static String repLocation = "https://github.com/apache/";

	// Prende l'anno ed il mese di inizio progetto
	public static void initDate(logDate date) {

		String startDate = gitCmd.takeFirstCommitDate(projectName);
		date.startYear = Integer.parseInt(startDate.substring(0, 4));
		date.startMonth = Integer.parseInt(startDate.substring(5, 7));
	}

	// Prende l'anno ed il mese dell'ultimo commit
	public static void takeLastDate(logDate date) {

		String endDate = gitCmd.takeLastCommitDate(projectName);
		updateLogDate(endDate, date);
	}

	// aggiorna i valori year e month del logDate utilizzando una data in formato
	// stringa
	public static void updateLogDate(String dateStr, logDate date) {
		date.year = Integer.parseInt(dateStr.substring(0, 4));
		date.month = Integer.parseInt(dateStr.substring(5, 7));
	}

	// Prende i tickets da jira
	public static ArrayList<String[]> ticketsInfo() {

		try {
			ArrayList<String[]> ticketsInfo = jira.takeTicketsInfo(projectName, "Bug");
			return ticketsInfo;
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// prende i dati delle fix usando le date di jira
	public static void jiraAction(int[] commit, ArrayList<String[]> ticketsInfo, logDate date) {
		String endDate;
		int pos;

		for (int i = 0; i < ticketsInfo.size(); i++) { // per ogni tickets
			endDate = ticketsInfo.get(i)[2];

			date.year = Integer.parseInt(endDate.substring(0, 4));
			date.month = Integer.parseInt(endDate.substring(5, 7));

			pos = date.position();

			if (pos < 0) {
				System.out.println("posizione negativa con anno : " + date.year + " e mese: " + date.month);
				System.out.println("e id: " + ticketsInfo.get(i)[0]);
				continue;
			}

			commit[pos]++;
		}
	}

	// prende i dati della fiix tramite i log di git
	public static void takeData(int[] commit, ArrayList<String[]> ticketsInfo, logDate date) {
		String lastDate;
		int pos;
		for (int i = 0; i < ticketsInfo.size(); i++) { // per ogni tickets

			lastDate = null;

			try {
				lastDate = gitCmd.takeLastCommitDateById(ticketsInfo.get(i)[0], projectName);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (lastDate == null) {
				// Utility.printNl("saltato ticket: " + ticketsInfo.get(i)[0]);
				continue;
			}

			updateLogDate(lastDate, date);
			pos = date.position();

			if (pos < 0) {
				System.out.println("posizione negativa con anno : " + date.year + " e mese: " + date.month);
				System.out.println("e id: " + ticketsInfo.get(i)[0]);
				continue;
			}

			commit[pos]++;
		}
	}

	// crea il file Csv
	public static void makeCsv(int commit[], int len, String extra) throws IOException {

		FileWriter csvWriter = new FileWriter(projectName + extra + ".csv");

		for (int i = 0; i < len; i++) {
			csvWriter.append("" + commit[i]);
			csvWriter.append("\n");
		}

		csvWriter.flush();
		csvWriter.close();

	}

	// funzione principale che si occupa di creare il file csv dei fix nei mesi di
	// lavoro del progetto selezionato (si puo selezionare la versione che usa le
	// date jira oppure quella che usa i commit di git)
	public static void work(String projName, int dateByJira) {
		String extra = "";

		gitCmd.initRepository(projName, repLocation);

		logDate date = new logDate();

		initDate(date);
		takeLastDate(date);

		int len = date.position() + 1;

		int commit[] = new int[len];

		ArrayList<String[]> ticketsInfo = ticketsInfo(); // --> id openDate closeDate Av

		if (dateByJira == 1) {
			jiraAction(commit, ticketsInfo, date);
			extra = "Jira";
		} else {
			takeData(commit, ticketsInfo, date);
			extra = "";
		}

		try {
			makeCsv(commit, len, extra);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return;
	}

	// Richiama la funzione work() con il projectName standard
	public static void work() {
		work(projectName,0);
	}
}

class logDate {
	int year;
	int month;
	int startYear;
	int startMonth;

	public int position() {

		int tempYear = year - startYear;
		return month + (12 * tempYear) - startMonth;
	}
}
