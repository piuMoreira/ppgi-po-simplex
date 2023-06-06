package src;

import Jama.*;


public class Simplex {

    static final int MIN = 0;
    static final int MAX = 1;
    static final int M = 1000000;
    static final double EPSILON = 0.00000001;

    private static double[][] restricoes = { {3, 6, 0, 4, 100}, {1, 0, 0, 10, 50}, {3, 1, 6, 0, 30}};
    private static int[] sinais = {1,3,3};  // 1: <=     2: =     3: >=
    private static double[] fucaoObjetivo = {2, 10, 1, 4} ;
    private static double[][] tableau;

    private static int[] base;
    private static int[] variaveis;

    public static void main(String[] args) {

        printaPrimal(MIN);
        System.out.println("\n################################################################\n");
        printaDual(MIN);
        System.out.println("\n################################################################\n");
        montaTableau(fucaoObjetivo.length,restricoes.length, MIN);
        System.out.println("\n################################################################\n");
        calculaSimplex();
        printaResposta(tableau.length, fucaoObjetivo.length);
        System.out.println("\n################################################################\n");
        printaSolucaoDual();
        System.out.println("\n################################################################\n");
        calculaRange();

    }


    public static void montaTableau(int numVariaveis, int numRestricoes, int objetivo) {

        // O número de linhas do tableau é igual ao número de restricoes mais um (funcao obejtivo)
        int numLinhas = numRestricoes + 1;
        // Número de variáveis de decisão + variáveis de folga + resultado das restrições
        int numColunas = numVariaveis + numRestricoes + 1;


        for(int k=0; k<sinais.length; k++) {
            if(sinais[k] == 3) {
                numColunas++;
            }
        }
        // Inicializa tableau com zeros
        tableau = new double[numLinhas][numColunas];
        for(int i=0; i<numLinhas; i++) {
            for(int j=0; j<numColunas; j++) {
                tableau[i][j] = 0;
            }
        }
        // A Primeira linha do tableau tem os valores da funcao objetivo
        for(int i=0; i<numVariaveis; i++) {
            if(objetivo == MAX){
                tableau[0][i] = fucaoObjetivo[i] * -1;
            } else if (objetivo == MIN) {
                tableau[0][i] = fucaoObjetivo[i];
            }
        }
        // A ultima coluna do tableau tem os valores do resultado das restrições
        for(int i=0; i<numRestricoes; i++) {
            tableau[i+1][numColunas-1] = restricoes[i][restricoes[0].length -1];
        }
        // Coloca o valor das restrições
        for(int i=0; i<numRestricoes; i++) {
            for(int j=0; j<numVariaveis; j++) {
                tableau[i+1][j] = restricoes[i][j];
            }
        }

        // Array auxiliar para acompanhar as variáveis que estão na base
        base = new int[restricoes.length];

        // Array auxiliar para guardar o indice das variáveis a se considerar no dual
        variaveis = new int[restricoes.length];

        // Inicializa array auxilixar das variáveis na base
        for(int i=0; i < restricoes.length; i++) {
            base[i] = fucaoObjetivo.length + i + 1;
        }

        // Coloca valores nas variaveis de folga e/ou artificiais
        int count = 2;
        int count2 = 0;
        for(int i=1; i<numLinhas; i++) {
            for(int j=numVariaveis; j<numColunas-1; j++) {
                if(j-i == numVariaveis-1 && sinais[i-1] == 1) {
                    tableau[i][j] = 1;
                    variaveis[count2] = j;
                    count2++;
                } else if (j-i == numVariaveis-1 && sinais[i-1] == 2) {
                    tableau[i][j] = 1;
                    tableau[0][j] = 1*M;
                    variaveis[count2] = j;
                    count2++;
                } else if(j-i == numVariaveis-1 && sinais[i-1] == 3) {
                    tableau[i][j] = -1;
                    tableau[0][tableau[0].length-count] = 1*M;
                    tableau[i][tableau[0].length-count] = 1;
                    variaveis[count2] = tableau[0].length-count;
                    count++;
                    count2++;
                }
            }
        }

        // Remove variaveis de folga e artificiais da equação inicial
        for(int k=0; k<sinais.length; k++) {
            if(sinais[k] != 1) {
                for(int j=0; j<tableau[0].length; j++){
                    tableau[0][j] = tableau[0][j] - (M * tableau[k+1][j]);
                }                
            }
        }


        System.out.println("Esse é o Tableau:\n");
        printaTableau(numLinhas, numColunas);
    }

