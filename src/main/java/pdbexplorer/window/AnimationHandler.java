package pdbexplorer.window;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class handles all animations present in the program.
 */
public class AnimationHandler {
    /**
     * Generates a Timeline object for the animated rotation of the molecule.
     * @param outerGroup (Group): Group containing all balls, sticks and ribbons
     * @param rotate (Rotate): rotation to apply based on mouse drag
     * @return TimeLine: the timeline for the rotation animation
     */
    public static Timeline rotationAnimation(Group outerGroup, Rotate rotate) {
        KeyFrame keyFrame = new KeyFrame(Duration.millis(30), event -> {
            Transform oldTransform = outerGroup.getTransforms().get(0);
            Transform newTransform = rotate.createConcatenation(oldTransform);
            outerGroup.getTransforms().set(0, newTransform);
        });

        Timeline timeline = new Timeline(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);

        return timeline;
    }

    /**
     * Generates an animation where all chains contained in the molecule first move away from each other and then back
     * together.
     * @param outerGroup (Group): Group containing all balls, sticks and ribbons
     * @param chains (ArrayList): the chains contained in the molecule
     * @param numberOfModels (int): the number of models available for the molecule
     * @return TimeLine: the timeline for the explode-animation
     */
    public static Timeline explodeAnimation(Group outerGroup, ArrayList<String> chains, int numberOfModels) {
        // Just in case the Explode-button is not disabled properly:
        if (chains.size() < 2)
            return null;

        // Compute mean points for balls groups
        ArrayList<ArrayList<Point3D>> means = new ArrayList<>();
        for (int i = 0; i < (numberOfModels == 0 ? 1 : numberOfModels); i++) {
            Group currentModel = (Group) ((Group) outerGroup.getChildren().get(0)).getChildren().get(i);
            means.add(new ArrayList<>());
            for (int j = 0; j < chains.size(); j++) {
                Group currentChain = (Group) currentModel.getChildren().get(j);

                // Compute the mean point of the chain
                Point3D meanPoint = new Point3D(0, 0, 0);
                for (Node node : currentChain.getChildren()) {
                    meanPoint = meanPoint.add(
                            new Point3D(node.getTranslateX(), node.getTranslateY(), node.getTranslateZ()));
                }
                meanPoint = meanPoint.multiply((1.0 / currentChain.getChildren().size()));

                means.get(i).add(meanPoint);
            }
        }

        // Create KeyValues
        Collection<KeyValue> keyValues = new ArrayList<>();
        for (int h = 0; h < outerGroup.getChildren().size(); h++) {
            Group nodeGroup = (Group) outerGroup.getChildren().get(h);

            if (h == 2 && nodeGroup.getChildren().isEmpty()) // in case ribbons have not been computed yet
                break;

            for (int i = 0; i < (numberOfModels == 0 ? 1 : numberOfModels); i++) {
                for (int j = 0; j < chains.size(); j++) {
                    Group currChain = (Group) ((Group) nodeGroup.getChildren().get(i)).getChildren().get(j);

                    // Compute new x, y, z coordinates
                    double x = currChain.getTranslateX() + means.get(i).get(j).getX() * 1.25;
                    double y = currChain.getTranslateY() + means.get(i).get(j).getY() * 1.25;
                    double z = currChain.getTranslateZ() + means.get(i).get(j).getZ() * 1.25;

                    // Add coordinates of mean point to the current coordinate of the chain
                    keyValues.add(new KeyValue(currChain.translateXProperty(), x));
                    keyValues.add(new KeyValue(currChain.translateYProperty(), y));
                    keyValues.add(new KeyValue(currChain.translateZProperty(), z));
                }
            }
        }

        // Create KeyFrame
        KeyFrame keyFrameRun = new KeyFrame(Duration.seconds(2), keyValues.toArray(new KeyValue[0]));
        KeyFrame keyFramePause = new KeyFrame(Duration.seconds(2.5), keyValues.toArray(new KeyValue[0]));

        // Create timeline and add KeyFrame
        Timeline timeline = new Timeline(keyFrameRun, keyFramePause);
        timeline.setCycleCount(2);
        timeline.setAutoReverse(true);

        return timeline;
    }

    /**
     * Generates an animation that "jiggles" through all models if a PDB file contains more than one.
     * This only works for the atoms! Bonds or ribbons do not jiggle.
     * Under the assumption that the atoms in all models follow the same order and all models have the same total
     * number of atoms. The animation always starts at the currently selected model.
     * @return TimeLine: the timeline for the jiggle animation
     */
    public static Timeline jiggleAnimation(Group balls, int numberOfModels, int selectedModel) {
        // Just in case the Jiggle-button is not disabled properly:
        if (numberOfModels == 0)
            return null;

        // Generate KeyFrames
        Collection<KeyFrame> keyFrames = new ArrayList<>();
        double duration = 0.0;

        for (int i = 0; i < numberOfModels; i++) {
            // Generate KeyValues
            Collection<KeyValue> keyValues = new ArrayList<>();

            // Get currently looked at model
            Group modelGroup = (Group) balls.getChildren().get(i);

            for (int k = 0; k < modelGroup.getChildren().size(); k++) {
                Group chainGroup;
                try {
                    chainGroup = (Group) modelGroup.getChildren().get(k);
                } catch (IndexOutOfBoundsException exception) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "This PDB file is corrupted. Jiggle can only be performed if atom number is the same in each model!");
                    alert.show();
                    return null;
                }

                for (int l = 0; l < chainGroup.getChildren().size(); l++) {
                    Node node;
                    Node nextNode;
                    try {
                        node = ((Group) ((Group) balls.getChildren().get(selectedModel))
                                .getChildren().get(k)).getChildren().get(l);

                        int currentModel = (selectedModel + i) % numberOfModels;
                        nextNode = ((Group) ((Group) balls.getChildren()
                                .get(currentModel == numberOfModels - 1 ? 0 : currentModel + 1))
                                .getChildren().get(k)).getChildren().get(l);
                    } catch (IndexOutOfBoundsException exception) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "This PDB file is corrupted. Jiggle can only be performed if atom number is the same in each model!");
                        alert.show();
                        return null;
                    }

                    keyValues.add(new KeyValue(node.translateXProperty(), nextNode.getTranslateX()));
                    keyValues.add(new KeyValue(node.translateYProperty(), nextNode.getTranslateY()));
                    keyValues.add(new KeyValue(node.translateZProperty(), nextNode.getTranslateZ()));
                }
            }


            duration += 0.2;
            keyFrames.add(new KeyFrame(Duration.seconds(duration), keyValues.toArray(new KeyValue[0])));
        }

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(keyFrames);
        timeline.setCycleCount(1);
        timeline.setAutoReverse(false);

        return timeline;
    }
}
