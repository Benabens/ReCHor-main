package ch.epfl.rechor.journey;

import ch.epfl.rechor.Preconditions;

import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

/**
 * Représente une frontière de Pareto immuable contenant des tuples de critères empaquetés.
 * Les tuples sont stockés dans un tableau de type long[].
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class ParetoFront {

    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);
    private final long[] front;

    private ParetoFront(long[] front) {
        this.front = front;
    }

    /**
     * Retourne le nombre de tuples contenus dans cette frontière.
     *
     * @return la taille de la frontière
     */
    public int size() {
        return front.length;
    }

    /**
     * Retourne le tuple dont l'heure d'arrivée et le nombre de changements correspondent.
     *
     * @param arrMins heure d'arrivée
     * @param changes nombre de changements
     * @return le tuple correspondant
     * @throws NoSuchElementException si aucun tuple ne correspond
     */
    public long get(int arrMins, int changes) {
        for (long c : front) {
            if (PackedCriteria.arrMins(c) == arrMins && PackedCriteria.changes(c) == changes) {
                return c;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Applique l'action donnée à chaque tuple.
     *
     * @param action le consommateur de long à appliquer
     */
    public void forEach(LongConsumer action) {
        for (long c : front) {
            action.accept(c);
        }
    }

    static ParetoFront ofArrayNoCopy(long[] array) {
        return new ParetoFront(array);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(front.length == 0 ? "ParetoFront(empty)" : "ParetoFront(" + front.length + " elements) [\n");
        for (long c : front) {
            boolean hasDep = PackedCriteria.hasDepMins(c);
            sb.append("  dep=")
                    .append(hasDep ? PackedCriteria.depMins(c) : "none")
                    .append(" arr=").append(PackedCriteria.arrMins(c))
                    .append(" changes=").append(PackedCriteria.changes(c)).append("\n");
        }
        return sb.append("]").toString();
    }

    /**
     * Bâtisseur permettant de construire progressivement une ParetoFront.
     */
    public static class Builder {
        private long[] buffer;
        private int size;

        /**
         * Crée un nouveau bâtisseur vide.
         */
        public Builder() {
            buffer = new long[16];
            size = 0;
        }

        /**
         * Constructeur de copie.
         *
         * @param that autre bâtisseur à copier
         */
        public Builder(Builder that) {
            buffer = new long[that.buffer.length];
            System.arraycopy(that.buffer, 0, buffer, 0, that.size);
            size = that.size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public Builder clear() {
            size = 0;
            return this;
        }

        /**
         * Ajoute un tuple empaqueté à la frontière.
         *
         * @param packedTuple le tuple à ajouter
         * @return this
         */
        public Builder add(long packedTuple) {
            int i = 0;
            while (i < size && buffer[i] < packedTuple) {
                if (PackedCriteria.dominatesOrIsEqual(buffer[i], packedTuple)) return this;
                i++;
            }

            // Si un tuple égal existe
            if (i < size && PackedCriteria.dominatesOrIsEqual(buffer[i], packedTuple)) return this;

            int w = i;
            for (int r = i; r < size; r++) {
                if (!PackedCriteria.dominatesOrIsEqual(packedTuple, buffer[r])) {
                    buffer[w++] = buffer[r];
                }
            }

            ensureCapacity();
            System.arraycopy(buffer, i, buffer, i + 1, w - i);
            buffer[i] = packedTuple;
            size = w + 1;
            return this;
        }

        public Builder add(int arrMins, int changes, int payload) {
            return add(PackedCriteria.pack(arrMins, changes, payload));
        }

        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) add(that.buffer[i]);
            return this;
        }

        public boolean fullyDominates(Builder that, int depMins) {
            Preconditions.checkArgument(depMins >= 0);
            for (int i = 0; i < that.size; i++) {
                long t = that.buffer[i];
                Preconditions.checkArgument(!PackedCriteria.hasDepMins(t));
                if (!isDominatedTransformed(PackedCriteria.withDepMins(t, depMins), depMins)) return false;
            }
            return true;
        }

        public void forEach(LongConsumer action) {
            for (int i = 0; i < size; i++) action.accept(buffer[i]);
        }

        public ParetoFront build() {
            long[] copy = new long[size];
            System.arraycopy(buffer, 0, copy, 0, size);
            return ParetoFront.ofArrayNoCopy(copy);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(size == 0 ? "ParetoFront.Builder(empty)" : "ParetoFront.Builder(" + size + " elements) [\n");
            for (int i = 0; i < size; i++) {
                long c = buffer[i];
                sb.append("  dep=")
                        .append(PackedCriteria.hasDepMins(c) ? PackedCriteria.depMins(c) : "none")
                        .append(" arr=").append(PackedCriteria.arrMins(c))
                        .append(" changes=").append(PackedCriteria.changes(c)).append("\n");
            }
            return sb.append("]").toString();
        }

        private void ensureCapacity() {
            if (size == buffer.length) {
                long[] newBuffer = new long[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, size);
                buffer = newBuffer;
            }
        }

        private boolean isDominatedTransformed(long candidate, int depMins) {
            for (int i = 0; i < size; i++) {
                long t = PackedCriteria.hasDepMins(buffer[i]) ? buffer[i] : PackedCriteria.withDepMins(buffer[i], depMins);
                if (PackedCriteria.dominatesOrIsEqual(t, candidate)) return true;
            }
            return false;
        }
    }
}
