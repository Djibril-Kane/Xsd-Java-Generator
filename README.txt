PROJET : Générateur de code Java à partir de schémas XML (XSD) — v2
=====================================================================

Cette version reprend le même objectif que la v1 mais change le mécanisme
de sérialisation : au lieu d'écrire une méthode toXml() dans chaque classe
générée, la sérialisation est centralisée dans une classe commune XmlNode,
dont héritent toutes les classes produites, et qui utilise la réflexion
Java (java.lang.reflect.Field) pour parcourir les champs de l'objet.

CONTENU
-------
- docs/regles_correspondance.md      : règles de correspondance XSD -> Java (document autonome)
- docs/Rapport_Generateur_XSD_Java.docx : rapport complet (objectif, règles, architecture, tests, limites)
- bibliotheque.xsd                    : schéma XSD d'exemple fourni dans l'énoncé
- src/XsdToJavaGenerator.java         : le générateur (parsing XSD + génération des classes)
- src/XmlNode.java                    : classe mère fournissant save()/toXmlString() par réflexion
- generated/Bibliotheque.java         : classe générée (élément racine, expose save())
- generated/Livre.java                : classe générée
- generated/TestMain.java             : programme de test qui recrée le XML de l'énoncé
- generated/output/bibliotheque_generee.xml : résultat du test (identique en contenu à l'énoncé)

COMMENT REJOUER TOUT LE PROJET
-------------------------------
1) Compiler le générateur et la classe runtime :
   javac src/XmlNode.java src/XsdToJavaGenerator.java -d src/

2) Générer les classes Java à partir du XSD :
   java -cp src XsdToJavaGenerator bibliotheque.xsd generated

3) Compiler les classes générées + le test, puis exécuter :
   javac -cp src generated/*.java -d generated
   java -cp src:generated TestMain

   (Sous Windows, remplacer ":" par ";" dans le classpath : -cp src;generated)

4) Le fichier généré apparaît dans generated/output/bibliotheque_generee.xml.

DIFFÉRENCES AVEC LA V1
-----------------------
- Sérialisation par réflexion (XmlNode) au lieu d'un toXml() dupliqué dans
  chaque classe générée : les classes générées sont plus courtes et ne
  contiennent que des champs + accesseurs.
- Résolution des ref="..." en liens directs objet -> objet dès le parsing,
  au lieu d'un simple nom recherché à chaque utilisation.
- Mapping de types simples étendu : xs:int/xs:integer -> Integer,
  xs:boolean -> Boolean (en plus de xs:string -> String).
- XML indenté par défaut (4 espaces/niveau), avec une méthode compact()
  pour désactiver l'indentation si besoin.
- Règles de correspondance documentées dans un fichier séparé
  (docs/regles_correspondance.md), en plus du rapport.
