package T1_Seguranca;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static final String KEY = "a";

    public static char removeSpecialChar(char text) {
        text = text.replaceAll("[^a-zA-Z0-9]", "");
        return text.charAt(0);
    }

    public static char criptografar(char textoChar, char chaveChar) {
        textoChar = Character.toLowerCase(textoChar);
        chaveChar = Character.toLowerCase(chaveChar);

        int posTexto = textoChar - 'a' + 1;
        int posChave = chaveChar - 'a' + 1;

        int novaPos = (posTexto + posChave) % 26;

        return (char) ('a' + novaPos);
}

    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);

        String arquivoEntrada = "entrada.txt";
        String arquivoSaida = "saida.txt";

        try (
            BufferedReader reader = new BufferedReader(new FileReader(arquivoEntrada));
            BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoSaida))
        ) {
            int character;
            int i = 0;

            while ((character = reader.read()) != -1) {
                char caractere = Character.toLowerCase((char) character);

                char k = KEY.charAt(i % KEY.length());
                char criptografado = criptografar(caractere, k);

                writer.write(criptografado);
                i++;    
            }
        } catch (IOException e) {
            System.out.println("erro");
        }
        sc.close();
    }
}