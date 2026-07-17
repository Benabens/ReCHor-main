package ch.epfl.rechor;

/**
 * Fournit des méthodes utilitaires pour vérifier des préconditions dans le programme.
 * Cette classe est une classe utilitaire qui ne doit pas être instanciée.
 * Elle offre notamment une méthode statique pour s'assurer qu'un argument remplit une condition attendue
 * et lève une exception {@link IllegalArgumentException} si ce n'est pas le cas.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class Preconditions {

    private Preconditions() {
        // Empêche l'instanciation de cette classe utilitaire.
    }

    /**
     * Vérifie que la condition spécifiée est vraie.
     * Si la condition n'est pas satisfaite, cette méthode lève une exception de type
     * {@link IllegalArgumentException}.
     *
     * @param shouldBeTrue la condition qui doit être vraie
     * @throws IllegalArgumentException si la condition est fausse
     */
    public static void checkArgument(boolean shouldBeTrue) {
        if (!shouldBeTrue) {
            throw new IllegalArgumentException();
        }
    }
}
