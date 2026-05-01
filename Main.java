package T1_Seguranca;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public class Main {
    public static final String KEY = "a";

    public static String higienizar(char c) {
        String s = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        s = s.toLowerCase();
        s = s.replaceAll("[^a-z]", "");
        return s;
    }

    public static char criptografar(char textoChar, char chaveChar) {
        int posTexto = textoChar - 'a';
        int posChave = chaveChar - 'a';
        int novaPos = (posTexto + posChave) % 26;
        return (char) ('a' + novaPos);
    }

    public static void main(String args[]) {
        String arquivoEntrada = "DomCasmurro.txt";
        String arquivoSaida = "saida.txt";

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(arquivoEntrada), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(arquivoSaida), StandardCharsets.UTF_8))
        ) {
            int character;
            int i = 0;

            while ((character = reader.read()) != -1) {
                String limpo = higienizar((char) character);

                for (int j = 0; j < limpo.length(); j++) {
                    char ch = limpo.charAt(j);
                    char posicaoChave = KEY.charAt(i % KEY.length());
                    writer.write(criptografar(ch, posicaoChave));
                    i++;    
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
