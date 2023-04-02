import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class BuscadorDeLinks implements Runnable {

    private static final String LINKS = "<a\\s+[^>]*href=\"([^\"]*)\"[^>]*>";
    private static Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(new String[]{"A", "O", "E", "AS", "OS", "UM", "UMA", "UNS", "UMAS", "COM", "ESTA", "FOI", "ESTAR"}));
    private String url;
    private String urlOrigem;
    private static Set<String> urlsConsumidas = new HashSet<>();
    private Map<String, Set<String>> indice;
    private static int counter = 0;
    private static PrintStream arquivoLog;
    private ExecutorService exService;

    private int qtd_link;
    private static final int QTD_MAX_LINKS = 7;

    public BuscadorDeLinks(String url, ExecutorService exService, Map<String, Set<String>> indice, int qtd_link, PrintStream arquivoLog) {
        this.url = url;
        this.urlOrigem = url;
        this.exService = exService;
        this.indice = indice;
        this.qtd_link = qtd_link;
        this.arquivoLog = arquivoLog;
    }

    @Override
    public void run() {
    	processamentoUrl(url, qtd_link);
    }

    private synchronized void processamentoUrl(String url, int qtd_link) {
        try {
            if (qtd_link > QTD_MAX_LINKS || urlsConsumidas.contains(url)) {
                return;
            }
            
            var sites = searchSites();
            
            if (sites.getResponseCode() == 200) {
            	arquivoLog.println("URL " + counter + ": " + url);
                var html = adquirirHTML(sites);
                var links = obterLinks(url);
                indexarPalavra(links, url, indice);
                pesquisarLinks(html);
                counter++;
            }
        } catch (Exception e) {}
    }
    
    private static String normalize(String palavra) {
        return Normalizer.normalize(palavra, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").toUpperCase();
    }

    private static void indexarPalavra(String texto, String url, Map<String, Set<String>> indice)  {
    	arquivoLog.println("Indexando ...");
        String[] palavras = texto.split(" ");

        Arrays.stream(palavras)
                .map((p) -> normalize(p))
                .filter((p) -> !isStopWord(p))
                .forEach((palavra) -> {
                    Set<String> indicePorPalavra = null;

                    if (indice.containsKey(palavra)) {
                        indicePorPalavra = indice.get(palavra);
                    } else {
                        indicePorPalavra = new HashSet<>();
                        indice.put(palavra, indicePorPalavra);
                    }

                    indicePorPalavra.add(url);
                });
        arquivoLog.println("Pronto!");
    }
    
    public static boolean isStopWord(String palavra) {
		if (palavra.length() <= 2) {
			return true;
		}
		
		return STOP_WORDS.contains(palavra);
	}

    private synchronized String obterLinks(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        String links = doc.text();
        return links;
    }

    private synchronized HttpURLConnection searchSites() throws IOException {
    	urlsConsumidas.add(url);
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("GET");
        return conn;
    }

    private synchronized String adquirirHTML(HttpURLConnection conn) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
        	stringBuilder.append(line);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }
    
    private synchronized void pesquisarLinks(String html) {
        Pattern p = Pattern.compile(LINKS);
        Matcher m = p.matcher(html);
        while (m.find()) {
            String link = m.group(1);
            if (link.startsWith("/") || link.startsWith(urlOrigem)) {
                if (link.startsWith("/")) {
                    link = urlOrigem + link;
                }
            }
            exService.submit(new Thread(new BuscadorDeLinks(link, exService, indice, qtd_link + 1, arquivoLog)));
        }
    }
    
}