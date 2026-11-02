package blockchain;

public enum ReplaceMessage {
	INVALIDCHAIN,
	DONTCHANGE,		//Cadeia válida, mas menor que atual
	CHAINSWITCH		//Cadeia valida e troca feita
}
