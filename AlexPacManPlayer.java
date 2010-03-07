package player;

import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import util.*;
import pacman.*;

/**
 * CM0312 coursework
 *
 * The class uses minimax, alpha-beta pruning and allows for random ghost moves
 * to be considered. Each of the heuristics in the evaluation has been weighted
 * in order to make the pacman player more aggressive and try to gobble dots as
 * quickly as possible. 
 *
 * The search tree is also reduced by ignoring moving in the opposite direction
 * within the tree.
 *
 * Against basic ghosts the aforementioned approach can get to level 250
 * without losing a life.
 *
 * @author Alex Duller
 */
public class AlexPacManPlayer implements PacManPlayer
{
    // Depth to traverse in game tree
    private final double MAX_DEPTH = 15;
    // Keep track of the last move since we penalise moving in the opposite
    // direction
    private Move last_move = Move.NONE;
    // Amount to deduct from score for going into the opposite direction to
    // the last move
    private final double OPPOSITE_MOVE_PENALTY = 10;
    // Amount to multiply number of dots left (used in evaluation function)
    private final double GOBBLE_SCALING_FACTOR = 5;
    // Amount to multiply distance from dots (used in evaluation function)
    private final double DOT_DIST_SCALING_FACTOR = 0.2;
    // Amount to multiply distance from ghosts (used in evaluation function)
    private final double GHOST_DIST_SCALING_FACTOR = 0.1;
    private final double LOSING_SCORE = -5000;
    private final double WINNING_SCORE = 5000;
    // Set this to true if ghosts always choose the worst move for pacman
    private final boolean OPTIMAL_GHOSTS = true;

    public Move chooseMove(Game game)
    {
        Move best_move = Move.NONE;
        double best_score = Double.NEGATIVE_INFINITY;
        State current = game.getCurrentState();

        // alpha-beta pruning values
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;

        // Start at a max node
        for (Move move : game.getLegalPacManMoves())
        {
            double move_score = 0;
            move_score = min_value(Game.getNextState(current, move),
                                   move,
                                   alpha,
                                   beta,
                                   MAX_DEPTH - 1);
            if (last_move == move.getOpposite())
                move_score -= OPPOSITE_MOVE_PENALTY;

            if (move_score > best_score)
            {
                best_score = move_score;
                best_move = move;
            }

            if (move_score < beta)
                alpha = Math.max(alpha, move_score);
            else
                break;
        }
        System.out.format("D: %03d, T: %d, P: %d\r",
                          current.getDotLocations().size(),
                          game.getTime(),
                          game.getPoints());
        //        try {Thread.sleep(2000);} catch (Exception e) {}
        last_move = best_move;
        return best_move;
    }

    /**
     * Work out the best move for pacman. Uses alpha-beta pruning and ignores
     * searches in the opposite direction since these are already covered
     * elsewhere in the tree.
     *
     * @param state The current state (assumed to be pacman's move)
     * @param prev_move pacman's previous move
     * @param alpha best for max
     * @param beta best for min
     * @param depth within search tree. The lower, the further down the tree
     * @return The score of the best move for pacman using minimax
     */
    private double max_value(State state,
                             Move prev_move,
                             double alpha,
                             double beta,
                             double depth)
    {
        if (Game.isFinal(state) || depth < 1)
            return evaluate(state);

        double v = Double.NEGATIVE_INFINITY;
        List<Move> moves = Game.getLegalPacManMoves(state);
        // There's no need to explore moves in the opposite direction as these
        // should be explored elsewhere in the tree.
        moves.remove(prev_move.getOpposite());
        for (Move move : moves)
        {
            v = Math.max(v, min_value(Game.getNextState(state, move),
                                      move,
                                      alpha,
                                      beta,
                                      depth - 1));
            if (v < beta)
                alpha = Math.max(alpha, v);
            else
                break;
        }
        return v;
    }

    /**
     * Find the best combined move by the ghosts i.e. the worst move for
     * pacman. If the ghosts are not considered optimal opponents a single
     * random move will be considered. This goes some way to implementing a
     * chance node.
     *
     * @param successors The successors of the previous move by pacman
     * @param prev_move pacman's previous move. Handed straight onto the
     * max_value call.
     * @param alpha max cut-off
     * @param beta min cut-off
     * @param depth Depth within search tree. The lower, the further down the
     * tree.
     * @return The score of the best move for the ghosts/worst move for pacman
     */
    private double min_value(State state,
                             Move prev_move,
                             double alpha,
                             double beta,
                             double depth)
    {
        if (Game.isFinal(state) || depth < 1)
            return evaluate(state);

        Set<List<Move>> combined = Game.getLegalCombinedGhostMoves(state);
        if (!OPTIMAL_GHOSTS)
        {
            Random rand = new Random(System.currentTimeMillis());
            List<Move> rand_move =
                (List<Move>)combined.toArray()[rand.nextInt(combined.size())];
            combined.clear();
            combined.add(rand_move);
        }
        double v = Double.POSITIVE_INFINITY;
        for (List<Move> move : combined)
        {
            v = Math.min(v, max_value(Game.getNextState(state, move),
                                      prev_move,
                                      alpha,
                                      beta,
                                      depth - 1));
            if (v > alpha)
                beta = Math.min(beta, v);
            else
                break;
        }
        return v;
    }

    /**
     * Assign a value to a given state depending on the desirability of that
     * state.
     *
     * @param state The state to evaluate
     * @return The value of the state. Higher is better.
     */
    private double evaluate(State state)
    {
        // Use arbitrary big numbers for winning and losing. The problem with
        // Double.POSITIVE_INFINITY and Double.NEGATIVE_INFINITY here is that
        // we can't add numbers to those values to make them bigger and smaller
        // respectively. Consequently, if all moves result in a loss pacman
        // won't choose between them and will sit there waiting for the worst.
        if (Game.isLosing(state))
            return LOSING_SCORE;

        if (Game.isWinning(state))
            return WINNING_SCORE;

        double score = 0;
        // Each of these heuristics has a scaling factor applied to it so that
        // pacman makes better decisions.
        score -= state.getDotLocations().size() * GOBBLE_SCALING_FACTOR;
        Location p = state.getPacManLocation();
        score -=
            Location.manhattanDistanceToClosest(p, state.getDotLocations())
            * DOT_DIST_SCALING_FACTOR;
        // pacman seems to work quite well if he's aggressive and largely
        // ignores the movements of the basic ghosts - this may not be the case
        // if more aggressive ghosts were used!
        score +=
            Location.manhattanDistanceToClosest(p, state.getGhostLocations())
            * GHOST_DIST_SCALING_FACTOR;
        score -= num_groups(state.getDotLocations());
        return score;
    }

    // Work out the number of groups of dots
    private double num_groups(Collection<Location> dots)
    {

        return 1;
    }
}
