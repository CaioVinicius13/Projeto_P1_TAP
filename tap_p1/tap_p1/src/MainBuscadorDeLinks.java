import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainBuscadorDeLinks{

    public static Scanner teclado = new Scanner(System.in);
    public static PrintStream entrada;
    public static PrintStream arquivoLog;

    public static void main(String[] args) throws FileNotFoundException {
    	arquivoLog = new PrintStream(new FileOutputStream("arquivo-log.txt"), true);
        entrada = System.out;
        Map<String, Set<String>> indice = new HashMap<>();
        ExecutorService exService = Executors.newFixedThreadPool(10);
        exService.submit(new Thread(new BuscadorDeLinks("https://pt.wikipedia.org/wiki/Campina_Grande", exService, indice, 0,arquivoLog)));

        while (true) {
            var pesquisa = getString("O que deseja pesquisar? (ou SAIR para sair): ");
           
            if (pesquisa.equals("SAIR")) {
            	System.out.println("Pesquisa encerrada!");
            	break;
            }

            if (indice.containsKey(pesquisa)) {
                indice.get(pesquisa)
                        .forEach(x -> entrada.println(x));
            } else {
                System.out.println("Não tenho essa informação");
            }
        }
    }

    public static String getString(String pesquisa) {
        entrada.println(pesquisa);
        return teclado.nextLine().toUpperCase();
    }
}
