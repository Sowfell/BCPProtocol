package blockchain;
public class AppTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BlockChain chain = buildChain(3, "Original");
        check("T1 cadeia válida", true, BlockChain.isChainValid(chain.getBlockChain()));

        // --- T2: adulterar o bloco do meio ---
        Block meio = chain.getBlockChain().get(1);
        String dadoOriginal = meio.data;
        meio.data = "ADULTERADO";
        check("T2 bloco do meio adulterado", false, BlockChain.isChainValid(chain.getBlockChain()));

        // restaurando o dado, a cadeia volta a ser válida
        meio.data = dadoOriginal;
        check("T2b dado restaurado", true, BlockChain.isChainValid(chain.getBlockChain()));

        // --- T3: o bug do gênese (cadeia de 1 bloco adulterado) ---
        BlockChain solo = buildChain(1, "Solo");
        solo.getBlockChain().get(0).data = "ADULTERADO";
        check("T3 gênese adulterado", false, BlockChain.isChainValid(solo.getBlockChain()));

        // --- T4: null e vazia ---
        check("T4 cadeia null", false, BlockChain.isChainValid(null));
        check("T4b cadeia vazia", false, BlockChain.isChainValid(new java.util.ArrayList<>()));

        // --- T5: addToChain recusa bloco com previousHash errado ---
        int tamanhoAntes = chain.getBlockChain().size();
        chain.addToChain(new Block("Intruso", "abc"));
        check("T5 bloco com previousHash errado", tamanhoAntes, chain.getBlockChain().size());

        // --- T6: addToChain recusa gênese que não aponta pra "0" ---
        BlockChain vazia = new BlockChain();
        vazia.addToChain(new Block("Genesis falso", "abc"));
        check("T6 gênese com previousHash errado", 0, vazia.getBlockChain().size());

        // --- T7: replaceChain, cadeia menor ---
        check("T7 cadeia menor", ReplaceMessage.DONTCHANGE,
                chain.replaceChain(buildChain(2, "Menor").getBlockChain()));

        // --- T8: replaceChain, cadeia de mesmo tamanho ---
        check("T8 cadeia igual", ReplaceMessage.DONTCHANGE,
                chain.replaceChain(buildChain(3, "Igual").getBlockChain()));

        // --- T9: replaceChain, cadeia maior e válida ---
        check("T9 cadeia maior válida", ReplaceMessage.CHAINSWITCH,
                chain.replaceChain(buildChain(4, "Maior").getBlockChain()));
        check("T9b tamanho após troca", 4, chain.getBlockChain().size());

        // --- T10: replaceChain, cadeia maior mas adulterada ---
        BlockChain ruim = buildChain(6, "Ruim");
        ruim.getBlockChain().get(2).data = "ADULTERADO";
        check("T10 cadeia maior inválida", ReplaceMessage.INVALIDCHAIN,
                chain.replaceChain(ruim.getBlockChain()));
        check("T10b tamanho continua 4", 4, chain.getBlockChain().size());

        // --- T11: motivos do REJECT ---
        check("T11 reason INVALIDCHAIN", "Cadeia inválida",
                BlockChain.rejectReason(ReplaceMessage.INVALIDCHAIN));
        check("T11b reason DONTCHANGE", "Tamanho menor que a cadeia local",
                BlockChain.rejectReason(ReplaceMessage.DONTCHANGE));
        check("T11c reason CHAINSWITCH", null,
                BlockChain.rejectReason(ReplaceMessage.CHAINSWITCH));
    }

    // monta uma cadeia válida com 'size' blocos
    private static BlockChain buildChain(int size, String prefixo) {
        BlockChain c = new BlockChain();
        String prev = "0";
        for (int i = 1; i <= size; i++) {
            c.addToChain(new Block(prefixo + " bloco " + i, prev));
            prev = c.getLastBlock().hash;
        }
        return c;
    }

    // compara esperado x obtido e imprime o resultado
    private static void check(String nome, Object esperado, Object obtido) {
        boolean ok = (esperado == null) ? obtido == null : esperado.equals(obtido);
        System.out.println("\n[" + (ok ? "OK" : "FALHOU") + "] " + nome
                + " -> esperado: " + esperado + ", obtido: " + obtido + "\n");
 
	}
}
