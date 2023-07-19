package pdbexplorer.model.protein;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * This class defines an atom in a molecule.
 */
public class PDBAtom {
    private final String letter;
    private final double radius;
    private final Color color;
    private final String role;
    private final int id;
    private final Point3D coordinates;
    private final int model;
    private final String chain;

    // HashMap defining atom colors (according to https://www.ch.ic.ac.uk/rzepa/mim/domestic/html/atomcols.htm)
    private static final Map<String, Color> colorMap = Map.of("O", Color.RED, "C",
            Color.GRAY, "N", Color.BLUE, "S", Color.YELLOW, "SE", Color.ORANGE, "DEFAULT", Color.GREEN);

    /**
     * Constructor for an Atom object.
     * @param letter (String): element symbol
     * @param role (String): the role of the atom (e.g., CA for C alpha)
     * @param id (int): atom ID
     * @param coordinates (Point3D): atom x, y, z coordinates in 3D-space
     * @param model (int): number of the model
     * @param chain (String): ID of the chain
     */
    public PDBAtom(String letter, String role, int id, Point3D coordinates, int model, String chain) {
        // HashMap defining atom radii (in Angstrom)
        // taken from: Handbook of Chemistry and Physics by W.M. Haynes, 97th Edition, Section 9.57
        Map<String, Double> radiusMap = Map.of("O", 0.64, "C", 0.75,
                "N", 0.71, "S", 1.04, "SE", 1.18, "DEFAULT", 0.6);

        this.letter = letter;
        this.radius = radiusMap.get(radiusMap.containsKey(letter) ? letter: "DEFAULT");
        this.color = colorMap.get(radiusMap.containsKey(letter) ? letter: "DEFAULT");
        this.role = role;
        this.id = id;
        this.coordinates = coordinates;
        this.model = model;
        this.chain = chain;
    }

    /**
     * Getter method for the atom letter.
     * @return String: letter that gives the atom type
     */
    public String getLetter() {
        return letter;
    }

    /**
     * Getter method for radius of the atom.
     * @return double: atom radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Getter method for color of the atom.
     * @return Color: atom color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Getter method for role of the atom.
     * @return String: role of the atom in the monomer
     */
    public String getRole() {
        return role;
    }

    /**
     * Getter method for ID of the atom.
     * @return int: atom ID
     */
    public int getId() {
        return id;
    }

    /**
     * Getter method for coordinates of the atom.
     * @return Point3D: atom coordinates in 3D space
     */
    public Point3D getCoordinates() {
        return coordinates;
    }

    /**
     * Getter method for the model number.
     * @return int: number of the model the atom is contained in
     */
    public int getModel() {
        return model;
    }

    /**
     * Getter method for the chain label.
     * @return String: label of the chain the atom is contained in
     */
    public String getChain() {
        return chain;
    }

    /**
     * Getter method for the mapping of atom type to color.
     * @return Map: maps atom to display color
     */
    public static Map<String, Color> getColorMap() {
        return colorMap;
    }
}
