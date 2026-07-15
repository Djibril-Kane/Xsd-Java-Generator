import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class XsdToJavaGenerator {

    static class ElementDef {
        String name;
        boolean complex;
        String simpleJavaType;         // "String", "Integer", "Boolean" si non complexe
        List<Child> children = new ArrayList<>();
    }

    static class Child {
        ElementDef target;
        boolean unbounded;
    }

    static Map<String, ElementDef> registry = new LinkedHashMap<>();
    static String rootName;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java XsdToJavaGenerator <fichier.xsd> <dossier_sortie>");
            return;
        }
        String xsdPath = args[0];
        String outDir = args[1];

        parse(xsdPath);
        resolveRoot();

        new File(outDir).mkdirs();
        for (ElementDef def : registry.values()) {
            if (def.complex) writeClass(def, outDir);
        }

        System.out.println("Classes générées dans " + outDir + " :");
        for (ElementDef def : registry.values()) {
            if (def.complex) System.out.println("  - " + capitalize(def.name) + (def.name.equals(rootName) ? "  (racine, expose save())" : ""));
        }
    }

    // ---------- PARSING ----------

    static void parse(String xsdPath) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new File(xsdPath));
        Element schema = doc.getDocumentElement();

        // Passe 1 : déclarer tous les éléments de premier niveau (pour pouvoir résoudre les ref en avant)
        List<Element> topLevelNodes = new ArrayList<>();
        NodeList children = schema.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && local(n).equals("element")) {
                Element el = (Element) n;
                ElementDef def = new ElementDef();
                def.name = el.getAttribute("name");
                registry.put(def.name, def);
                topLevelNodes.add(el);
            }
        }

        // Passe 2 : remplir chaque ElementDef (type simple, ou séquence d'enfants résolus)
        for (Element el : topLevelNodes) {
            ElementDef def = registry.get(el.getAttribute("name"));
            String typeAttr = el.getAttribute("type");
            Element complexType = child(el, "complexType");

            if (complexType != null) {
                def.complex = true;
                Element sequence = child(complexType, "sequence");
                if (sequence != null) {
                    NodeList seqChildren = sequence.getChildNodes();
                    for (int j = 0; j < seqChildren.getLength(); j++) {
                        Node cn = seqChildren.item(j);
                        if (cn.getNodeType() != Node.ELEMENT_NODE || !local(cn).equals("element")) continue;
                        Element childEl = (Element) cn;
                        String ref = childEl.getAttribute("ref");
                        ElementDef targetDef = registry.get(ref);
                        if (targetDef == null) {
                            throw new IllegalStateException("Référence XSD non résolue : ref=\"" + ref + "\"");
                        }
                        Child c = new Child();
                        c.target = targetDef;
                        c.unbounded = "unbounded".equals(childEl.getAttribute("maxOccurs"));
                        def.children.add(c);
                    }
                }
            } else if (!typeAttr.isEmpty()) {
                def.complex = false;
                def.simpleJavaType = mapSimpleType(typeAttr);
            }
        }
    }

    static String mapSimpleType(String xsdType) {
        String local = xsdType.contains(":") ? xsdType.substring(xsdType.indexOf(':') + 1) : xsdType;
        return switch (local) {
            case "int", "integer", "long", "short" -> "Integer";
            case "boolean" -> "Boolean";
            default -> "String"; 
        };
    }

    /** L'élément racine est celui qu'aucun autre élément ne référence. */
    static void resolveRoot() {
        Set<String> referenced = new HashSet<>();
        for (ElementDef def : registry.values()) {
            for (Child c : def.children) referenced.add(c.target.name);
        }
        for (String name : registry.keySet()) {
            if (!referenced.contains(name)) { rootName = name; break; }
        }
    }

    static Element child(Element parent, String localName) {
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && local(n).equals(localName)) return (Element) n;
        }
        return null;
    }

    static String local(Node n) {
        String ln = n.getLocalName();
        return ln != null ? ln : n.getNodeName().replaceFirst("^.*:", "");
    }

    // ---------- GENERATION ----------

    static void writeClass(ElementDef def, String outDir) throws IOException {
        String className = capitalize(def.name);
        boolean needsList = def.children.stream().anyMatch(c -> c.unbounded);

        StringBuilder sb = new StringBuilder();
        if (needsList) {
            sb.append("import java.util.ArrayList;\n");
            sb.append("import java.util.List;\n\n");
        }
        sb.append("public class ").append(className).append(" extends XmlNode {\n\n");

        for (Child c : def.children) {
            String fieldType = javaType(c.target);
            String fieldName = fieldName(c);
            if (c.unbounded) {
                sb.append("    private List<").append(fieldType).append("> ").append(fieldName)
                  .append(" = new ArrayList<>();\n");
            } else {
                sb.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
            }
        }
        sb.append("\n");

        for (Child c : def.children) {
            String fieldName = fieldName(c);
            String accessor = capitalize(fieldName);
            if (c.unbounded) {
                String fieldType = "List<" + javaType(c.target) + ">";
                sb.append("    public ").append(fieldType).append(" get").append(accessor).append("() {\n");
                sb.append("        return ").append(fieldName).append(";\n    }\n\n");
                sb.append("    public void set").append(accessor).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
                sb.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n    }\n\n");
            } else {
                String fieldType = javaType(c.target);
                sb.append("    public ").append(fieldType).append(" get").append(accessor).append("() {\n");
                sb.append("        return ").append(fieldName).append(";\n    }\n\n");
                sb.append("    public void set").append(accessor).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
                sb.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n    }\n\n");
            }
        }

        sb.append("}\n");

        try (FileWriter fw = new FileWriter(new File(outDir, className + ".java"))) {
            fw.write(sb.toString());
        }
    }

    /** Nom du champ Java. Les champs répétés (listes) sont pluralisés pour donner getLivres()/setLivres(). */
    static String fieldName(Child c) {
        String name = decapitalize(c.target.name);
        if (c.unbounded && !name.endsWith("s")) return name + "s";
        return name;
    }

    static String javaType(ElementDef def) {
        return def.complex ? capitalize(def.name) : def.simpleJavaType;
    }

    static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
