/**
 * @author domino
 * */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class DictServlet extends HttpServlet {
	private final static int PORT = 2628;
	private final static String HOST = "dict.org";

	private Socket socket;
	private BufferedReader dictReader;
	private PrintWriter dictWriter;
	private HashMap<String, String> dbMap;
	private HashMap<String, String> stratMap;

	public DictServlet() {
		dbMap = new HashMap<>();
		stratMap = new HashMap<>();

		connect();

		String fromServer;
		try {
			fromServer = dictReader.readLine();
			if (!fromServer.startsWith("220")) {

			}

			dictWriter.println("SHOW DB");

			String dbName = "", dbDesc = "";
			while (!(fromServer = dictReader.readLine()).startsWith("250")) {

				if (fromServer.startsWith("110")) // n db present
					continue;
				if (fromServer.startsWith("."))// end of databases
					continue; // continue to get reply code
				if (fromServer.startsWith("554")) // no db present
					break;

				dbName = fromServer.substring(0, fromServer.indexOf("\"") - 1);
				dbDesc = fromServer.substring(fromServer.indexOf("\""))
						.replaceAll("\"", "");

				dbMap.put(dbName, dbDesc);
			}

			dictWriter.println("SHOW STRAT");

			String stratName = "", stratDesc = "";
			while (!(fromServer = dictReader.readLine()).startsWith("250")) {

				if (fromServer.startsWith("111"))
					continue;
				if (fromServer.startsWith("."))
					continue;
				if (fromServer.startsWith("555"))
					break;

				stratName = fromServer.substring(0,
						fromServer.indexOf("\"") - 1);
				stratDesc = fromServer.substring(fromServer.indexOf("\""))
						.replaceAll("\"", "");

				stratMap.put(stratName, stratDesc);
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		disconnect();

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		connect();

		String line = dictReader.readLine();

		if (!line.startsWith("220")) {
			resp.getWriter().println(
					"<html><body><h1>Connection error</h1></body><html>");
			return;
		}

		sendHTMLToServlet(resp);

		disconnect();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		connect();

		String line = dictReader.readLine();

		if (!line.startsWith("220")) {
			resp.getWriter().println(
					"<html><body><h1>Connection error</h1></body><html>");
			return;
		}

		sendHTMLToServlet(resp);

		PrintWriter out = resp.getWriter();
		out.println("</hr></br><h2>Search result:</h2>");// ?

		String dbName = req.getParameter("dbOption");
		String stratName = req.getParameter("stratOption");
		String wordToTranslate = req.getParameter("wordToTranslate");

		if (stratName.equals("*"))
			dictWriter.println("DEFINE " + dbName + " " + wordToTranslate);
		else
			dictWriter.println("MATCH " + dbName + " " + stratName + " "
					+ wordToTranslate);

		out.println("<!DOCTYPE html><body>");

		while (!(line = dictReader.readLine()).startsWith("250")) {

			if (line.startsWith("150"))
				continue;
			if (line.startsWith("."))
				continue;
			if (line.startsWith("550"))
				break;
			if (line.startsWith("552")) {
				out.println("<h1>No match</h1>");
				break;
			}

			out.println(line + "</br>");

		}

		out.println("</body></html>");

		disconnect();
	}

	private void connect() {
		try {
			socket = new Socket(HOST, PORT);
			dictReader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			dictWriter = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void disconnect() {
		if (dictWriter != null)
			dictWriter.close();
		try {
			if (dictReader != null)
				dictReader.close();
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendHTMLToServlet(HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();

		out.println("<!DOCTYPE html><body>");
		out.println("<h1 style=\"text-align:center;\">DICT protocol servlet</h1></br>");

		out.println("<form action=\"http://localhost:8080/dict/\" method=POST>");
		out.println("<input style=\"width:30%;\" type=\"text\" name=\"wordToTranslate\">");
		out.println("</br>");

		sendDBOptionHtmlToServlet(resp);
		out.println("</br>");

		sendStratOptionHtmlToServlet(resp);
		out.println("</br>");

		out.println("<input type=\"submit\" value=\"Search\">");

		out.println("</form>");
		out.println("</body></html>");
	}

	private void sendDBOptionHtmlToServlet(HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();

		out.println("<select name=\"dbOption\" style=\"width:30%;\">");
		out.println("<option value=\"*\">All databases</option>");

		for (String key : dbMap.keySet()) {
			out.println("<option value=" + key + ">" + dbMap.get(key)
					+ "</option>");
		}
		out.println("</select>");
	}

	private void sendStratOptionHtmlToServlet(HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();

		out.println("<select name=\"stratOption\" style=\"width:30%;\">");
		out.println("<option value=\"*\">Return definitions</option>");

		for (String key : stratMap.keySet()) {
			out.println("<option value=" + key + ">" + stratMap.get(key)
					+ "</option>");
		}

		out.println("</select>");
	}
}