    public static void printaTableau(int linha, int coluna) {
        for(int i=0; i < linha; i++) {
            if (i==0) {
                System.out.print("Z   ");
            } else {
                System.out.print("X"+base[i-1]+"  ");
            }
            for (int j=0; j < coluna; j++) {
                System.out.printf("|  %.2f  ", tableau[i][j]);
            }
            System.out.println("|");
        }
    }

    public static void calculaSimplex() {

        int count = 1;

        while (!ehOtimo()) {

            System.out.println("\nIteração " + count + "\n");

            // Determina elemento pivo
            int colunaPivo = achaColunaPivo();
            int linhaPivo = achaLinhaPivo(colunaPivo);

            // Atualiza Tableau
            atualizaTableau(linhaPivo, colunaPivo);
            printaTableau(tableau.length, tableau[0].length);
            count++;
            System.out.println("\n################################################################\n");

        }
    }

    public static boolean ehOtimo() {
        // A solução é otima se e somente se casa coeficiente da equação for não negativo
        for(int i=0; i<tableau[0].length-1; i++) {
            if(tableau[0][i] < -EPSILON) {
                return false;
            }
        }
        return true;
    }

    public static int achaColunaPivo() {
        int colunaPivo = -1;
        double aux = Double.POSITIVE_INFINITY;
        // Seleciona elemento com maior valor absoluto
        for(int i=0; i<tableau[0].length-1;i++) {
            if(tableau[0][i] < aux) {
                colunaPivo = i;
                aux = tableau[0][i];
            }
        }
        return colunaPivo;
    }

    public static int achaLinhaPivo(int colunaPivo) {
        int linhaPivo = -1;
        double[] aux = new double[tableau.length-1];
        double aux2 = Double.POSITIVE_INFINITY;
        // Calcula a razão mínima das linhas
        for(int i=1; i<tableau.length; i++) {
            if(tableau[i][tableau[0].length-1] > -EPSILON && tableau[i][colunaPivo] > -EPSILON) {
                aux[i-1] = tableau[i][tableau[0].length-1] / tableau[i][colunaPivo];
            } else {
                aux[i-1] = -1;
            }
        }

        // Seleciona aquele que apresenta menor valor no teste da razão miníma
        for(int i=0; i<aux.length; i++) {
            if(aux[i] < aux2 && aux[i] > -EPSILON) {
                linhaPivo = i+1;
                aux2 = aux[i];
            }
        }
        return linhaPivo;
    }

    public static void atualizaTableau(int linhaPivo, int colunaPivo) {
        double elementoPivo = tableau[linhaPivo][colunaPivo];

        // Divide linha pivô pelo elemento pivô
        for(int i=0; i<tableau[0].length; i++) {
            tableau[linhaPivo][i] /= elementoPivo;
        }

        // Zera a coluna pivô a exceção do elemento pivô
        for (int i=0; i<tableau.length; i++){
            if(i != linhaPivo && tableau[i][colunaPivo] != 0) {
                double elementoAlvo = tableau[i][colunaPivo];
                for(int j=0; j<tableau[0].length; j++){
                    tableau[i][j] = tableau[i][j] - (elementoAlvo * tableau[linhaPivo][j]);
                }
            }
        }

        // Atualiza variáveis na base
        base[linhaPivo-1] = colunaPivo+1;
    }

    public static void printaResposta(int numLinhas, int numColunas) {

        System.out.println("Solução Primal: \n");

        int posicao = -1;
        int count = 0;

        System.out.printf("Z = %.3f\n", tableau[0][tableau[0].length-1]);

        for(int i=0; i < numColunas; i++) {
            for(int j=0; j< numLinhas; j++) {
                if(tableau[j][i] == 1){
                    posicao = j;
                } else if (tableau[j][i] == 0) {
                    count ++;
                }
            }
            if(posicao > -1 && count == numLinhas-1) {
                System.out.printf("X%d = %.3f\n", i + 1, tableau[posicao][tableau[0].length - 1]);
            } else {
                System.out.printf("X%d = 0\n", i + 1);
            }
            posicao = -1;
            count = 0;
        }
    }

    public static void printaSolucaoDual() {
        System.out.println("Solução Dual:\n");
        int aux=1;
        // Pega Solução Dual a partir do Tableau Final do Primal
        for(int i= 0; i<restricoes.length; i++) {
            if(tableau[0][variaveis[i]] >= M-50) {
                System.out.printf("Y%d = %.2f\n", aux, tableau[0][variaveis[i]] - M);
            } else {
                System.out.printf("Y%d = %.2f\n", aux, tableau[0][variaveis[i]]);
            }
            
            aux++;
        }
    }

