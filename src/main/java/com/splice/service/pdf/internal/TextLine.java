package com.splice.service.pdf.internal;

import com.splice.geometry.BoundingBox;

import java.util.*;

public class TextLine {
    private static final float SPACE_TOLERANCE_FACTOR = 0.5f;

    private final List<TextAtom> atoms = new ArrayList<>();
    private BoundingBox box;
    private String cachedText = null;

    public void addAtom(TextAtom atom) {
        if (atom == null) return;

        atoms.add(atom);
        cachedText = null;

        if (box == null) {
            this.box = atom.box();
        } else {
            this.box = this.box.union(atom.box());
        }
    }

    public BoundingBox getBox() { return box; }

    public List<TextAtom> getAtoms() {
        return Collections.unmodifiableList(atoms);
    }

    public String getText() {
        if (cachedText != null) {
            return cachedText;
        }

        if (atoms.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        sb.append(atoms.getFirst().content());

        for (int i = 1; i < atoms.size(); i++) {
            TextAtom previous = atoms.get(i - 1);
            TextAtom current = atoms.get(i);

            float gap = previous.box().horizontalGap(current.box());

            float spaceWidth = previous.getEstimatedSpaceWidth();

            if (gap > spaceWidth * SPACE_TOLERANCE_FACTOR) {
                sb.append(" ");
            }

            sb.append(current.content());
        }

        cachedText = sb.toString();
        return cachedText;
    }

    public TextAtom getLastAtom() {
        if (atoms.isEmpty()) return null;
        return atoms.getLast();
    }

    public boolean accepts(TextAtom atom, float maxDistanceFactor) {
        if (atoms.isEmpty()) return true;

        TextAtom previousAtom = getLastAtom();

        float horizontalGap = previousAtom.box().horizontalGap(atom.box());
        float threshold = previousAtom.getEstimatedSpaceWidth() * maxDistanceFactor;

        boolean isAligned = previousAtom.isVerticallyAlignedWith(atom);
        boolean isNear = horizontalGap < threshold;
        boolean isNotBackwards = atom.box().x() > previousAtom.box().x();

        return isAligned && isNear && isNotBackwards;
    }

    @Override
    public String toString() {
        return getText();
    }
}