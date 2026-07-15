import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public abstract class XmlNode {

    private boolean indent = true;

    /** Désactive l'indentation : la sérialisation produit alors une seule ligne. */
    public XmlNode compact() {
        this.indent = false;
        return this;
    }

    /**
     * Sérialise cet objet (et toute sa hiérarchie) en fichier XML complet,
     * avec l'en-tête <?xml ... ?>.
     */
    public void save(String cheminFichier) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append(indent ? "\n" : "");
        render(sb, 0);

        java.io.File target = new java.io.File(cheminFichier);
        java.io.File parent = target.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (FileWriter fw = new FileWriter(target)) {
            fw.write(sb.toString());
        }
    }

    /** Sérialise cet objet en chaîne XML (sans en-tête), utile pour debug/log. */
    public String toXmlString() {
        StringBuilder sb = new StringBuilder();
        render(sb, 0);
        return sb.toString();
    }

    private void render(StringBuilder sb, int depth) {
        String pad = indent ? "    ".repeat(depth) : "";
        String nl = indent ? "\n" : "";
        String tag = tagName();

        sb.append(pad).append("<").append(tag).append(">").append(nl);

        for (Field f : getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object value;
            try {
                value = f.get(this);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Lecture impossible du champ " + f.getName(), e);
            }
            if (value == null) continue;

            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof XmlNode node) {
                        node.indent = this.indent;
                        node.render(sb, depth + 1);
                    }
                }
            } else if (value instanceof XmlNode node) {
                node.indent = this.indent;
                node.render(sb, depth + 1);
            } else {
                String childPad = indent ? "    ".repeat(depth + 1) : "";
                sb.append(childPad).append("<").append(f.getName()).append(">")
                  .append(value)
                  .append("</").append(f.getName()).append(">").append(nl);
            }
        }

        sb.append(pad).append("</").append(tag).append(">").append(nl);
    }

    /** Nom de la balise XML représentant cet objet */
    protected String tagName() {
        return getClass().getSimpleName().toLowerCase();
    }
}
