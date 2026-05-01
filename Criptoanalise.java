package T1_Seguranca;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Criptoanalise {

    // IC esperado para o português. Se o IC médio de um candidato K
    // estiver perto disso, é forte indício de que K é o tamanho da chave.
    private static final double IC_PORTUGUES = 0.072;

    // -----------------------------------------------------------------
    // 1) IC de um único trecho de texto
    // -----------------------------------------------------------------
    // Recebe um conjunto de caracteres (uma "coluna") e devolve seu IC.
    // Pré-condição: o texto já está higienizado (só a-z).
    public static double indiceDeCoincidencia(String coluna) {
        int[] freq = new int[26];          // contador para cada letra a-z
        for (int i = 0; i < coluna.length(); i++) {
            freq[coluna.charAt(i) - 'a']++;
        }

        int n = coluna.length();
        if (n < 2) return 0.0;             // evita divisão por zero

        // Fórmula: IC = Σ [ n_c · (n_c - 1) ] / [ N · (N - 1) ]
        long soma = 0;
        for (int c = 0; c < 26; c++) {
            soma += (long) freq[c] * (freq[c] - 1);
        }
        return (double) soma / ((long) n * (n - 1));
    }

    // -----------------------------------------------------------------
    // 2) IC médio para um candidato de tamanho de chave K
    // -----------------------------------------------------------------
    // Particiona o texto em K colunas pela regra "posição mod K = i"
    // e devolve a média dos K ICs.
    public static double icMedioParaK(String textoCifrado, int k) {
        StringBuilder[] colunas = new StringBuilder[k];
        for (int i = 0; i < k; i++) colunas[i] = new StringBuilder();

        // Distribui cada caractere na sua coluna correspondente.
        // Posição 0 -> coluna 0, posição 1 -> coluna 1, ...,
        // posição k -> coluna 0 (o ciclo recomeça).
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

    // -----------------------------------------------------------------
    // 3) Varre K = 1..maxK e escolhe o mais provável
    // -----------------------------------------------------------------
    public static int descobrirTamanhoChave(String textoCifrado, int maxK) {
        int melhorK = 1;
        double menorDistancia = Double.MAX_VALUE;

        System.out.println("  K  |  IC médio  |  distância p/ 0,072");
        System.out.println("-----+------------+--------------------");

        for (int k = 1; k <= maxK; k++) {
            double ic = icMedioParaK(textoCifrado, k);
            double distancia = Math.abs(ic - IC_PORTUGUES);

            System.out.printf(" %2d  |   %.4f   |       %.4f%n", k, ic, distancia);

            // Critério: o K cujo IC fica mais próximo de 0,072.
            // (Múltiplos do K real também dão IC bom, então mantemos
            //  o menor empate, varrendo de 1 para cima.)
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhorK = k;
            }
        }
        return melhorK;
    }

// Frequências relativas das letras no português europeu/brasileiro
// (texto higienizado, sem acentos). Os valores somam ~1,0.
// Fonte: contagens em corpora padrão; pequenas variações são normais
// entre fontes diferentes, mas a forma geral do histograma é estável.
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

// -----------------------------------------------------------------
// 1) Avalia um deslocamento candidato para uma coluna
// -----------------------------------------------------------------
// "Decifra" a coluna usando o deslocamento, conta as letras
// resultantes, e mede o quão próximo o histograma fica do português
// usando o teste qui-quadrado. Quanto MENOR o chi, melhor o encaixe.
public static double avaliarDeslocamento(String coluna, int deslocamento) {
    int n = coluna.length();
    int[] freq = new int[26];

    for (int i = 0; i < n; i++) {
        // Inverso da cifra de César: subtrai o deslocamento.
        // O "+ 26" antes do "% 26" garante resultado não-negativo.
        int letra = (coluna.charAt(i) - 'a' - deslocamento + 26) % 26;
        freq[letra]++;
    }

    // Chi-quadrado: Σ (observado - esperado)² / esperado
    // - "esperado" é a frequência esperada multiplicada pelo tamanho
    //   da amostra (transforma proporção em contagem)
    // - se a coluna decifrada tiver perfil de português, observado ≈
    //   esperado e o chi tende a zero
    double chi = 0.0;
    for (int c = 0; c < 26; c++) {
        double esperado = FREQ_PT[c] * n;
        if (esperado > 0) {
            double diff = freq[c] - esperado;
            chi += (diff * diff) / esperado;
        }
    }
    return chi;
}

// -----------------------------------------------------------------
// 2) Descobre o melhor deslocamento de UMA coluna
// -----------------------------------------------------------------
// Testa todos os 26 deslocamentos possíveis e devolve o que produziu
// o menor chi-quadrado. Esse valor (0..25) é a letra da chave naquela
// posição, codificada como 0='a', 1='b', ..., 25='z'.
public static int descobrirDeslocamento(String coluna) {
    int melhor = 0;
    double menorChi = Double.MAX_VALUE;
    for (int desloc = 0; desloc < 26; desloc++) {
        double chi = avaliarDeslocamento(coluna, desloc);
        if (chi < menorChi) {
            menorChi = chi;
            melhor = desloc;
        }
    }
    return melhor;
}

// -----------------------------------------------------------------
// 3) Descobre a chave inteira
// -----------------------------------------------------------------
// Particiona o texto em K colunas (igual à Etapa 1) e roda a análise
// de frequência em cada uma. Cada coluna contribui com uma letra da
// chave.
public static String descobrirChave(String textoCifrado, int k) {
    StringBuilder[] colunas = new StringBuilder[k];
    for (int i = 0; i < k; i++) colunas[i] = new StringBuilder();
    for (int pos = 0; pos < textoCifrado.length(); pos++) {
        colunas[pos % k].append(textoCifrado.charAt(pos));
    }

    StringBuilder chave = new StringBuilder();
    for (int i = 0; i < k; i++) {
        int desloc = descobrirDeslocamento(colunas[i].toString());
        chave.append((char) ('a' + desloc));
    }
    return chave.toString();
}

// -----------------------------------------------------------------
// 4) Decifra o texto inteiro com a chave descoberta
// -----------------------------------------------------------------
// Vigenère reverso: cada caractere é deslocado de volta pela letra
// correspondente da chave.
public static String decifrar(String textoCifrado, String chave) {
    StringBuilder out = new StringBuilder(textoCifrado.length());
    for (int i = 0; i < textoCifrado.length(); i++) {
        int c = textoCifrado.charAt(i) - 'a';
        int k = chave.charAt(i % chave.length()) - 'a';
        int original = (c - k + 26) % 26;
        out.append((char) ('a' + original));
    }
    return out.toString();
}

// -----------------------------------------------------------------
// 5) Pipeline completo
// -----------------------------------------------------------------
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
    Files.write(Paths.get("decifrado.txt"),
                original.getBytes(StandardCharsets.UTF_8));

    System.out.println("\nPrimeiros 200 caracteres do texto decifrado:");
    System.out.println(original.substring(0, Math.min(200, original.length())));
}
}