public class ParserValidator {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java ParserDemo <fichier.xsd>");
            return;
        }
        String xsdPath = args[0];

        // On appelle directement les méthodes de parsing de XsdToJavaGenerator,
        // sans jamais appeler writeClass() : la génération de code n'est pas
        // sollicitée ici, seul le parsing est testé.
        XsdToJavaGenerator.parse(xsdPath);
        XsdToJavaGenerator.resolveRoot();

        System.out.println("=== Résultat du parsing de " + xsdPath + " ===\n");

        System.out.println("Éléments de premier niveau détectés : " + XsdToJavaGenerator.registry.size());
        System.out.println("Élément racine détecté : " + XsdToJavaGenerator.rootName + "\n");

        for (XsdToJavaGenerator.ElementDef def : XsdToJavaGenerator.registry.values()) {
            System.out.println("- " + def.name
                + (def.complex ? "  [complexe]" : "  [simple -> " + def.simpleJavaType + "]")
                + (def.name.equals(XsdToJavaGenerator.rootName) ? "  (racine)" : ""));

            if (def.complex) {
                for (XsdToJavaGenerator.Child c : def.children) {
                    String cardinalite = c.unbounded ? "liste (maxOccurs=unbounded)" : "unique";
                    System.out.println("    ref -> " + c.target.name + "  [" + cardinalite + "]");
                }
            }
        }

        System.out.println("\n=== Vérifications automatiques ===");
        boolean ok = true;

        if (XsdToJavaGenerator.rootName == null) {
            System.out.println("[ECHEC] Aucun élément racine détecté.");
            ok = false;
        } else {
            System.out.println("[OK] Élément racine détecté : " + XsdToJavaGenerator.rootName);
        }

        for (XsdToJavaGenerator.ElementDef def : XsdToJavaGenerator.registry.values()) {
            if (def.complex) {
                for (XsdToJavaGenerator.Child c : def.children) {
                    if (c.target == null) {
                        System.out.println("[ECHEC] Référence non résolue dans " + def.name);
                        ok = false;
                    }
                }
            }
        }
        if (ok) {
            System.out.println("[OK] Toutes les références (ref=\"...\") sont correctement résolues en objets.");
        }

        System.out.println(ok ? "\nParser validé avec succès sur ce schéma." : "\nDes erreurs ont été détectées ci-dessus.");
    }
}