import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Criptoanalise {

    private static final double IC_PORTUGUES = 0.075;

    // Cálculo de IC
    public static double indiceDeCoincidencia(String coluna) {
        int[] freq = new int[26];         
        for (int i = 0; i < coluna.length(); i++) {
            freq[coluna.charAt(i) - 'a']++;
        }

        int n = coluna.length();
        if (n < 2) return 0.0;            

        // Fórmula: IC = Σ [ n_c · (n_c - 1) ] / [ N · (N - 1) ]
        long soma = 0;
        for (int c = 0; c < 26; c++) {
            soma += (long) freq[c] * (freq[c] - 1);
        }
        return (double) soma / ((long) n * (n - 1));
    }

    // Método que separa o texto cifrado em K colunas, calcula o IC de cada uma e devolve a média.
    public static double icMedioParaK(String textoCifrado, int k) {
        StringBuilder[] colunas = new StringBuilder[k];
        for (int i = 0; i < k; i++) colunas[i] = new StringBuilder();

        // Separa os caracteres para as colunas seguindo o tamanho de K(chave)
        for (int pos = 0; pos < textoCifrado.length(); pos++) {
            colunas[pos % k].append(textoCifrado.charAt(pos));
        }

        // Calcula IC de cada coluna e tira a média.
        double soma = 0.0;
        for (int i = 0; i < k; i++) {
            soma += indiceDeCoincidencia(colunas[i].toString());
        }
        return soma / k;
    }


    public static int descobrirTamanhoChave(String textoCifrado, int maxK) {
        // Inicia valores
        int melhorK = 1;
        double menorDistancia = Double.MAX_VALUE;

        for (int k = 1; k <= maxK; k++) {
            // Testa o texto para cada tamanho de chave candidato e calcula o IC médio
            double ic = icMedioParaK(textoCifrado, k);

            // Compara o IC médio com o IC esperado
            double distancia = Math.abs(ic - IC_PORTUGUES);

            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                // Atribui o melhor K encontrado até agora
                melhorK = k;
            }
        }
        return melhorK;
    }

    // Frequências das letras no português
    private static final double[] FREQ_PT = {
        0.1463, // a
        0.0104, // b
        0.0388, // c
        0.0499, // d
        0.1257, // e
        0.0102, // f
        0.0130, // g
        0.0128, // h
        0.0618, // i
        0.0040, // j
        0.0002, // k
        0.0278, // l
        0.0474, // m
        0.0505, // n
        0.1073, // o
        0.0252, // p
        0.0120, // q
        0.0653, // r
        0.0781, // s
        0.0434, // t
        0.0463, // u
        0.0167, // v
        0.0001, // w
        0.0021, // x
        0.0001, // y
        0.0047  // z
    };

    public static double avaliarDeslocamento(String coluna, int deslocamento) {
        int n = coluna.length();

        // Tabela de fraquências para a coluna do K decifrada com o deslocamento testado
        int[] freq = new int[26];

        for (int i = 0; i < n; i++) {
            // Testa cada letra com o deslocamento tentando "decifrar"
            int letra = (coluna.charAt(i) - 'a' - deslocamento + 26) % 26;

            // Salva frequência da letra decifrada
            freq[letra]++;
        }

        double qui = 0.0;
        for (int c = 0; c < 26; c++) {
            // Salva esperado como a frequência esperada X a quantidade de letras daquela coluna
            double esperado = FREQ_PT[c] * n;
            if (esperado > 0) {

                // Fórmula do qui-quadrado
                int observado = freq[c];
                double diff = observado - esperado;
                qui += (diff * diff) / esperado;
            }
        }
        return qui;
    }

    public static char descobrirLetra(String coluna) {
        int melhor = 0;
        double menorQui = Double.MAX_VALUE;
        for (int desloc = 0; desloc < 26; desloc++) {
            // Calcula o qui-quadrado para o deslocamento testado e compara com o melhor encontrado até agora
            // Representa a proximidade da letra testada para decifrar com o português
            double qui = avaliarDeslocamento(coluna, desloc);
            if (qui < menorQui) {
                // Quanto menor o qui, mais próximo do português
                menorQui = qui;
                melhor = desloc;
            }
        }
        return (char) ('a' + melhor);
    }

    public static String descobrirChave(String textoCifrado, int k) {
        StringBuilder[] colunas = new StringBuilder[k];
        for (int i = 0; i < k; i++) colunas[i] = new StringBuilder();

        // Separa os caracteres para as colunas seguindo o tamanho de K(chave)
        for (int pos = 0; pos < textoCifrado.length(); pos++) {
            colunas[pos % k].append(textoCifrado.charAt(pos));
        }

        StringBuilder chave = new StringBuilder();
        for (int i = 0; i < k; i++) {

            //Descobre a letra para cada coluna de K 
            char letra = descobrirLetra(colunas[i].toString());
            chave.append(letra);
        }
        return chave.toString();
    }

    public static String decifrar(String textoCifrado, String chave) {
        StringBuilder out = new StringBuilder(textoCifrado.length());
        for (int i = 0; i < textoCifrado.length(); i++) {
            // Letra do texto cifrado
            int c = textoCifrado.charAt(i) - 'a';

            // Letra da chave correspondente
            int k = chave.charAt(i % chave.length()) - 'a';

            // Fórmula de decifração: P = (C - K + 26) mod 26
            int original = (c - k + 26) % 26;
            out.append((char) ('a' + original));
        }
        return out.toString();
    }

    public static void main(String[] args) throws IOException {
        String cifrado = new String(
            Files.readAllBytes(Paths.get("saida.txt")),
            StandardCharsets.UTF_8
        );

        int k = descobrirTamanhoChave(cifrado, 10);
        System.out.println("\nTamanho da chave estimado: " + k);

        String chave = descobrirChave(cifrado, k);
        System.out.println("Chave recuperada: " + chave);

        String original = decifrar(cifrado, chave);
        Files.write(Paths.get("decifrado.txt"), original.getBytes(StandardCharsets.UTF_8));
    }
}