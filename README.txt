PROJET : Générateur de code Java à partir de schémas XML (XSD)
=====================================================================

Ce générateur lit un schéma XSD (xs:element / xs:complexType / xs:sequence)
et produit une classe Java par élément complexe. La sérialisation n'est pas
dupliquée dans chaque classe générée : elle est centralisée dans XmlNode,
dont héritent toutes les classes produites, et qui utilise la réflexion
Java (java.lang.reflect.Field) pour parcourir les champs de l'objet et
reconstruire le XML.

CONTENU
-------
- bibliotheque.xsd                   : schéma XSD d'exemple fourni dans l'énoncé
- docs/regles_correspondance.md      : règles de correspondance XSD -> Java (document autonome)
- src/XmlNode.java                   : classe mère fournissant save()/toXmlString() par réflexion
- src/XsdToJavaGenerator.java        : le générateur (parsing XSD + génération des classes)
- src/ParserValidator.java           : validation autonome du parser : vérifie que la racine
                                        est détectée et que toutes les ref="..." sont résolues,
                                        sans passer par la génération/compilation de code
- generated/TestMain.java            : programme de test qui recrée le XML de l'énoncé à partir
                                        des classes générées (seul fichier de generated/ suivi par git)
- generated/*.java (générés)         : classes produites par le générateur à partir du XSD
                                        (Bibliotheque.java, Livre.java) — non versionnées, voir .gitignore
- output/                            : XML produit par TestMain à l'exécution — non versionné
- run.sh                             : rejoue tout le pipeline (voir ci-dessous)
- clean.sh                           : supprime les .class et les fichiers générés/output
- .gitignore                         : exclut du dépôt tout ce qui est généré ou compilé

COMMENT REJOUER TOUT LE PROJET
-------------------------------
Le plus simple :
   ./run.sh

Ce script fait, dans l'ordre :
1) Compile le générateur, XmlNode et ParserValidator.
2) Lance ParserValidator sur bibliotheque.xsd : affiche les éléments détectés,
   la racine, et vérifie que toutes les références sont résolues — sans
   générer ni compiler aucune classe métier.
3) Lance le pipeline complet :
   - génère les classes Java dans generated/ à partir du XSD
   - compile ces classes générées + TestMain
   - exécute TestMain, qui produit output/bibliotheque_generee.xml

Pour repartir de zéro (supprimer .class, classes générées et output/) :
   ./clean.sh

Détail des commandes lancées par run.sh, si besoin de les rejouer à la main :
   javac src/XmlNode.java src/XsdToJavaGenerator.java src/ParserValidator.java -d src/
   java -cp src ParserValidator bibliotheque.xsd
   java -cp src XsdToJavaGenerator bibliotheque.xsd generated
   javac -cp src generated/*.java -d generated
   java -cp "src;generated" TestMain

   (Sous Linux/macOS, remplacer ";" par ":" dans le classpath : -cp "src:generated")

LIMITES VOLONTAIRES
--------------------
Le générateur ne gère pas xs:attribute, xs:choice/xs:all, les contraintes
(xs:restriction, xs:pattern, xs:enumeration) ni les imports de schémas
multiples — voir docs/regles_correspondance.md, section 4, pour le détail
et la justification de ces choix.
