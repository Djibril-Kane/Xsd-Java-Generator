/**
 * Recrée le fichier XML de l'énoncé (3 livres) en utilisant uniquement les
 * classes générées à partir du XSD. La sérialisation passe par XmlNode.save(),
 * hérité automatiquement — aucune classe générée n'a eu besoin d'implémenter
 * quoi que ce soit pour ça.
 */
public class TestMain {
    public static void main(String[] args) throws Exception {

        Bibliotheque biblio = new Bibliotheque();

        for (int i = 1; i <= 3; i++) {
            Livre livre = new Livre();
            livre.setTitre("titre " + i);
            livre.setAuteur("auteur " + i);
            livre.setEditeur("editeur " + i);
            biblio.getLivres().add(livre);
        }

        biblio.save("output/bibliotheque_generee.xml");

        System.out.println("Fichier XML généré : output/bibliotheque_generee.xml");
        System.out.println();
        System.out.println(biblio.toXmlString());
    }
}