    public static void calculaRange() {
        System.out.println("Range: \n");

        double[][] s = new double[restricoes.length][restricoes.length];
        double[][] b = new double[restricoes.length][1];

        for(int i=1; i<tableau.length; i++) {
            for(int j= 0; j<restricoes.length; j++) {
                s[i-1][j] = tableau[i][variaveis[j]];
            }
        }

        for(int i=1; i<tableau.length; i++) {
            b[i-1][0] = tableau[i][tableau[0].length-1];
        }

        for(int a=0; a<restricoes.length;a++) {
            double[][] delta = new double[restricoes.length][1];

            for(int c=0;c<restricoes.length;c++) {
                if(c==a) {
                    delta[c][0] = 1;
                } else {
                    delta[c][0] = 0;
                }
            }

            Matrix deltaX = new Matrix(delta);
            Matrix sEstrela = new Matrix(s);
            Matrix bEstrela = new Matrix(b);
            double limiteInferior = Double.NEGATIVE_INFINITY;
            double limiteSuperior = Double.POSITIVE_INFINITY;

            Matrix aux = sEstrela.times(deltaX);
            Matrix aux2 = bEstrela.arrayRightDivide(aux.times(-1));

            for(int i=0; i<aux2.getRowDimension(); i++) {
                for(int j=0; j<aux2.getColumnDimension(); j++) {
                    // System.out.print(aux2.get(i, j)+"   ");
                    if(aux2.get(i, j) <= -EPSILON && aux2.get(i, j) > limiteInferior) {
                        limiteInferior = aux2.get(i, j);
                    } else if(aux2.get(i, j) > EPSILON && aux2.get(i, j) < limiteSuperior) {
                        limiteSuperior = aux2.get(i, j);
                    }
                }
            }

            System.out.printf("%.3f <= b%d <=  %.3f\n", limiteInferior, a+1, limiteSuperior);

        }
    }

    public static void printaPrimal(int objetivo) {

        System.out.println("Primal:\n");

        if(objetivo == MAX) {
            System.out.print("Max Z = ");
        } else if(objetivo == MIN) {
            System.out.print("Min Z = ");
        }

        for(int i=0; i < fucaoObjetivo.length; i++) {
            System.out.printf("%.2fX%d + ", fucaoObjetivo[i], i + 1);
        }

        System.out.println("\nS.A.:");

        for(int i=0; i < restricoes.length; i++) {
            for(int j=0; j < restricoes[0].length-1; j++){
                System.out.printf("%.2fX%d + ", restricoes[i][j], j + 1);
            }
            if(sinais[i] == 1) {
                System.out.printf("<= %.2f", restricoes[i][restricoes[0].length-1]);
            } else if(sinais[i] == 2) {
                System.out.printf("= %.2f", restricoes[i][restricoes[0].length-1]);
            } else if(sinais[i] == 3) {
                System.out.printf(">= %.2f", restricoes[i][restricoes[0].length-1]);
            }
            System.out.println();
        }

        for(int i=0; i<fucaoObjetivo.length; i++) {
            System.out.printf("X%d, ", i+1);
        }
        System.out.println(">= 0");

    }

    public static void printaDual(int objetivo) {

        System.out.println("Dual:\n");

        if(objetivo == MAX) {
            System.out.print("Min W = ");
        } else if(objetivo == MIN) {
            System.out.print("Max W = ");
        }
        
        for(int i=0; i < restricoes.length; i++) {
            System.out.printf("%.2fY%d + ", restricoes[i][restricoes[0].length-1], i + 1);
        }

        System.out.println("\nS.A.:");

        for(int i=0; i < fucaoObjetivo.length; i++) {
            for(int j=0; j < restricoes.length; j++) {
                System.out.printf("%.2fY%d + ", restricoes[j][i], j + 1);
            }
            System.out.printf(">= %.2f\n", fucaoObjetivo[i]);
        }

        for(int i=0; i<restricoes.length; i++) {
            System.out.printf("Y%d ", i+1);
            if(sinais[i] == 1) {
                System.out.println(">= 0");
            } else if( sinais[i] == 2) {
                System.out.println("livre");
            } else if(sinais[i] == 3) {
                System.out.println("<= 0");
            }
        }
    }
    
}
